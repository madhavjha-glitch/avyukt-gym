package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object GymConstants {
    const val FEES_AMOUNT = 500
    const val OWNER_PHONE_RAW = "919990715771"
    const val OWNER_PHONE_DIAL = "+919990715771"
    const val OWNER_PHONE_DISPLAY = "+91 99907 15771"
}

enum class FeeFilter {
    ALL,
    PENDING,
    PAID
}

enum class GymBatch(
    val id: String,
    val label: String,
    val timingHint: String
) {
    MORNING("MORNING", "Morning", "5–10 AM"),
    EVENING("EVENING", "Evening", "5–10 PM"),
    BOTH("BOTH", "Both", "Morning & Evening");

    fun matchesFilter(filter: BatchFilter): Boolean = when (filter) {
        BatchFilter.ALL -> true
        BatchFilter.MORNING -> this == MORNING || this == BOTH
        BatchFilter.EVENING -> this == EVENING || this == BOTH
    }

    companion object {
        fun fromId(raw: String?): GymBatch {
            return entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: MORNING
        }
    }
}

enum class BatchFilter(val label: String) {
    ALL("All"),
    MORNING("Morning"),
    EVENING("Evening")
}

enum class SyncState {
    ONLINE_SYNCED,
    PENDING_SYNC,
    OFFLINE_SAVED
}

enum class DueUrgency {
    PAID,
    OVERDUE,
    DUE_SOON,
    UPCOMING
}

data class MemberDueInfo(
    val dueDate: LocalDate,
    val daysRemaining: Long,
    val urgency: DueUrgency
)

fun computeMemberDueInfo(
    joinDateIso: String,
    isCurrentMonthPaid: Boolean,
    today: LocalDate = LocalDate.now()
): MemberDueInfo {
    val parsedJoin = runCatching { LocalDate.parse(joinDateIso) }.getOrElse { today }
    val currentYm = YearMonth.from(today)
    val clampedDay = parsedJoin.dayOfMonth.coerceAtMost(currentYm.lengthOfMonth())
    val dueDateThisMonth = currentYm.atDay(clampedDay)

    if (isCurrentMonthPaid) {
        val nextYm = currentYm.plusMonths(1)
        val nextDue = nextYm.atDay(parsedJoin.dayOfMonth.coerceAtMost(nextYm.lengthOfMonth()))
        return MemberDueInfo(
            dueDate = nextDue,
            daysRemaining = ChronoUnit.DAYS.between(today, nextDue),
            urgency = DueUrgency.PAID
        )
    }

    val daysUntilDue = ChronoUnit.DAYS.between(today, dueDateThisMonth)
    val urgency = when {
        daysUntilDue < 0L -> DueUrgency.OVERDUE
        daysUntilDue <= 5L -> DueUrgency.DUE_SOON
        else -> DueUrgency.UPCOMING
    }
    return MemberDueInfo(
        dueDate = dueDateThisMonth,
        daysRemaining = daysUntilDue,
        urgency = urgency
    )
}

@IgnoreExtraProperties
data class GymMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val joinDate: String = "",
    val batch: String = GymBatch.MORNING.id,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MembersSnapshot(
    val members: List<GymMember> = emptyList(),
    val hasPendingWrites: Boolean = false,
    val isFromCache: Boolean = false
)

data class FeeStatus(
    val paid: Boolean = false,
    val paidDate: String? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
