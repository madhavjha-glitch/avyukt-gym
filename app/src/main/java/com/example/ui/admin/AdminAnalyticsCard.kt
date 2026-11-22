package com.example.ui.admin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FeeStatus
import com.example.data.model.GymBatch
import com.example.data.model.GymConstants
import com.example.data.model.GymMember
import com.example.ui.theme.GymAccent
import com.example.ui.theme.GymAmber
import com.example.ui.theme.GymBlue
import com.example.ui.theme.GymElevated
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymInk
import com.example.ui.theme.GymInkMuted
import com.example.ui.theme.GymInkSecondary
import com.example.ui.theme.GymLine
import com.example.ui.theme.GymPaper
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymSurface
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class AnalyticsTab(val labelRes: Int) {
    REVENUE(R.string.analytics_tab_revenue),
    ATTENDANCE(R.string.analytics_tab_attendance),
    BATCHES(R.string.analytics_tab_batches)
}

@Composable
fun AdminAnalyticsCard(
    members: List<GymMember>,
    allFeeMonths: Map<String, Map<String, FeeStatus>>,
    allAttendanceDays: Map<String, Map<String, Boolean>>
) {
    var selectedTab by remember { mutableStateOf(AnalyticsTab.REVENUE) }
    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dash_analytics_card"),
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
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint = GymInk,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.analytics_title),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = GymInk,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnalyticsTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    Surface(
                        onClick = { selectedTab = tab },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("analytics_tab_${tab.name.lowercase()}"),
                        shape = CircleShape,
                        color = if (selected) GymInk else GymPaper,
                        border = BorderStroke(1.dp, if (selected) GymInk else GymLine)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(tab.labelRes),
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
            }

            HorizontalDivider(color = GymLine)

            when (selectedTab) {
                AnalyticsTab.REVENUE -> RevenueSixMonthChart(
                    members = members,
                    allFeeMonths = allFeeMonths,
                    currencyFormatter = currencyFormatter
                )
                AnalyticsTab.ATTENDANCE -> AttendanceSevenDayChart(
                    members = members,
                    allAttendanceDays = allAttendanceDays
                )
                AnalyticsTab.BATCHES -> BatchStrengthChart(members = members)
            }
        }
    }
}

@Composable
fun AttendanceTrendChartCard(
    members: List<GymMember>,
    allAttendanceDays: Map<String, Map<String, Boolean>>,
    anchorDateIso: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("att_trend_chart_card"),
        shape = RoundedCornerShape(20.dp),
        color = GymSurface,
        border = BorderStroke(1.dp, GymLine)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        imageVector = Icons.Rounded.ShowChart,
                        contentDescription = null,
                        tint = GymBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Attendance Graph (7 Days)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = GymInk,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text = "Upto ${AdminViewModel.formatDisplayDate(anchorDateIso)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GymInkMuted,
                        letterSpacing = 0.sp
                    )
                )
            }
            HorizontalDivider(color = GymLine)
            AttendanceSevenDayChart(
                members = members,
                allAttendanceDays = allAttendanceDays,
                anchorDateIso = anchorDateIso
            )
        }
    }
}

@Composable
fun FeesRevenueChartCard(
    members: List<GymMember>,
    allFeeMonths: Map<String, Map<String, FeeStatus>>,
    selectedMonth: String
) {
    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
    }
    val monthFees = allFeeMonths[selectedMonth].orEmpty()
    val paidCount = members.count { monthFees[it.id]?.paid == true }
    val pendingCount = (members.size - paidCount).coerceAtLeast(0)
    val paidRatio = if (members.isNotEmpty()) paidCount.toFloat() / members.size.toFloat() else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fees_revenue_chart_card"),
        shape = RoundedCornerShape(20.dp),
        color = GymSurface,
        border = BorderStroke(1.dp, GymLine)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        imageVector = Icons.Rounded.BarChart,
                        contentDescription = null,
                        tint = GymGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Fees Collection Graph (6 Months)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = GymInk,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text = "${(paidRatio * 100).toInt()}% Paid",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (paidRatio >= 0.7f) GymGreen else GymAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                )
            }

            if (members.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        drawRoundRect(
                            color = GymRed.copy(alpha = 0.16f),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(h / 2f, h / 2f)
                        )
                        val paidW = (w * paidRatio).coerceIn(0f, w)
                        if (paidW > 0f) {
                            drawRoundRect(
                                color = GymGreen,
                                size = Size(paidW, h),
                                cornerRadius = CornerRadius(h / 2f, h / 2f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${AdminViewModel.formatMonthLabel(selectedMonth)}: $paidCount Paid",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GymGreen,
                                letterSpacing = 0.sp
                            )
                        )
                        Text(
                            text = "$pendingCount Pending",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GymRed,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = GymLine)

            RevenueSixMonthChart(
                members = members,
                allFeeMonths = allFeeMonths,
                currencyFormatter = currencyFormatter,
                anchorMonthIso = selectedMonth
            )
        }
    }
}

