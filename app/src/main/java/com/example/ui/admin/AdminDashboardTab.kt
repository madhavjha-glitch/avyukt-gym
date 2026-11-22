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
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.GymAccent
import com.example.ui.theme.GymBlue
import com.example.ui.theme.GymElevated
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymInk
import com.example.ui.theme.GymInkMuted
import com.example.ui.theme.GymInkSecondary
import com.example.ui.theme.GymLine
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymSurface
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
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = GymInk,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = todayHeader,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = GymInkMuted,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        if (syncState != SyncState.ONLINE_SYNCED) {
            item {
                val (bannerColor, bannerIcon, bannerMsg) = when (syncState) {
                    SyncState.OFFLINE_SAVED -> Triple(
                        GymAccent,
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
                    shape = RoundedCornerShape(16.dp),
                    color = bannerColor.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
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
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GymInk,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dash_card_members"),
                        icon = Icons.Rounded.Group,
                        value = totalMembers.toString(),
                        label = stringResource(R.string.total_members),
                        accent = GymAccent,
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    shape = RoundedCornerShape(20.dp),
                    color = GymSurface,
                    border = BorderStroke(1.dp, GymLine)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
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
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${stringResource(R.string.expiry_alert_title)} (${dueAlertMembers.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = GymInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            TextButton(
                                onClick = { onOpenFees(FeeFilter.PENDING) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    text = "View All",
                                    color = GymAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        HorizontalDivider(color = GymLine)
                        dueAlertMembers.take(4).forEachIndexed { idx, (member, due) ->
                            if (idx > 0) {
                                HorizontalDivider(color = GymLine)
                            }
                            val badgeColor = if (due.urgency == DueUrgency.OVERDUE) GymRed else GymAccent
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
                                        .padding(horizontal = 18.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = GymInk,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Due: ${due.dueDate.format(DateTimeFormatter.ofPattern("dd MMM"))} • ₹${GymConstants.FEES_AMOUNT}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = GymInkMuted)
                                        )
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = badgeColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            AdminAnalyticsCard(
                members = members,
                allFeeMonths = allFeeMonths,
                allAttendanceDays = allAttendanceDays
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = GymSurface,
                border = BorderStroke(1.dp, GymLine)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = GymInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.backup_export_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = GymInk,
                                fontWeight = FontWeight.Bold
                            )
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
                shape = RoundedCornerShape(20.dp),
                color = GymSurface,
                border = BorderStroke(1.dp, GymLine)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.today_attendance_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = GymInk,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = todayShort,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GymInkMuted,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                    HorizontalDivider(color = GymLine)

                    when (membersState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = GymInk)
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
                                        tint = GymInkMuted.copy(alpha = 0.5f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Koi member nahi hai. Members tab se add karo.",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = GymInkMuted)
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
                    shape = RoundedCornerShape(18.dp),
                    color = GymSurface,
                    border = BorderStroke(1.dp, GymLine)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MemberAvatarBadge(memberId = member.id, name = member.name)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = GymInk,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = member.phone,
                                style = MaterialTheme.typography.bodySmall.copy(color = GymInkSecondary)
                            )
                        }
                        Icon(
                            imageVector = if (isPresent) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (isPresent) "Present" else "Absent",
                            tint = if (isPresent) GymGreen else GymInkMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
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
        shape = CircleShape,
        border = BorderStroke(1.dp, GymLine),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GymElevated.copy(alpha = 0.55f),
            contentColor = GymInk
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.sp,
                fontWeight = FontWeight.SemiBold
            ),
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
        shape = RoundedCornerShape(20.dp),
        color = GymSurface,
        border = BorderStroke(1.dp, GymLine)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.8).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GymInkMuted,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
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
            .size(42.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
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
