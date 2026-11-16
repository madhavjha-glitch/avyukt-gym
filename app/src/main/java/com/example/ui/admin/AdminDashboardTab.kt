package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DueUrgency
import com.example.data.model.FeeFilter
import com.example.data.model.FeeStatus
import com.example.data.model.GymConstants
import com.example.data.model.GymMember
import com.example.data.model.SyncState
import com.example.data.model.UiState
import com.example.data.model.computeMemberDueInfo
import com.example.ui.theme.AvatarPalette
import com.example.ui.theme.GymBlue
import com.example.ui.theme.GymBorder
import com.example.ui.theme.GymCardBg
import com.example.ui.theme.GymElevated
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymOrange
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymTextMuted
import com.example.ui.theme.GymTextPrimary
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AdminDashboardTab(
    membersState: UiState<List<GymMember>>,
    todayAttendance: Map<String, Boolean>,
    currentMonthFees: Map<String, FeeStatus>,
    allFeeMonths: Map<String, Map<String, FeeStatus>>,
    allAttendanceDays: Map<String, Map<String, Boolean>>,
    syncState: SyncState,
    onOpenMembers: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenFees: (FeeFilter) -> Unit,
    onOpenMemberHistory: (GymMember) -> Unit,
    onExportFeesCsv: () -> Unit,
    onExportAttendanceCsv: () -> Unit,
    onExportMembersCsv: () -> Unit
) {
    val todayHeader = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }
    val todayShort = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
    }

    val members = (membersState as? UiState.Success)?.data ?: emptyList()
    val totalMembers = members.size
    val todayPresent = members.count { todayAttendance[it.id] == true }
    val feesPaidCount = members.count { currentMonthFees[it.id]?.paid == true }
    val feesPendingCount = (totalMembers - feesPaidCount).coerceAtLeast(0)
    val collectedInr = feesPaidCount * GymConstants.FEES_AMOUNT

    val dueAlertMembers = remember(members, currentMonthFees) {
        members.map { member ->
            val paid = currentMonthFees[member.id]?.paid == true
            member to computeMemberDueInfo(member.joinDate, paid)
        }.filter { (_, due) ->
            due.urgency == DueUrgency.OVERDUE || due.urgency == DueUrgency.DUE_SOON
        }.sortedBy { (_, due) -> due.daysRemaining }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Dashboard,
                        contentDescription = null,
                        tint = GymOrange
                    )
                    Text(
                        text = "DASHBOARD",
                        style = MaterialTheme.typography.headlineMedium.copy(color = GymTextPrimary)
                    )
                }
                Text(
                    text = todayHeader,
                    style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted)
                )
            }
        }

        if (syncState != SyncState.ONLINE_SYNCED) {
            item {
                val (bannerColor, bannerIcon, bannerMsg) = when (syncState) {
                    SyncState.OFFLINE_SAVED -> Triple(
                        GymOrange,
                        Icons.Rounded.CloudOff,
                        "Offline Mode active — saare changes device par save ho rahe hain aur internet aate hi cloud me sync honge."
                    )
                    SyncState.PENDING_SYNC -> Triple(
                        GymBlue,
                        Icons.Rounded.CloudDone,
                        "Local changes cloud me sync ho rahe hain..."
                    )
                    SyncState.ONLINE_SYNCED -> Triple(GymGreen, Icons.Rounded.CloudDone, "")
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = bannerColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = bannerIcon,
                            contentDescription = null,
                            tint = bannerColor
                        )
                        Text(
                            text = bannerMsg,
                            style = MaterialTheme.typography.bodySmall.copy(color = bannerColor)
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardStatCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dash_card_members"),
                        icon = Icons.Rounded.Group,
                        value = totalMembers.toString(),
                        label = stringResource(R.string.total_members),
                        accent = GymOrange,
                        onClick = onOpenMembers
                    )
                    DashboardStatCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dash_card_attendance"),
                        icon = Icons.Rounded.HowToReg,
                        value = todayPresent.toString(),
                        label = stringResource(R.string.today_present_label),
                        accent = GymBlue,
                        onClick = onOpenAttendance
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardStatCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dash_card_fees_paid"),
                        icon = Icons.Rounded.CurrencyRupee,
                        value = "₹${currencyFormatter.format(collectedInr)}",
                        label = stringResource(R.string.fees_collected_label),
                        accent = GymGreen,
                        onClick = { onOpenFees(FeeFilter.PAID) }
                    )
                    DashboardStatCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dash_card_fees_pending"),
                        icon = Icons.Rounded.HourglassEmpty,
                        value = feesPendingCount.toString(),
                        label = stringResource(R.string.fees_pending_label),
                        accent = GymRed,
                        onClick = { onOpenFees(FeeFilter.PENDING) }
                    )
                }
            }
        }

        if (dueAlertMembers.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dash_expiry_alert_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = GymCardBg,
                    border = BorderStroke(1.dp, GymRed.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = GymRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${stringResource(R.string.expiry_alert_title)} (${dueAlertMembers.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(color = GymTextPrimary)
                                )
                            }
                            TextButton(
                                onClick = { onOpenFees(FeeFilter.PENDING) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("View All", color = GymOrange)
                            }
                        }
                        HorizontalDivider(color = GymBorder)
                        dueAlertMembers.take(4).forEach { (member, due) ->
                            val badgeColor = if (due.urgency == DueUrgency.OVERDUE) GymRed else GymOrange
                            val badgeText = when {
                                due.daysRemaining < 0L -> "Overdue ${-due.daysRemaining}d"
                                due.daysRemaining == 0L -> "Due Today"
                                else -> "Due in ${due.daysRemaining}d"
                            }
                            Surface(
                                onClick = { onOpenMemberHistory(member) },
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = GymTextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Due: ${due.dueDate.format(DateTimeFormatter.ofPattern("dd MMM"))} • ₹${GymConstants.FEES_AMOUNT}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = GymTextMuted)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = badgeColor.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Analytics & Trends Card
        item {
            AdminAnalyticsCard(
                members = members,
                allFeeMonths = allFeeMonths,
                allAttendanceDays = allAttendanceDays
            )
        }

        // Backup & Export CSV Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = GymCardBg,
                border = BorderStroke(1.dp, GymBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = GymOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.backup_export_title),
                            style = MaterialTheme.typography.titleSmall.copy(color = GymTextPrimary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dash_export_fees_csv"),
                            label = stringResource(R.string.export_fees_csv),
                            enabled = members.isNotEmpty(),
                            onClick = onExportFeesCsv
                        )
                        ExportActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dash_export_attendance_csv"),
                            label = stringResource(R.string.export_attendance_csv),
                            enabled = members.isNotEmpty(),
                            onClick = onExportAttendanceCsv
                        )
                        ExportActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dash_export_members_csv"),
                            label = stringResource(R.string.export_members_csv),
                            enabled = members.isNotEmpty(),
                            onClick = onExportMembersCsv
                        )
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = GymCardBg,
                border = BorderStroke(1.dp, GymBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.today_attendance_title),
                            style = MaterialTheme.typography.titleMedium.copy(color = GymTextPrimary)
                        )
                        Text(
                            text = todayShort,
                            style = MaterialTheme.typography.labelSmall.copy(color = GymTextMuted)
                        )
                    }
                    HorizontalDivider(color = GymBorder)

                    when (membersState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = GymOrange)
                            }
                        }
                        is UiState.Error -> {
                            Text(
                                text = membersState.message,
                                style = MaterialTheme.typography.bodyMedium.copy(color = GymRed),
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                        is UiState.Success -> {
                            if (members.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(36.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Group,
                                        contentDescription = null,
                                        tint = GymTextMuted.copy(alpha = 0.5f),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Koi member nahi hai. Members tab se add karo.",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (membersState is UiState.Success && members.isNotEmpty()) {
            val preview = members.take(6)
            items(preview, key = { it.id }) { member ->
                val isPresent = todayAttendance[member.id] == true
                Surface(
                    onClick = { onOpenMemberHistory(member) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = GymCardBg,
                    border = BorderStroke(1.dp, GymBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MemberAvatarBadge(memberId = member.id, name = member.name)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = GymTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = member.phone,
                                style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted)
                            )
                        }
                        Icon(
                            imageVector = if (isPresent) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (isPresent) "Present" else "Absent",
                            tint = if (isPresent) GymGreen else GymTextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun ExportActionButton(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GymBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GymElevated,
            contentColor = GymTextPrimary
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = GymCardBg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(color = GymTextMuted)
            )
        }
    }
}

@Composable
fun MemberAvatarBadge(
    memberId: String,
    name: String
) {
    val color = AvatarPalette[(memberId.firstOrNull()?.code ?: 0) % AvatarPalette.size]
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium.copy(
                color = color,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