@Composable
internal fun RevenueSixMonthChart(
    members: List<GymMember>,
    allFeeMonths: Map<String, Map<String, FeeStatus>>,
    currencyFormatter: NumberFormat,
    anchorMonthIso: String? = null
) {
    val activeMemberIds = remember(members) { members.map { it.id }.toSet() }
    val monthSeries = remember(allFeeMonths, activeMemberIds, anchorMonthIso) {
        val anchorYm = anchorMonthIso?.let {
            runCatching { YearMonth.parse(it) }.getOrNull()
        } ?: YearMonth.now()
        (5L downTo 0L).map { offset ->
            val ym = anchorYm.minusMonths(offset)
            val key = ym.toString()
            val paidCount = allFeeMonths[key]
                ?.entries
                ?.count { (id, status) -> status.paid && (activeMemberIds.isEmpty() || id in activeMemberIds) }
                ?: 0
            val amount = paidCount * GymConstants.FEES_AMOUNT
            Triple(ym.format(DateTimeFormatter.ofPattern("MMM")), amount, paidCount)
        }
    }

    val total6Mo = monthSeries.sumOf { it.second }
    val maxAmount = (monthSeries.maxOfOrNull { it.second } ?: 0).coerceAtLeast(GymConstants.FEES_AMOUNT)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "6 Months Collection",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GymInkMuted,
                        letterSpacing = 0.sp
                    )
                )
                Text(
                    text = "₹${currencyFormatter.format(total6Mo)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = GymGreen,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
            val currentPaid = monthSeries.lastOrNull()?.third ?: 0
            Text(
                text = "Selected: $currentPaid/${members.size} Paid",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GymInkMuted,
                    letterSpacing = 0.sp
                )
            )
        }

        if (total6Mo == 0) {
            AnalyticsEmptyHint()
        } else {
            val animFraction by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f),
                label = "revenue_bars"
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    monthSeries.forEach { (_, amount, _) ->
                        Text(
                            text = if (amount > 0) {
                                if (amount >= 1000) "₹${String.format(Locale.US, "%.1fk", amount / 1000f)}" else "₹$amount"
                            } else "₹0",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (amount > 0) GymGreen else GymInkMuted,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                ) {
                    val slotWidth = size.width / monthSeries.size
                    val barWidth = (slotWidth * 0.44f).coerceAtMost(32.dp.toPx())
                    val gridColor = Color(0xFFEAE8E0)

                    for (i in 0..2) {
                        val y = size.height * (i / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    monthSeries.forEachIndexed { index, (_, amount, _) ->
                        val ratio = (amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f) * animFraction
                        val barHeight = (size.height * ratio).coerceAtLeast(4.dp.toPx())
                        val left = index * slotWidth + (slotWidth - barWidth) / 2f
                        val top = size.height - barHeight
                        drawRoundRect(
                            color = if (amount > 0) GymGreen else GymElevated,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    monthSeries.forEach { (label, _, _) ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GymInkMuted,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AttendanceSevenDayChart(
    members: List<GymMember>,
    allAttendanceDays: Map<String, Map<String, Boolean>>,
    anchorDateIso: String? = null
) {
    val activeIds = remember(members) { members.map { it.id }.toSet() }
    val daySeries = remember(allAttendanceDays, activeIds, anchorDateIso) {
        val anchorDate = anchorDateIso?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: LocalDate.now()
        (6L downTo 0L).map { offset ->
            val date = anchorDate.minusDays(offset)
            val key = date.toString()
            val present = allAttendanceDays[key]
                ?.entries
                ?.count { (id, isPresent) -> isPresent && (activeIds.isEmpty() || id in activeIds) }
                ?: 0
            date.format(DateTimeFormatter.ofPattern("dd")) to present
        }
    }

    val totalWeekPresent = daySeries.sumOf { it.second }
    val avg = if (daySeries.isNotEmpty()) totalWeekPresent / daySeries.size else 0
    val maxCount = (daySeries.maxOfOrNull { it.second } ?: 1).coerceAtLeast(members.size.coerceAtLeast(1))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "7-Day Daily Avg",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GymInkMuted,
                        letterSpacing = 0.sp
                    )
                )
                Text(
                    text = "$avg members/day",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = GymBlue,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
            Text(
                text = "Peak: ${daySeries.maxOfOrNull { it.second } ?: 0}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GymInkMuted,
                    letterSpacing = 0.sp
                )
            )
        }

        if (totalWeekPresent == 0) {
            AnalyticsEmptyHint()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daySeries.forEach { (_, count) ->
                        Text(
                            text = count.toString(),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (count > 0) GymBlue else GymInkMuted,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp)
                ) {
                    val slotWidth = size.width / daySeries.size
                    val points = daySeries.mapIndexed { idx, (_, count) ->
                        val x = idx * slotWidth + slotWidth / 2f
                        val ratio = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                        val y = size.height - 10.dp.toPx() - (ratio * (size.height - 20.dp.toPx()))
                        Offset(x, y)
                    }

                    if (points.size >= 2) {
                        val fillPath = Path().apply {
                            moveTo(points.first().x, size.height)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, size.height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(GymBlue.copy(alpha = 0.16f), Color.Transparent)
                            )
                        )

                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = strokePath,
                            color = GymBlue,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    points.forEach { pt ->
                        drawCircle(color = GymBlue, radius = 4.dp.toPx(), center = pt)
                        drawCircle(color = GymSurface, radius = 2.dp.toPx(), center = pt)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daySeries.forEach { (dayLabel, _) ->
                        Text(
                            text = dayLabel,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GymInkMuted,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchStrengthChart(members: List<GymMember>) {
    val total = members.size
    val morningCount = members.count { GymBatch.fromId(it.batch) == GymBatch.MORNING }
    val eveningCount = members.count { GymBatch.fromId(it.batch) == GymBatch.EVENING }
    val bothCount = members.count { GymBatch.fromId(it.batch) == GymBatch.BOTH }

    if (total == 0) {
        AnalyticsEmptyHint()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Total Members: $total",
            style = MaterialTheme.typography.titleMedium.copy(
                color = GymInk,
                fontWeight = FontWeight.Bold
            )
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            val w = size.width
            val h = size.height
            val mW = (morningCount.toFloat() / total) * w
            val eW = (eveningCount.toFloat() / total) * w
            val bW = (bothCount.toFloat() / total) * w

            drawRoundRect(
                color = GymElevated,
                size = Size(w, h),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )
            if (mW > 0f) {
                drawRect(color = GymAccent, topLeft = Offset(0f, 0f), size = Size(mW, h))
            }
            if (eW > 0f) {
                drawRect(color = GymBlue, topLeft = Offset(mW, 0f), size = Size(eW, h))
            }
            if (bW > 0f) {
                drawRect(color = GymAmber, topLeft = Offset(mW + eW, 0f), size = Size(bW, h))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BatchLegendChip(
                modifier = Modifier.weight(1f),
                label = "Morning",
                count = morningCount,
                total = total,
                color = GymAccent
            )
            BatchLegendChip(
                modifier = Modifier.weight(1f),
                label = "Evening",
                count = eveningCount,
                total = total,
                color = GymBlue
            )
            BatchLegendChip(
                modifier = Modifier.weight(1f),
                label = "Both",
                count = bothCount,
                total = total,
                color = GymAmber
            )
        }
    }
}

@Composable
private fun BatchLegendChip(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val pct = if (total > 0) (count * 100) / total else 0
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = GymPaper,
        border = BorderStroke(1.dp, GymLine)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = color,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                text = "$label ($pct%)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GymInkMuted,
                    letterSpacing = 0.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnalyticsEmptyHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = null,
                tint = GymInkMuted.copy(alpha = 0.45f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.analytics_no_data),
                style = MaterialTheme.typography.bodySmall.copy(color = GymInkMuted)
            )
        }
    }
}
