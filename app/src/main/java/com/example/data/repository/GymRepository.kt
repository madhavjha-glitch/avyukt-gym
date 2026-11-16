package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.FeeStatus
import com.example.data.model.GymBatch
import com.example.data.model.GymMember
import com.example.data.model.MembersSnapshot
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GymRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeMembers(): Flow<List<GymMember>> =
        observeMembersSnapshot().map { it.members }

    fun observeMembersSnapshot(): Flow<MembersSnapshot> = flow {
        val uid = requireUserId()
        val path = "users/$uid/members"
        db.collection("users").document(uid).collection("members")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                MembersSnapshot(
                    members = snapshot.toObjects(
                        GymMember::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ),
                    hasPendingWrites = snapshot.metadata.hasPendingWrites(),
                    isFromCache = snapshot.metadata.isFromCache
                )
            }
            .catch { error ->
                logIfAuthenticated(error, OperationType.LIST, path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeAttendanceDay(date: String): Flow<Map<String, Boolean>> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("attendance").document(date)
        docRef.snapshots()
            .map { snapshot -> parseAttendanceRecords(snapshot) }
            .catch { error ->
                logIfAuthenticated(error, OperationType.GET, docRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeAllAttendanceDays(): Flow<Map<String, Map<String, Boolean>>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/attendance"
        db.collection("users").document(uid).collection("attendance")
            .snapshots()
            .map { snapshot ->
                buildMap {
                    snapshot.documents.forEach { doc ->
                        put(doc.id, parseAttendanceRecords(doc))
                    }
                }
            }
            .catch { error ->
                logIfAuthenticated(error, OperationType.LIST, path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeMonthlyAttendanceCounts(monthPrefix: String): Flow<Map<String, Int>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/attendance"
        db.collection("users").document(uid).collection("attendance")
            .whereGreaterThanOrEqualTo("date", "$monthPrefix-01")
            .whereLessThanOrEqualTo("date", "$monthPrefix-31")
            .snapshots()
            .map { snapshot ->
                val counts = mutableMapOf<String, Int>()
                snapshot.documents.forEach { doc ->
                    val records = parseAttendanceRecords(doc)
                    records.forEach { (memberId, present) ->
                        if (present) {
                            counts[memberId] = (counts[memberId] ?: 0) + 1
                        }
                    }
                }
                counts.toMap()
            }
            .catch { error ->
                logIfAuthenticated(error, OperationType.LIST, path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeFeeMonth(month: String): Flow<Map<String, FeeStatus>> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("fees").document(month)
        docRef.snapshots()
            .map { snapshot -> parseFeeRecords(snapshot) }
            .catch { error ->
                logIfAuthenticated(error, OperationType.GET, docRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeAllFeeMonths(): Flow<Map<String, Map<String, FeeStatus>>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/fees"
        db.collection("users").document(uid).collection("fees")
            .snapshots()
            .map { snapshot ->
                buildMap {
                    snapshot.documents.forEach { doc ->
                        put(doc.id, parseFeeRecords(doc))
                    }
                }
            }
            .catch { error ->
                logIfAuthenticated(error, OperationType.LIST, path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun addMember(
        name: String,
        phone: String,
        joinDate: String,
        batch: String = GymBatch.MORNING.id
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("members").document()
        val safeBatch = GymBatch.fromId(batch).id
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim(),
            "phone" to phone.trim(),
            "joinDate" to joinDate.trim(),
            "batch" to safeBatch,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateMember(
        memberId: String,
        name: String,
        phone: String,
        joinDate: String,
        batch: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("members").document(memberId)
        val safeBatch = GymBatch.fromId(batch).id
        val updates = mapOf(
            "name" to name.trim(),
            "phone" to phone.trim(),
            "joinDate" to joinDate.trim(),
            "batch" to safeBatch,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMember(memberId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("members").document(memberId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setAttendance(date: String, memberId: String, present: Boolean): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("attendance").document(date)
        return try {
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) {
                    tx.set(
                        docRef,
                        mapOf(
                            "userId" to uid,
                            "date" to date,
                            "records" to mapOf(memberId to present),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.update(
                        docRef,
                        mapOf(
                            "records.$memberId" to present,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setAllAttendance(
        date: String,
        memberIds: List<String>,
        present: Boolean
    ): Result<Unit> {
        if (memberIds.isEmpty()) return Result.success(Unit)
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("attendance").document(date)
        return try {
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) {
                    tx.set(
                        docRef,
                        mapOf(
                            "userId" to uid,
                            "date" to date,
                            "records" to memberIds.associateWith { present },
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val existingRecords = parseAttendanceRecords(snap).toMutableMap()
                    memberIds.forEach { id -> existingRecords[id] = present }
                    tx.update(
                        docRef,
                        mapOf(
                            "records" to existingRecords,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setFeeStatus(
        month: String,
        memberId: String,
        paid: Boolean,
        paidDate: String?
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("fees").document(month)
        val statusMap: Map<String, Any> = if (paid && !paidDate.isNullOrBlank()) {
            mapOf("paid" to true, "paidDate" to paidDate)
        } else {
            mapOf("paid" to paid)
        }
        return try {
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) {
                    tx.set(
                        docRef,
                        mapOf(
                            "userId" to uid,
                            "month" to month,
                            "records" to mapOf(memberId to statusMap),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val existingRecords = (snap.get("records") as? Map<*, *>)
                        ?.entries
                        ?.mapNotNull { (k, v) ->
                            if (k is String && v != null) k to v else null
                        }
                        ?.toMap()
                        ?.toMutableMap()
                        ?: mutableMapOf()
                    existingRecords[memberId] = statusMap
                    tx.update(
                        docRef,
                        mapOf(
                            "records" to existingRecords,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun logIfAuthenticated(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    private fun parseAttendanceRecords(snapshot: DocumentSnapshot): Map<String, Boolean> {
        if (!snapshot.exists()) return emptyMap()
        val raw = snapshot.get("records") as? Map<*, *> ?: return emptyMap()
        return buildMap {
            raw.forEach { (k, v) ->
                if (k is String && v is Boolean) {
                    put(k, v)
                }
            }
        }
    }

    private fun parseFeeRecords(snapshot: DocumentSnapshot): Map<String, FeeStatus> {
        if (!snapshot.exists()) return emptyMap()
        val raw = snapshot.get("records") as? Map<*, *> ?: return emptyMap()
        return buildMap {
            raw.forEach { (k, v) ->
                if (k is String && v is Map<*, *>) {
                    val paid = v["paid"] as? Boolean ?: false
                    val paidDate = v["paidDate"] as? String
                    put(k, FeeStatus(paid = paid, paidDate = paidDate))
                }
            }
        }
    }
}
