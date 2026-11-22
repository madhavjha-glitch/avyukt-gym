package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FeeFilter
import com.example.data.model.FeeStatus
import com.example.data.model.GymBatch
import com.example.data.model.GymConstants
import com.example.data.model.GymMember
import com.example.data.model.UiState
import com.example.ui.theme.GymAccent
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymInk
import com.example.ui.theme.GymInkMuted
import com.example.ui.theme.GymInkSecondary
import com.example.ui.theme.GymLine
import com.example.ui.theme.GymPaper
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymSurface
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminFeesTab(
    membersState: UiState<List<GymMember>>,
    feesState: UiState<Map<String, FeeStatus>>,
    selectedMonth: String,
    selectedFilter: FeeFilter,
    togglingMemberIds: Set<String>,
    onShiftMonth: (Long) -> Unit,
    onSelectFilter: (FeeFilter) -> Unit,
    onToggleFee: (String) -> Unit,
    onSetFeePaid: (String, Boolean) -> Unit,
    onRetryFees: () -> Unit,
    onBuildReport: (List<GymMember>, Map<String, FeeStatus>) -> String,
    onBuildMemberReminder: (GymMember) -> String,
    onOpenMemberHistory: (GymMember) -> Unit,
    onShareReceipt: (member: GymMember, month: String, paidDate: String?) -> Unit,
    onExportFeesCsv: () -> Unit
) {
    val context = LocalContext.current
    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
    }
    var selectedMemberForDialog by remember { mutableStateOf<GymMember?>(null) }

    val members = (membersState as? UiState.Success)?.data ?: emptyList()
    val feesMap = (feesState as? UiState.Success)?.data ?: emptyMap()
    val paidCount = members.count { feesMap[it.id]?.paid == true }
    val pendingCount = (members.size - paidCount).coerceAtLeast(0)

    val displayedMembers = remember(members, feesMap, selectedFilter) {
        val filtered = when (selectedFilter) {
            FeeFilter.ALL -> members
            FeeFilter.PENDING -> members.filter { feesMap[it.id]?.paid != true }
            FeeFilter.PAID -> members.filter { feesMap[it.id]?.paid == true }
        }
        filtered.sortedBy { if (feesMap[it.id]?.paid == true) 1 else 0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fees",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = GymInk,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onExportFeesCsv,
                        enabled = members.isNotEmpty(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("export_fees_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = stringResource(R.string.export_fees_csv),
                            tint = GymInk
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val report = onBuildReport(members, feesMap)
                            val url = "https://wa.me/${GymConstants.OWNER_PHONE_RAW}?text=${Uri.encode(report)}"
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        enabled = members.isNotEmpty(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fees_whatsapp_report_button"),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, GymGreen.copy(alpha = 0.38f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = GymGreen.copy(alpha = 0.1f),
                            contentColor = GymGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.fees_report_btn),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = GymSurface,
                border = BorderStroke(1.dp, GymLine)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onShiftMonth(-1) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fees_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            tint = GymInk
                        )
                    }
                    Text(
                        text = AdminViewModel.formatMonthLabel(selectedMonth),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = GymInk,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(
                        onClick = { onShiftMonth(1) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fees_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next month",
                            tint = GymInk
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeeSummaryCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fees_summary_paid_card"),
                    amountText = "₹${currencyFormatter.format(paidCount * GymConstants.FEES_AMOUNT)}",
                    subtitle = "$paidCount ${stringResource(R.string.fees_collected_sum)}",
                    accent = GymGreen,
                    selected = selectedFilter == FeeFilter.PAID,
                    onClick = {
                        onSelectFilter(if (selectedFilter == FeeFilter.PAID) FeeFilter.ALL else FeeFilter.PAID)
                    }
                )
                FeeSummaryCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fees_summary_pending_card"),
                    amountText = "₹${currencyFormatter.format(pendingCount * GymConstants.FEES_AMOUNT)}",
                    subtitle = "$pendingCount ${stringResource(R.string.fees_pending_sum)}",
                    accent = GymRed,
                    selected = selectedFilter == FeeFilter.PENDING,
                    onClick = {
                        onSelectFilter(if (selectedFilter == FeeFilter.PENDING) FeeFilter.ALL else FeeFilter.PENDING)
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeeFilterChip(
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("fees_filter_all"),
                    label = "${stringResource(R.string.fees_filter_all)} (${members.size})",
                    selected = selectedFilter == FeeFilter.ALL,
                    onClick = { onSelectFilter(FeeFilter.ALL) }
                )
                FeeFilterChip(
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("fees_filter_pending"),
                    label = "${stringResource(R.string.fees_filter_pending)} ($pendingCount)",
                    selected = selectedFilter == FeeFilter.PENDING,
                    onClick = { onSelectFilter(FeeFilter.PENDING) }
                )
                FeeFilterChip(
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("fees_filter_paid"),
                    label = "${stringResource(R.string.fees_filter_paid)} ($paidCount)",
                    selected = selectedFilter == FeeFilter.PAID,
                    onClick = { onSelectFilter(FeeFilter.PAID) }
                )
            }
        }

        if (feesState is UiState.Error) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = GymRed.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, GymRed.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = GymRed
                        )
                        Text(
                            text = feesState.message,
                            style = MaterialTheme.typography.bodySmall.copy(color = GymRed),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onRetryFees,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text(
                                text = stringResource(R.string.retry_label),
                                color = GymAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.fees_rate_header),
                    style = MaterialTheme.typography.labelLarge.copy(color = GymInk)
                )
                Text(
                    text = stringResource(R.string.fees_tap_hint),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GymInkMuted,
                        letterSpacing = 0.sp
                    )
                )
            }
        }

        when {
            membersState is UiState.Loading || feesState is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GymInk)
                    }
                }
            }
            membersState is UiState.Error -> {
                item {
                    Text(
                        text = membersState.message,
                        color = GymRed,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            membersState is UiState.Success -> {
                if (members.isEmpty()) {
                    item {
                        FeesEmptyCard(message = "Members tab se pehle members add karo.")
                    }
                } else if (displayedMembers.isEmpty()) {
                    item {
                        val emptyMsg = when (selectedFilter) {
                            FeeFilter.PENDING -> "Is mahine sabhi members ki fees paid ho chuki hai!"
                            FeeFilter.PAID -> "Is mahine abhi kisi member ki fees paid nahi hai."
                            FeeFilter.ALL -> "Koi member nahi mila."
                        }
                        FeesEmptyCard(message = emptyMsg)
                    }
                } else {
                    items(displayedMembers, key = { it.id }) { member ->
                        val feeStatus = feesMap[member.id]
                        val isPaid = feeStatus?.paid == true
                        val isSaving = member.id in togglingMemberIds
                        val badgeAccent = if (isPaid) GymGreen else GymRed
                        val batchLabel = GymBatch.fromId(member.batch).label

                        Surface(
                            onClick = { selectedMemberForDialog = member },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fee_card_${member.id}"),
                            shape = RoundedCornerShape(18.dp),
                            color = GymSurface,
                            border = BorderStroke(1.dp, GymLine)
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
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = GymInk,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${member.phone} • $batchLabel",
                                        style = MaterialTheme.typography.bodySmall.copy(color = GymInkSecondary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isPaid && !feeStatus?.paidDate.isNullOrBlank()) {
                                        Text(
                                            text = "Paid on: ${AdminViewModel.formatDisplayDate(feeStatus!!.paidDate!!)}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = GymGreen,
                                                letterSpacing = 0.sp
                                            )
                                        )
                                    } else if (!isPaid) {
                                        Text(
                                            text = "₹${GymConstants.FEES_AMOUNT} baaki hai",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = GymRed,
                                                letterSpacing = 0.sp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                if (isPaid) {
                                    IconButton(
                                        onClick = { onShareReceipt(member, selectedMonth, feeStatus?.paidDate) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("fee_receipt_${member.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ReceiptLong,
                                            contentDescription = stringResource(R.string.receipt_share_btn),
                                            tint = GymGreen
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onToggleFee(member.id) },
                                    enabled = !isSaving,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("fee_toggle_${member.id}"),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.35f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = badgeAccent.copy(alpha = 0.1f),
                                        contentColor = badgeAccent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = badgeAccent,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.HourglassEmpty,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = stringResource(if (isPaid) R.string.paid_badge else R.string.pending_badge),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    val dialogMember = selectedMemberForDialog
    if (dialogMember != null) {
        val currentStatus = feesMap[dialogMember.id]
        val isPaid = currentStatus?.paid == true
        val isSaving = dialogMember.id in togglingMemberIds

        AlertDialog(
            onDismissRequest = { selectedMemberForDialog = null },
            containerColor = GymSurface,
            title = {
                Column {
                    Text(
                        text = dialogMember.name,
                        color = GymInk,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${AdminViewModel.formatMonthLabel(selectedMonth)} • ₹${GymConstants.FEES_AMOUNT}",
                        color = GymInkSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = (if (isPaid) GymGreen else GymRed).copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, (if (isPaid) GymGreen else GymRed).copy(alpha = 0.28f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.HourglassEmpty,
                                contentDescription = null,
                                tint = if (isPaid) GymGreen else GymRed
                            )
                            Column {
                                Text(
                                    text = stringResource(if (isPaid) R.string.paid_badge else R.string.pending_badge),
                                    color = if (isPaid) GymGreen else GymRed,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isPaid && !currentStatus?.paidDate.isNullOrBlank()) {
                                    Text(
                                        text = "Paid on: ${AdminViewModel.formatDisplayDate(currentStatus!!.paidDate!!)}",
                                        color = GymInkMuted,
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onSetFeePaid(dialogMember.id, true)
                            selectedMemberForDialog = null
                        },
                        enabled = !isSaving && !isPaid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("dialog_mark_paid_${dialogMember.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GymGreen,
                            contentColor = Color.White
                        ),
                        shape = CircleShape
                    ) {
                        Text(stringResource(R.string.fees_mark_paid))
                    }

                    OutlinedButton(
                        onClick = {
                            onSetFeePaid(dialogMember.id, false)
                            selectedMemberForDialog = null
                        },
                        enabled = !isSaving && isPaid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("dialog_mark_pending_${dialogMember.id}"),
                        border = BorderStroke(1.dp, GymRed.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GymRed),
                        shape = CircleShape
                    ) {
                        Text(stringResource(R.string.fees_mark_pending))
                    }

                    if (isPaid) {
                        OutlinedButton(
                            onClick = {
                                val m = dialogMember
                                val pd = currentStatus?.paidDate
                                selectedMemberForDialog = null
                                onShareReceipt(m, selectedMonth, pd)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("dialog_share_receipt_${dialogMember.id}"),
                            border = BorderStroke(1.dp, GymGreen.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = GymGreen.copy(alpha = 0.08f),
                                contentColor = GymGreen
                            ),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.receipt_share_btn))
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                val digits = dialogMember.phone.filter { it.isDigit() }
                                val waPhone = if (digits.length == 10) "91$digits" else digits
                                val reminder = onBuildMemberReminder(dialogMember)
                                val url = "https://wa.me/$waPhone?text=${Uri.encode(reminder)}"
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("dialog_whatsapp_reminder_${dialogMember.id}"),
                            border = BorderStroke(1.dp, GymGreen.copy(alpha = 0.38f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GymGreen),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.fees_whatsapp_reminder))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val m = dialogMember
                            selectedMemberForDialog = null
                            onOpenMemberHistory(m)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("dialog_view_history_${dialogMember.id}"),
                        border = BorderStroke(1.dp, GymLine),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GymInk),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.member_history_title))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMemberForDialog = null }) {
                    Text("Close", color = GymInkMuted)
                }
            }
        )
    }
}

@Composable
private fun FeeSummaryCard(
    modifier: Modifier = Modifier,
    amountText: String,
    subtitle: String,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = GymSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else GymLine)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = accent,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (selected) accent else GymInkMuted,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeeFilterChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = CircleShape,
        color = if (selected) GymInk else GymSurface,
        border = BorderStroke(1.dp, if (selected) GymInk else GymLine)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (selected) GymPaper else GymInkSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeesEmptyCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GymSurface,
        border = BorderStroke(1.dp, GymLine)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.CurrencyRupee,
                contentDescription = null,
                tint = GymInkMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(color = GymInkMuted)
            )
        }
    }
}
