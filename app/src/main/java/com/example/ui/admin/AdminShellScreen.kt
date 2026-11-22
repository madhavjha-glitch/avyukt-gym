package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.GymMember
import com.example.data.model.SyncState
import com.example.data.model.UiState
import com.example.ui.theme.GymAccent
import com.example.ui.theme.GymAccentSoft
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymInk
import com.example.ui.theme.GymInkMuted
import com.example.ui.theme.GymInkSecondary
import com.example.ui.theme.GymLine
import com.example.ui.theme.GymPaper
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymSurface
import com.example.util.CsvExportHelper
import com.example.util.FeeReceiptHelper

enum class AdminTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME(R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    MEMBERS(R.string.nav_members, Icons.Rounded.Group, Icons.Outlined.Group, "nav_members"),
    ATTENDANCE(R.string.nav_attendance, Icons.Rounded.EventAvailable, Icons.Outlined.EventAvailable, "nav_attendance"),
    FEES(R.string.nav_fees, Icons.Rounded.CurrencyRupee, Icons.Outlined.CurrencyRupee, "nav_fees")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShellScreen(
    viewModel: AdminViewModel,
    onSignOut: () -> Unit,
    onBackToPublicHome: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by rememberSaveable { mutableStateOf(AdminTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }

    val membersState by viewModel.membersState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val currentMonthFees by viewModel.currentMonthFees.collectAsStateWithLifecycle()
    val selectedAttDate by viewModel.selectedAttDate.collectAsStateWithLifecycle()
    val attendanceForDate by viewModel.attendanceForSelectedDate.collectAsStateWithLifecycle()
    val monthlyCounts by viewModel.monthlyAttendanceCounts.collectAsStateWithLifecycle()
    val allAttendanceDays by viewModel.allAttendanceDays.collectAsStateWithLifecycle()
    val allFeeMonths by viewModel.allFeeMonths.collectAsStateWithLifecycle()
    val selectedFeesMonth by viewModel.selectedFeesMonth.collectAsStateWithLifecycle()
    val feesStateForMonth by viewModel.feesStateForSelectedMonth.collectAsStateWithLifecycle()
    val selectedFeeFilter by viewModel.selectedFeeFilter.collectAsStateWithLifecycle()
    val selectedBatchFilter by viewModel.selectedBatchFilter.collectAsStateWithLifecycle()
    val selectedAttBatchFilter by viewModel.selectedAttBatchFilter.collectAsStateWithLifecycle()
    val togglingFeeMemberIds by viewModel.togglingFeeMemberIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.memberSearchQuery.collectAsStateWithLifecycle()
    val isSavingMember by viewModel.isSavingMember.collectAsStateWithLifecycle()

    var memberForHistoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var memberForEditFromHistory by remember { mutableStateOf<GymMember?>(null) }

    val allMembers = (membersState as? UiState.Success)?.data ?: emptyList()
    val memberForHistory = remember(allMembers, memberForHistoryId) {
        memberForHistoryId?.let { id -> allMembers.find { it.id == id } }
    }

    val shareCsv: (fileName: String, content: String, chooserTitle: String) -> Unit = { fileName, content, title ->
        val result = CsvExportHelper.shareCsvFile(
            context = context,
            fileName = fileName,
            csvContent = content,
            chooserTitle = title
        )
        if (result.isFailure) {
            viewModel.notifyMessage("CSV export share nahi ho saka", AdminToastType.ERROR)
        }
    }

    val exportFeesCsv: () -> Unit = {
        shareCsv(
            "avyukt_gym_fees_${selectedFeesMonth}.csv",
            viewModel.buildSelectedMonthFeesCsv(),
            "Fees CSV Export"
        )
    }

    val exportAttendanceCsv: () -> Unit = {
        val monthPrefix = selectedAttDate.take(7)
        shareCsv(
            "avyukt_gym_attendance_${monthPrefix}.csv",
            viewModel.buildSelectedMonthAttendanceCsv(),
            "Attendance CSV Export"
        )
    }

    val exportMembersCsv: () -> Unit = {
        shareCsv(
            "avyukt_gym_members_backup_${AdminViewModel.todayIso()}.csv",
            viewModel.buildMembersBackupCsv(),
            "Members Backup CSV"
        )
    }

    val receiptChooserTitle = stringResource(R.string.receipt_share_chooser)
    val shareFeeReceipt: (GymMember, String, String?) -> Unit = { member, month, paidDate ->
        val result = FeeReceiptHelper.generateAndShareReceiptPdf(
            context = context,
            member = member,
            month = month,
            paidDate = paidDate,
            chooserTitle = receiptChooserTitle
        )
        if (result.isFailure) {
            viewModel.notifyMessage("Receipt PDF share nahi ho saka", AdminToastType.ERROR)
        }
    }

    BackHandler(enabled = activeTab != AdminTab.HOME) {
        activeTab = AdminTab.HOME
    }

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { toast ->
            snackbarHostState.showSnackbar(toast.message)
        }
    }

    val (statusText, statusColor, statusIcon) = when (membersState) {
        is UiState.Loading -> Triple(
            stringResource(R.string.syncing_status),
            GymAccent,
            Icons.Rounded.CloudSync
        )
        is UiState.Error -> Triple("Error", GymRed, Icons.Rounded.CloudOff)
        is UiState.Success -> when (syncState) {
            SyncState.OFFLINE_SAVED -> Triple(
                stringResource(R.string.offline_saved_status),
                GymAccent,
                Icons.Rounded.CloudOff
            )
            SyncState.PENDING_SYNC -> Triple(
                stringResource(R.string.pending_sync_status),
                GymAccent,
                Icons.Rounded.CloudSync
            )
            SyncState.ONLINE_SYNCED -> Triple(
                stringResource(R.string.cloud_connected),
                GymGreen,
                Icons.Rounded.CloudDone
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        Scaffold(
            containerColor = GymPaper,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = GymPaper,
                            titleContentColor = GymInk
                        ),
                        navigationIcon = {
                            IconButton(
                                onClick = onBackToPublicHome,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("admin_back_home_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.back_to_gym_home),
                                    tint = GymInk
                                )
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.gym_logo),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .border(1.dp, GymLine, RoundedCornerShape(9.dp))
                                )
                                Column {
                                    Text(
                                        text = buildAnnotatedString {
                                            append("AVYUKT ")
                                            withStyle(SpanStyle(color = GymAccent)) {
                                                append("GYM")
                                            }
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp,
                                            color = GymInk
                                        )
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = statusIcon,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = statusColor,
                                                letterSpacing = 0.sp
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("admin_sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = stringResource(R.string.sign_out_button),
                                    tint = GymInkSecondary
                                )
                            }
                        }
                    )
                    HorizontalDivider(color = GymLine)
                }
            },
            bottomBar = {
                if (!isExpanded) {
                    Column {
                        HorizontalDivider(color = GymLine)
                        NavigationBar(
                            containerColor = GymSurface,
                            tonalElevation = 0.dp,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            AdminTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { activeTab = tab },
                                    modifier = Modifier.testTag(tab.tag),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                letterSpacing = 0.2.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = GymAccent,
                                        selectedTextColor = GymAccent,
                                        indicatorColor = GymAccentSoft,
                                        unselectedIconColor = GymInkMuted,
                                        unselectedTextColor = GymInkMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    NavigationRail(
                        containerColor = GymSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AdminTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                modifier = Modifier.testTag(tab.tag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 0.2.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = GymAccent,
                                    selectedTextColor = GymAccent,
                                    indicatorColor = GymAccentSoft,
                                    unselectedIconColor = GymInkMuted,
                                    unselectedTextColor = GymInkMuted
                                )
                            )
                        }
                    }
                    VerticalDivider(color = GymLine)
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize()
                    ) {
                        when (activeTab) {
                            AdminTab.HOME -> AdminDashboardTab(
                                membersState = membersState,
                                todayAttendance = todayAttendance,
                                currentMonthFees = currentMonthFees,
                                allFeeMonths = allFeeMonths,
                                allAttendanceDays = allAttendanceDays,
                                syncState = syncState,
                                onOpenMembers = { activeTab = AdminTab.MEMBERS },
                                onOpenAttendance = { activeTab = AdminTab.ATTENDANCE },
                                onOpenFees = { filter ->
                                    viewModel.setFeesMonth(AdminViewModel.currentMonthIso())
                                    viewModel.setFeeFilter(filter)
                                    activeTab = AdminTab.FEES
                                },
                                onOpenMemberHistory = { member ->
                                    memberForHistoryId = member.id
                                },
                                onExportFeesCsv = exportFeesCsv,
                                onExportAttendanceCsv = exportAttendanceCsv,
                                onExportMembersCsv = exportMembersCsv
                            )
                            AdminTab.MEMBERS -> AdminMembersTab(
                                membersState = membersState,
                                currentMonthFees = currentMonthFees,
                                searchQuery = searchQuery,
                                selectedBatchFilter = selectedBatchFilter,
                                isSavingMember = isSavingMember,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onBatchFilterChange = viewModel::setBatchFilter,
                                onAddMember = viewModel::addMember,
                                onUpdateMember = viewModel::updateMember,
                                onDeleteMember = viewModel::deleteMember,
                                onOpenMemberHistory = { member ->
                                    memberForHistoryId = member.id
                                },
                                onExportMembersCsv = exportMembersCsv
                            )
                            AdminTab.ATTENDANCE -> AdminAttendanceTab(
                                membersState = membersState,
                                selectedDate = selectedAttDate,
                                selectedBatchFilter = selectedAttBatchFilter,
                                attendanceMap = attendanceForDate,
                                allAttendanceDays = allAttendanceDays,
                                monthlyCounts = monthlyCounts,
                                onDateChange = viewModel::setAttDate,
                                onBatchFilterChange = viewModel::setAttBatchFilter,
                                onToggleMember = viewModel::toggleAttendance,
                                onMarkAll = viewModel::markAllAttendance,
                                onOpenMemberHistory = { member ->
                                    memberForHistoryId = member.id
                                },
                                onExportAttendanceCsv = exportAttendanceCsv
                            )
                            AdminTab.FEES -> AdminFeesTab(
                                membersState = membersState,
                                feesState = feesStateForMonth,
                                allFeeMonths = allFeeMonths,
                                selectedMonth = selectedFeesMonth,
                                selectedFilter = selectedFeeFilter,
                                togglingMemberIds = togglingFeeMemberIds,
                                onShiftMonth = viewModel::shiftFeesMonth,
                                onSelectFilter = viewModel::setFeeFilter,
                                onToggleFee = viewModel::toggleFeeStatus,
                                onSetFeePaid = viewModel::setMemberFeePaid,
                                onRetryFees = viewModel::retryFees,
                                onBuildReport = viewModel::buildWhatsAppFeesReport,
                                onBuildMemberReminder = viewModel::buildMemberFeeReminder,
                                onOpenMemberHistory = { member ->
                                    memberForHistoryId = member.id
                                },
                                onShareReceipt = shareFeeReceipt,
                                onExportFeesCsv = exportFeesCsv
                            )
                        }
                    }
                }
            }
        }
    }

    if (memberForHistory != null) {
        MemberHistoryBottomSheet(
            member = memberForHistory,
            allAttendanceDays = allAttendanceDays,
            allFeeMonths = allFeeMonths,
            togglingMemberIds = togglingFeeMemberIds,
            onDismiss = { memberForHistoryId = null },
            onEditMember = { target ->
                memberForEditFromHistory = target
            },
            onSetFeeForMonth = viewModel::setMemberFeePaidForMonth,
            onBuildReminder = viewModel::buildMemberFeeReminder,
            onShareReceipt = shareFeeReceipt
        )
    }

    val editTargetFromHistory = memberForEditFromHistory
    if (editTargetFromHistory != null) {
        MemberFormDialog(
            existingMember = editTargetFromHistory,
            isSaving = isSavingMember,
            onDismiss = { memberForEditFromHistory = null },
            onSubmit = { name, phone, joinDate, batch ->
                viewModel.updateMember(
                    memberId = editTargetFromHistory.id,
                    name = name,
                    phone = phone,
                    joinDate = joinDate,
                    batch = batch,
                    onSuccess = { memberForEditFromHistory = null }
                )
            }
        )
    }
}
