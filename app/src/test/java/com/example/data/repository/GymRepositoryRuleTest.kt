package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class GymRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addObserveAndDeleteMember_authenticatedOwner_succeeds(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GymRepository(firestore, auth)

        val uniqueName = "Rahul ${UUID.randomUUID().toString().take(6)}"
        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMember(uniqueName, "9876543210", "2026-11-16", "MORNING")
        }
        assertTrue("Expected addMember to succeed: ${addResult.exceptionOrNull()}", addResult.isSuccess)
        val memberId = addResult.getOrThrow()

        val members = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMembers().first { list -> list.any { it.id == memberId } }
        }
        assertTrue(members.any { it.id == memberId && it.name == uniqueName && it.batch == "MORNING" })

        val updatedName = "$uniqueName Edited"
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateMember(memberId, updatedName, "9876500000", "2026-11-10", "EVENING")
        }
        assertTrue("Expected updateMember to succeed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val updatedMembers = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMembers().first { list ->
                list.any { it.id == memberId && it.name == updatedName && it.batch == "EVENING" }
            }
        }
        assertTrue(updatedMembers.any { it.id == memberId && it.phone == "9876500000" })

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteMember(memberId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun setAndObserveAttendanceAndFees_authenticatedOwner_succeeds(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GymRepository(firestore, auth)

        val memberId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMember("Vikram Singh", "9811122233", "2026-11-01").getOrThrow()
        }

        val date = "2026-11-16"
        val attResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setAttendance(date, memberId, true)
        }
        assertTrue("Expected setAttendance to succeed: ${attResult.exceptionOrNull()}", attResult.isSuccess)

        val dayRecords = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAttendanceDay(date).first { it[memberId] == true }
        }
        assertEquals(true, dayRecords[memberId])

        val monthCounts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMonthlyAttendanceCounts("2026-11").first { (it[memberId] ?: 0) >= 1 }
        }
        assertTrue((monthCounts[memberId] ?: 0) >= 1)

        val month = "2026-11"
        val feeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setFeeStatus(month, memberId, true, date)
        }
        assertTrue("Expected setFeeStatus(paid=true) to succeed: ${feeResult.exceptionOrNull()}", feeResult.isSuccess)

        val feeRecords = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFeeMonth(month).first { it[memberId]?.paid == true }
        }
        assertEquals(date, feeRecords[memberId]?.paidDate)

        // Toggle back to Pending
        val pendingResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setFeeStatus(month, memberId, false, null)
        }
        assertTrue("Expected setFeeStatus(paid=false) to succeed: ${pendingResult.exceptionOrNull()}", pendingResult.isSuccess)

        val pendingRecords = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFeeMonth(month).first { it[memberId]?.paid == false }
        }
        assertEquals(false, pendingRecords[memberId]?.paid)
        assertEquals(null, pendingRecords[memberId]?.paidDate)

        val allAttDays = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAllAttendanceDays().first { it[date]?.get(memberId) == true }
        }
        assertEquals(true, allAttDays[date]?.get(memberId))

        // Toggle back to Paid again
        val repaidResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setFeeStatus(month, memberId, true, date)
        }
        assertTrue("Expected setFeeStatus(paid=true) again to succeed: ${repaidResult.exceptionOrNull()}", repaidResult.isSuccess)

        val allFeeMonths = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAllFeeMonths().first { it[month]?.get(memberId)?.paid == true }
        }
        assertEquals(date, allFeeMonths[month]?.get(memberId)?.paidDate)

        // Cleanup
        withTimeout(DEFAULT_TIMEOUT_MS) { repository.deleteMember(memberId) }
        Unit
    }

    @Test
    fun crossUserRead_isDeniedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GymRepository(firestore, auth)
        val memberId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addMember("Amit Member", "9999888877", "2026-11-16").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        var caught: Throwable? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("members").document(memberId)
                    .get().await()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreEx = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun unauthenticatedCaller_cannotObserveOrWrite() = runBlocking {
        auth.signOut()
        val repository = GymRepository(firestore, auth)

        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMember("Unauth", "9876543210", "2026-11-16")
        }
        assertFalse(addResult.isSuccess)

        var caught: Throwable? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document("some_uid")
                    .collection("members").get().await()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.unwrapFirestoreException()?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_gym@test.com"
        const val BOB_EMAIL = "bob_gym@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
