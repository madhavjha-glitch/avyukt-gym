package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DueUrgency
import com.example.data.model.FeeStatus
import com.example.data.model.GymBatch
import com.example.data.model.GymConstants
import com.example.data.model.GymMember
import com.example.data.model.computeMemberDueInfo
import com.example.ui.theme.GymAccent
import com.example.ui.theme.GymBlue
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymInk
import com.example.ui.theme.GymInkMuted
import com.example.ui.theme.GymInkSecondary
import com.example.ui.theme.GymLine
import com.example.ui.theme.GymPaper
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymSurface
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private enum class HistorySubTab { FEES, ATTENDANCE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberHistoryBottomSheet(
    member: GymMember,
    allAttendanceDays: Map<String, Map<String, Boolean>>,
    allFeeMonths: Map<String, Map<String, FeeStatus>>,
    togglingMemberIds: Set<String>,
    onDismiss: () -> Unit,
    onEditMember: (GymMember) -> Unit,
    onSetFeeForMonth: (month: String, memberId: String, paid: Boolean) -> Unit,
    onBuildReminder: (GymMember, String) -> String,
    onShareReceipt: (member: GymMember, month: String, paidDate: String?) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var subTab by remember { mutableStateOf(HistorySubTab.FEES) }

    val batch = GymBatch.fromId(member.batch)
    val currentMonth = remember { AdminViewModel.currentMonthIso() }
    val currentPaid = allFeeMonths[currentMonth]?.get(member.id)?.paid == true
    val dueInfo = remember(member.joinDate, currentPaid) {
        computeMemberDueInfo(member.joinDate, currentPaid)
    }

    val memberAttendanceEntries = remember(allAttendanceDays, member.id) {
        allAttendanceDays.entries
            .mapNotNull { (date, records) ->
                val mark = records[member.id] ?: return@mapNotNull null
                date to mark
            }
            .sortedByDescending { it.first }
    }
    val totalPresentDays = memberAttendanceEntries.count { it.second }
    val thisMonthPresentDays = memberAttendanceEntries.count { (date, present) ->
        present && date.startsWith(currentMonth)
    }

    val feeMonthsList = remember(allFeeMonths, member.joinDate, currentMonth) {
        val currentYm = YearMonth.now()
        val joinYm = runCatching { YearMonth.from(LocalDate.parse(member.joinDate)) }
            .getOrElse { currentYm.minusMonths(5) }
        val earliestYm = maxOf(joinYm, currentYm.minusMonths(11))

        val rangeMonths = buildSet {
            var cursor = currentYm
            while (!cursor.isBefore(earliestYm)) {
                add(cursor.toString())
                cursor = cursor.minusMonths(1)
            }
            addAll(allFeeMonths.keys)
        }
        rangeMonths.sortedDescending()
    }

    val isSavingFee = member.id in togglingMemberIds

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GymSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemberAvatarBadge(memberId = member.id, name = member.name)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleLarge.copy(color = GymInk)
                    )
                    Text(
                        text = "${member.phone} • ${batch.label} (${batch.timingHint})",
                        style = MaterialTheme.typography.bodySmall.copy(color = GymInkSecondary)
                    )
                    Text(
                        text = "Joining: ${AdminViewModel.formatDisplayDate(member.joinDate)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GymInkMuted,
                            letterSpacing = 0.sp
                        )
                    )
                }

                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.phone}"))
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Call ${member.name}",
                        tint = GymGreen
                    )
                }

                IconButton(
                    onClick = { onEditMember(member) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("history_edit_member_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit ${member.name}",
                        tint = GymInkSecondary
                    )
                }
            }

            val (dueColor, dueLabel) = when (dueInfo.urgency) {
                DueUrgency.PAID -> GymGreen to "Current Month Paid • Next due ${dueInfo.dueDate.format(DateTimeFormatter.ofPattern("dd MMM"))}"
                DueUrgency.OVERDUE -> GymRed to "Overdue by ${-dueInfo.daysRemaining}d (Due ${dueInfo.dueDate.format(DateTimeFormatter.ofPattern("dd MMM"))})"
                DueUrgency.DUE_SOON -> GymAccent to if (dueInfo.daysRemaining == 0L) "Fees Due Today!" else "Due in ${dueInfo.daysRemaining}d (${dueInfo.dueDate.format(DateTimeFormatter.ofPattern("dd MMM"))})"
                DueUrgency.UPCOMING -> GymBlue to "Next due: ${dueInfo.dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = dueColor.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, dueColor.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (dueInfo.urgency == DueUrgency.PAID) Icons.Rounded.CheckCircle else Icons.Rounded.HourglassEmpty,
                            contentDescription = null,
                            tint = dueColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = dueLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = dueColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    if (!currentPaid) {
                        OutlinedButton(
                            onClick = {
                                val digits = member.phone.filter { it.isDigit() }
                                val waPhone = if (digits.length == 10) "91$digits" else digits
                                val msg = onBuildReminder(member, currentMonth)
                                val url = "https://wa.me/$waPhone?text=${Uri.encode(msg)}"
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, GymGreen.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = GymGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remind",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GymGreen,
                                    letterSpacing = 0.sp
                                )
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = GymPaper,
                    border = BorderStroke(1.dp, GymLine)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$thisMonthPresentDays days",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = GymAccent,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = "This Month ($totalPresentDays total)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GymInkMuted,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }

                val paidMonthsCount = feeMonthsList.count { allFeeMonths[it]?.get(member.id)?.paid == true }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = GymPaper,
                    border = BorderStroke(1.dp, GymLine)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$paidMonthsCount / ${feeMonthsList.size} mo",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = GymGreen,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = "Months Fees Paid",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GymInkMuted,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryTabChip(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("history_tab_fees"),
                    label = stringResource(R.string.fees_history_label),
                    selected = subTab == HistorySubTab.FEES,
                    onClick = { subTab = HistorySubTab.FEES }
                )
                HistoryTabChip(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("history_tab_attendance"),
                    label = stringResource(R.string.attendance_history_label),
                    selected = subTab == HistorySubTab.ATTENDANCE,
                    onClick = { subTab = HistorySubTab.ATTENDANCE }
                )
            }

            HorizontalDivider(color = GymLine)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (subTab == HistorySubTab.FEES) {
                    items(feeMonthsList, key = { it }) { month ->
                        val fee = allFeeMonths[month]?.get(member.id)
                        val isPaid = fee?.paid == true
                        val accent = if (isPaid) GymGreen else GymRed
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = GymPaper,
                            border = BorderStroke(1.dp, GymLine)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = AdminViewModel.formatMonthLabel(month),
                                        style = MaterialTheme.typography.titleSmall.copy(color = GymInk)
                                    )
                                    Text(
                                        text = if (isPaid && !fee?.paidDate.isNullOrBlank()) {
                                            "Paid on ${AdminViewModel.formatDisplayDate(fee!!.paidDate!!)}"
                                        } else {
                                            "₹${GymConstants.FEES_AMOUNT} • ${if (isPaid) "Paid" else "Pending"}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = accent,
                                            letterSpacing = 0.sp
                                        )
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isPaid) {
                                        IconButton(
                                            onClick = { onShareReceipt(member, month, fee?.paidDate) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("history_receipt_$month")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ReceiptLong,
                                                contentDescription = stringResource(R.string.receipt_share_btn),
                                                tint = GymGreen
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onSetFeeForMonth(month, member.id, !isPaid) },
                                        enabled = !isSavingFee,
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(if (isPaid) R.string.paid_badge else R.string.pending_badge),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = accent,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (memberAttendanceEntries.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EventAvailable,
                                    contentDescription = null,
                                    tint = GymInkMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Abhi tak koi attendance record nahi hai.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = GymInkMuted)
                                )
                            }
                        }
                    } else {
                        items(memberAttendanceEntries, key = { it.first }) { (date, present) ->
                            val accent = if (present) GymGreen else GymRed
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = GymPaper,
                                border = BorderStroke(1.dp, GymLine)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = AdminViewModel.formatDisplayDate(date),
                                        style = MaterialTheme.typography.bodyMedium.copy(color = GymInk)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (present) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(if (present) R.string.present_label else R.string.absent_label),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = accent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTabChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = CircleShape,
        color = if (selected) GymInk else GymPaper,
        border = BorderStroke(1.dp, if (selected) GymInk else GymLine)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (selected) GymPaper else GymInkSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
}
