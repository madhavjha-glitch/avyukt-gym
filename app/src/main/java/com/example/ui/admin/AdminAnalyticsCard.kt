package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt

private enum class AnalyticsTab(val labelRes: Int) {
    REVENUE(R.string.analytics_tab_revenue),
    ATTENDANCE(R.string.analytics_tab_attendance),
    BATCHES(R.string.analytics_tab_batches)
}

private data class TradingPoint(
    val shortLabel: String,
    val detailLabel: String,
    val value: Float,
    val valueText: String
)

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
                    imageVector = Icons.Rounded.ShowChart,
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
                        text = "Attendance Trend Graph",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = GymInk,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text = "7 Days • ${AdminViewModel.formatDisplayDate(anchorDateIso)}",
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
                        imageVector = Icons.Rounded.ShowChart,
                        contentDescription = null,
                        tint = GymGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Fees Trend Graph",
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
    val monthSeries = remember(allFeeMonths, activeMemberIds, anchorMonthIso, currencyFormatter) {
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
            val short = ym.format(DateTimeFormatter.ofPattern("MMM"))
            val detail = "${ym.format(DateTimeFormatter.ofPattern("MMM yyyy"))} ($paidCount Paid)"
            TradingPoint(
                shortLabel = short,
                detailLabel = detail,
                value = amount.toFloat(),
                valueText = "₹${currencyFormatter.format(amount)}"
            )
        }
    }

    val total6Mo = monthSeries.sumOf { it.value.toInt() }
    val currentPaidAmount = monthSeries.lastOrNull()?.value?.toInt() ?: 0

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
            Text(
                text = "Latest: ₹${currencyFormatter.format(currentPaidAmount)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GymInkMuted,
                    letterSpacing = 0.sp
                )
            )
        }

        TradingLineChart(
            points = monthSeries,
            lineColor = GymGreen,
            axisScaleMaxHint = (members.size * GymConstants.FEES_AMOUNT).coerceAtLeast(GymConstants.FEES_AMOUNT).toFloat(),
            formatAxisValue = { v ->
                val iv = v.roundToInt()
                if (iv >= 1000) "₹${String.format(Locale.US, "%.1fk", iv / 1000f).replace(".0k", "k")}" else "₹$iv"
            },
            formatDelta = { diff ->
                val iv = diff.roundToInt()
                val sign = if (iv > 0) "+" else if (iv < 0) "-" else ""
                val absVal = kotlin.math.abs(iv)
                "${sign}₹${currencyFormatter.format(absVal)}"
            }
        )
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
            val short = date.format(DateTimeFormatter.ofPattern("dd"))
            val detail = date.format(DateTimeFormatter.ofPattern("dd MMM"))
            TradingPoint(
                shortLabel = short,
                detailLabel = detail,
                value = present.toFloat(),
                valueText = "$present Present"
            )
        }
    }

    val totalWeekPresent = daySeries.sumOf { it.value.toInt() }
    val avg = if (daySeries.isNotEmpty()) totalWeekPresent / daySeries.size else 0
    val peak = daySeries.maxOfOrNull { it.value.toInt() } ?: 0

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
                text = "Peak: $peak",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GymInkMuted,
                    letterSpacing = 0.sp
                )
            )
        }

        TradingLineChart(
            points = daySeries,
            lineColor = GymBlue,
            axisScaleMaxHint = members.size.coerceAtLeast(1).toFloat(),
            formatAxisValue = { v -> v.roundToInt().toString() },
            formatDelta = { diff ->
                val iv = diff.roundToInt()
                if (iv > 0) "+$iv" else "$iv"
            }
        )
    }
}

