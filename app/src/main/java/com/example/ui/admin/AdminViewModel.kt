package com.example.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BatchFilter
import com.example.data.model.DueUrgency
import com.example.data.model.FeeFilter
import com.example.data.model.FeeStatus
import com.example.data.model.GymBatch
import com.example.data.model.GymConstants
import com.example.data.model.GymMember
import com.example.data.model.MembersSnapshot
import com.example.data.model.SyncState
import com.example.data.model.UiState
import com.example.data.model.computeMemberDueInfo
import com.example.data.repository.GymRepository
import com.example.util.CsvExportHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class AdminToastType { SUCCESS, ERROR, INFO }

data class AdminToast(
    val message: String,
    val type: AdminToastType = AdminToastType.INFO
)

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val repository: GymRepository,
    val currentUserId: String,
    isOnlineFlow: Flow<Boolean> = flowOf(true)
) : ViewModel() {

    private val _selectedAttDate = MutableStateFlow(todayIso())
    val selectedAttDate: StateFlow<String> = _selectedAttDate.asStateFlow()

    private val _selectedFeesMonth = MutableStateFlow(currentMonthIso())
    val selectedFeesMonth: StateFlow<String> = _selectedFeesMonth.asStateFlow()

    private val _selectedFeeFilter = MutableStateFlow(FeeFilter.ALL)
    val selectedFeeFilter: StateFlow<FeeFilter> = _selectedFeeFilter.asStateFlow()

    private val _selectedBatchFilter = MutableStateFlow(BatchFilter.ALL)
    val selectedBatchFilter: StateFlow<BatchFilter> = _selectedBatchFilter.asStateFlow()

    private val _selectedAttBatchFilter = MutableStateFlow(BatchFilter.ALL)
    val selectedAttBatchFilter: StateFlow<BatchFilter> = _selectedAttBatchFilter.asStateFlow()

    private val _feesRetryNonce = MutableStateFlow(0)

    private val _togglingFeeMemberIds = MutableStateFlow<Set<String>>(emptySet())
    val togglingFeeMemberIds: StateFlow<Set<String>> = _togglingFeeMemberIds.asStateFlow()

    private val _memberSearchQuery = MutableStateFlow("")
    val memberSearchQuery: StateFlow<String> = _memberSearchQuery.asStateFlow()

    private val _isSavingMember = MutableStateFlow(false)
    val isSavingMember: StateFlow<Boolean> = _isSavingMember.asStateFlow()

    private val _toastEvents = MutableSharedFlow<AdminToast>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<AdminToast> = _toastEvents.asSharedFlow()

    private val membersSnapshotFlow: StateFlow<UiState<MembersSnapshot>> = repository.observeMembersSnapshot()
        .asUiState("Failed to observe members")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val membersState: StateFlow<UiState<List<GymMember>>> = membersSnapshotFlow
        .map { state ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(state.message)
                is UiState.Success -> UiState.Success(state.data.members)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val syncState: StateFlow<SyncState> = combine(isOnlineFlow, membersSnapshotFlow) { isOnline, snapshotState ->
        val snap = (snapshotState as? UiState.Success)?.data
        when {
            !isOnline -> SyncState.OFFLINE_SAVED
            snap?.hasPendingWrites == true -> SyncState.PENDING_SYNC
            else -> SyncState.ONLINE_SYNCED
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SyncState.ONLINE_SYNCED
    )

    val todayAttendance: StateFlow<Map<String, Boolean>> = repository.observeAttendanceDay(todayIso())
        .catch { emit(emptyMap()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap()
        )

    val currentMonthFees: StateFlow<Map<String, FeeStatus>> = repository.observeFeeMonth(currentMonthIso())
        .catch { emit(emptyMap()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap()
        )

    val attendanceForSelectedDate: StateFlow<Map<String, Boolean>> = _selectedAttDate
        .flatMapLatest { date ->
            repository.observeAttendanceDay(date).catch { emit(emptyMap()) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap()
        )

    val monthlyAttendanceCounts: StateFlow<Map<String, Int>> = _selectedAttDate
        .flatMapLatest { date ->
            val monthPrefix = date.take(7)
            repository.observeMonthlyAttendanceCounts(monthPrefix).catch { emit(emptyMap()) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap()
        )

    val allAttendanceDays: StateFlow<Map<String, Map<String, Boolean>>> = repository.observeAllAttendanceDays()
        .catch { emit(emptyMap()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap()
        )

    val allFeeMonths: StateFlow<Map<String, Map<String, FeeStatus>>> = repository.observeAllFeeMonths()
        .catch { emit(emptyMap()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap()
        )

    val feesStateForSelectedMonth: StateFlow<UiState<Map<String, FeeStatus>>> =
        combine(_selectedFeesMonth, _feesRetryNonce) { month, _ -> month }
            .flatMapLatest { month ->
                repository.observeFeeMonth(month)
                    .asUiState("Fees status load nahi ho saka")
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    fun setAttDate(date: String) {
        _selectedAttDate.value = date
    }

    fun setFeesMonth(month: String) {
        _selectedFeesMonth.value = month
    }

    fun setFeeFilter(filter: FeeFilter) {
        _selectedFeeFilter.value = filter
    }

    fun setBatchFilter(filter: BatchFilter) {
        _selectedBatchFilter.value = filter
    }

    fun setAttBatchFilter(filter: BatchFilter) {
        _selectedAttBatchFilter.value = filter
    }

    fun retryFees() {
        _feesRetryNonce.update { it + 1 }
    }

    fun shiftFeesMonth(deltaMonths: Long) {
        val current = runCatching { YearMonth.parse(_selectedFeesMonth.value) }
            .getOrElse { YearMonth.now() }
        _selectedFeesMonth.value = current.plusMonths(deltaMonths).toString()
    }

    fun updateSearchQuery(query: String) {
        _memberSearchQuery.value = query
    }

    fun addMember(
        name: String,
        phone: String,
        joinDate: String,
        batch: GymBatch,
        onSuccess: () -> Unit
    ) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        val finalDate = joinDate.trim().ifBlank { todayIso() }

        if (trimmedName.isBlank() || trimmedPhone.length < 7) {
            _toastEvents.tryEmit(AdminToast("Naam aur sahi phone number bharo!", AdminToastType.ERROR))
            return
        }

        viewModelScope.launch {
            _isSavingMember.value = true
            val result = repository.addMember(trimmedName, trimmedPhone, finalDate, batch.id)
            _isSavingMember.value = false
            if (result.isSuccess) {
                _toastEvents.tryEmit(AdminToast("$trimmedName add ho gaya!", AdminToastType.SUCCESS))
                onSuccess()
            } else {
                _toastEvents.tryEmit(
                    AdminToast("Save nahi ho saka. Internet check karo.", AdminToastType.ERROR)
                )
            }
        }
    }

    fun updateMember(
        memberId: String,
        name: String,
        phone: String,
        joinDate: String,
        batch: GymBatch,
        onSuccess: () -> Unit
    ) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        val finalDate = joinDate.trim().ifBlank { todayIso() }

        if (trimmedName.isBlank() || trimmedPhone.length < 7) {
            _toastEvents.tryEmit(AdminToast("Naam aur sahi phone number bharo!", AdminToastType.ERROR))
            return
        }

        viewModelScope.launch {
            _isSavingMember.value = true
            val result = repository.updateMember(memberId, trimmedName, trimmedPhone, finalDate, batch.id)
            _isSavingMember.value = false
            if (result.isSuccess) {
                _toastEvents.tryEmit(AdminToast("$trimmedName update ho gaya!", AdminToastType.SUCCESS))
                onSuccess()
            } else {
                _toastEvents.tryEmit(
                    AdminToast("Update nahi ho saka. Internet check karo.", AdminToastType.ERROR)
                )
            }
        }
    }

    fun deleteMember(member: GymMember) {
        viewModelScope.launch {
            val result = repository.deleteMember(member.id)
            if (result.isSuccess) {
                _toastEvents.tryEmit(AdminToast("${member.name} delete ho gaya", AdminToastType.INFO))
            } else {
                _toastEvents.tryEmit(
                    AdminToast("Delete nahi ho saka. Internet check karo.", AdminToastType.ERROR)
                )
            }
        }
    }

    fun toggleAttendance(memberId: String) {
        val date = _selectedAttDate.value
        val wasPresent = attendanceForSelectedDate.value[memberId] == true
        val nextState = !wasPresent
        viewModelScope.launch {
            val result = repository.setAttendance(date, memberId, nextState)
            if (result.isSuccess) {
                _toastEvents.tryEmit(
                    if (nextState) AdminToast("Present mark kiya", AdminToastType.SUCCESS)
                    else AdminToast("Absent mark kiya", AdminToastType.INFO)
                )
            } else {
                _toastEvents.tryEmit(
                    AdminToast("Save nahi ho saka. Internet check karo.", AdminToastType.ERROR)
                )
            }
        }
    }

    fun markAllAttendance(present: Boolean) {
        val members = (membersState.value as? UiState.Success)?.data ?: return
        val filter = _selectedAttBatchFilter.value
        val targetMembers = members.filter { GymBatch.fromId(it.batch).matchesFilter(filter) }
        if (targetMembers.isEmpty()) return
        val date = _selectedAttDate.value
        viewModelScope.launch {
            val result = repository.setAllAttendance(date, targetMembers.map { it.id }, present)
            if (result.isSuccess) {
                val prefix = if (filter == BatchFilter.ALL) "Sabko" else "${filter.label} (${targetMembers.size})"
                _toastEvents.tryEmit(
                    if (present) AdminToast("$prefix Present kiya!", AdminToastType.SUCCESS)
                    else AdminToast("$prefix Absent mark kiya", AdminToastType.INFO)
                )
            } else {
                _toastEvents.tryEmit(
                    AdminToast("Save nahi ho saka. Internet check karo.", AdminToastType.ERROR)
                )
            }
        }
    }

    fun toggleFeeStatus(memberId: String) {
        val currentMap = (feesStateForSelectedMonth.value as? UiState.Success)?.data ?: emptyMap()
        val wasPaid = currentMap[memberId]?.paid == true
        setMemberFeePaidForMonth(month = _selectedFeesMonth.value, memberId = memberId, paid = !wasPaid)
    }

    fun setMemberFeePaid(memberId: String, paid: Boolean) {
        setMemberFeePaidForMonth(month = _selectedFeesMonth.value, memberId = memberId, paid = paid)
    }

    fun setMemberFeePaidForMonth(month: String, memberId: String, paid: Boolean) {
        if (memberId in _togglingFeeMemberIds.value) return
        val paidDate = if (paid) todayIso() else null
        viewModelScope.launch {
            _togglingFeeMemberIds.update { it + memberId }
            try {
                val result = repository.setFeeStatus(month, memberId, paid, paidDate)
                if (result.isSuccess) {
                    _toastEvents.tryEmit(
                        if (paid) AdminToast("Fees Paid mark ho gayi!", AdminToastType.SUCCESS)
                        else AdminToast("Fees Pending mark ho gayi", AdminToastType.INFO)
                    )
                } else {
                    _toastEvents.tryEmit(
                        AdminToast("Fees save nahi ho saki. Internet check karo.", AdminToastType.ERROR)
                    )
                }
            } finally {
                _togglingFeeMemberIds.update { it - memberId }
            }
        }
    }

    fun buildMemberFeeReminder(member: GymMember, month: String = _selectedFeesMonth.value): String {
        val monthLabel = formatMonthLabel(month)
        return "Namaste ${member.name} ji, Avyukt Gym ki $monthLabel mahine ki fees (₹${GymConstants.FEES_AMOUNT}) pending hai. Kripya jaldi jama kar dein. Dhanyavaad!"
    }

    fun buildWhatsAppFeesReport(
        members: List<GymMember>,
        feesMap: Map<String, FeeStatus>
    ): String {
        val monthLabel = formatMonthLabel(_selectedFeesMonth.value)
        val paidMembers = members.filter { feesMap[it.id]?.paid == true }
        val pendingMembers = members.filter { feesMap[it.id]?.paid != true }
        val paidNames = paidMembers.joinToString("\n") { "✅ ${it.name}" }
        val pendingNames = pendingMembers.joinToString("\n") { "❌ ${it.name}" }

        return buildString {
            append("🏋️ *Avyukt Gym – Fees Report*\n")
            append("📅 $monthLabel\n\n")
            append("👥 Total: ${members.size} members\n")
            append("✅ Paid: ${paidMembers.size} (₹${paidMembers.size * GymConstants.FEES_AMOUNT})\n")
            append("❌ Pending: ${pendingMembers.size} (₹${pendingMembers.size * GymConstants.FEES_AMOUNT})\n\n")
            if (paidNames.isNotEmpty()) {
                append("*Fees Paid:*\n$paidNames\n\n")
            }
            if (pendingNames.isNotEmpty()) {
                append("*Fees Baaki:*\n$pendingNames")
            }
        }.trim()
    }

    fun buildDueMembersSummary(
        members: List<GymMember>,
        feesMap: Map<String, FeeStatus>
    ): List<Pair<GymMember, com.example.data.model.MemberDueInfo>> {
        return members
            .map { member ->
                val isPaid = feesMap[member.id]?.paid == true
                member to computeMemberDueInfo(member.joinDate, isPaid)
            }
            .filter { (_, due) ->
                due.urgency == DueUrgency.OVERDUE || due.urgency == DueUrgency.DUE_SOON
            }
            .sortedBy { (_, due) -> due.daysRemaining }
    }

    fun buildMembersBackupCsv(): String {
        val members = (membersState.value as? UiState.Success)?.data ?: emptyList()
        return CsvExportHelper.buildMembersBackupCsv(members)
    }

    fun buildSelectedMonthFeesCsv(): String {
        val members = (membersState.value as? UiState.Success)?.data ?: emptyList()
        val feesMap = (feesStateForSelectedMonth.value as? UiState.Success)?.data ?: currentMonthFees.value
        return CsvExportHelper.buildMonthlyFeesCsv(_selectedFeesMonth.value, members, feesMap)
    }

    fun buildSelectedMonthAttendanceCsv(): String {
        val members = (membersState.value as? UiState.Success)?.data ?: emptyList()
        val monthPrefix = _selectedAttDate.value.take(7)
        return CsvExportHelper.buildMonthlyAttendanceCsv(monthPrefix, members, allAttendanceDays.value)
    }

    fun notifyMessage(message: String, type: AdminToastType = AdminToastType.INFO) {
        _toastEvents.tryEmit(AdminToast(message, type))
    }

    private fun <T> Flow<T>.asUiState(defaultError: String): Flow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(UiState.Error(rootError.message ?: defaultError))
            }

    companion object {
        fun todayIso(): String = LocalDate.now().toString()
        fun currentMonthIso(): String = YearMonth.now().toString()

        fun formatDisplayDate(iso: String): String {
            return runCatching {
                LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            }.getOrDefault(iso)
        }

        fun formatMonthLabel(ym: String): String {
            return runCatching {
                YearMonth.parse(ym).format(DateTimeFormatter.ofPattern("MMM yyyy"))
            }.getOrDefault(ym)
        }
    }
}