@Composable
private fun TradingLineChart(
    points: List<TradingPoint>,
    lineColor: Color,
    axisScaleMaxHint: Float,
    formatAxisValue: (Float) -> String,
    formatDelta: (Float) -> String
) {
    if (points.isEmpty()) {
        AnalyticsEmptyHint()
        return
    }

    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex) }
    val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
    val selected = points[safeIndex]
    val prev = if (safeIndex > 0) points[safeIndex - 1] else selected
    val diff = selected.value - prev.value
    val pct = if (safeIndex > 0 && prev.value > 0f) ((diff / prev.value) * 100f).roundToInt() else null

    val maxPointValue = points.maxOfOrNull { it.value } ?: 0f
    val maxScale = maxOf(axisScaleMaxHint, maxPointValue, 1f)
    val midScale = maxScale / 2f

    val deltaColor = when {
        diff > 0f -> GymGreen
        diff < 0f -> GymRed
        else -> GymInkMuted
    }
    val deltaArrow = when {
        diff > 0f -> "▲ "
        diff < 0f -> "▼ "
        else -> "• "
    }
    val deltaText = buildString {
        append(deltaArrow)
        append(formatDelta(diff))
        if (pct != null) {
            append(" (${if (pct > 0) "+" else ""}$pct%)")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Trading ticker readout bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = GymPaper,
            border = BorderStroke(1.dp, GymLine)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selected.detailLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GymInkMuted,
                            letterSpacing = 0.sp
                        )
                    )
                    Text(
                        text = selected.valueText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = GymInk,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = deltaColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = deltaText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = deltaColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        )
                    )
                }
            }
        }

        // Chart canvas + right-side Y-axis scale (TradingView style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val count = points.size
                            if (count > 0 && size.width > 0) {
                                val padX = 12.dp.toPx()
                                val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                                val idx = if (count == 1) 0 else {
                                    (((offset.x - padX) / usableW) * (count - 1)).roundToInt()
                                        .coerceIn(0, count - 1)
                                }
                                selectedIndex = idx
                            }
                        }
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures { change, _ ->
                            val count = points.size
                            if (count > 0 && size.width > 0) {
                                val padX = 12.dp.toPx()
                                val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                                val idx = if (count == 1) 0 else {
                                    (((change.position.x - padX) / usableW) * (count - 1)).roundToInt()
                                        .coerceIn(0, count - 1)
                                }
                                selectedIndex = idx
                            }
                        }
                    }
            ) {
                val padX = 12.dp.toPx()
                val topPad = 12.dp.toPx()
                val bottomPad = 12.dp.toPx()
                val chartW = (size.width - padX * 2f).coerceAtLeast(1f)
                val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                val bottomY = topPad + chartH

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                val gridColor = Color(0xFFDEDCD5)

                // Dashed horizontal trading grid lines
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val gy = bottomY - frac * chartH
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, gy),
                        end = Offset(size.width, gy),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                val offsets = points.mapIndexed { idx, pt ->
                    val x = if (points.size == 1) {
                        padX + chartW / 2f
                    } else {
                        padX + (idx.toFloat() / (points.size - 1)) * chartW
                    }
                    val ratio = (pt.value / maxScale).coerceIn(0f, 1f)
                    val y = bottomY - ratio * chartH
                    Offset(x, y)
                }

                if (offsets.size >= 2) {
                    val areaPath = Path().apply {
                        moveTo(offsets.first().x, bottomY)
                        offsets.forEach { lineTo(it.x, it.y) }
                        lineTo(offsets.last().x, bottomY)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.24f),
                                lineColor.copy(alpha = 0.02f)
                            ),
                            startY = topPad,
                            endY = bottomY
                        )
                    )

                    val linePath = Path().apply {
                        moveTo(offsets.first().x, offsets.first().y)
                        offsets.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(
                            width = 2.6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Crosshair dashed lines on active point
                val activeOffset = offsets[safeIndex]
                drawLine(
                    color = lineColor.copy(alpha = 0.45f),
                    start = Offset(activeOffset.x, topPad),
                    end = Offset(activeOffset.x, bottomY),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = lineColor.copy(alpha = 0.35f),
                    start = Offset(0f, activeOffset.y),
                    end = Offset(size.width, activeOffset.y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                // Vertex dots + active trading highlight ring
                offsets.forEachIndexed { idx, pt ->
                    if (idx == safeIndex) {
                        drawCircle(
                            color = lineColor.copy(alpha = 0.20f),
                            radius = 9.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = GymSurface,
                            radius = 2.5.dp.toPx(),
                            center = pt
                        )
                    } else {
                        drawCircle(
                            color = lineColor,
                            radius = 3.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = GymSurface,
                            radius = 1.4.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right Y-axis price/count scale
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatAxisValue(maxScale),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GymInkMuted,
                        fontSize = 10.sp,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = formatAxisValue(midScale),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GymInkMuted,
                        fontSize = 10.sp,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = formatAxisValue(0f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GymInkMuted,
                        fontSize = 10.sp,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 1
                )
            }
        }

        // Bottom X-axis labels matching canvas width
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { idx, pt ->
                val isSelected = idx == safeIndex
                Text(
                    text = pt.shortLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSelected) lineColor else GymInkMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                )
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
