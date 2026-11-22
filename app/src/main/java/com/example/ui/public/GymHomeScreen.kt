package com.example.ui.public

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GymConstants
import com.example.ui.theme.GymAccent
import com.example.ui.theme.GymAmber
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymInk
import com.example.ui.theme.GymInkMuted
import com.example.ui.theme.GymInkSecondary
import com.example.ui.theme.GymLine
import com.example.ui.theme.GymPaper
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymSurface
import com.example.ui.theme.InterFontFamily
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class GymLiveStatus(
    val label: String,
    val dotColor: Color,
    val isOpen: Boolean
)

@Composable
fun GymHomeScreen(
    onOpenAdminPanel: () -> Unit
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000L)
        }
    }

    val liveTimeText = remember(now) {
        now.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }
    val statusSundayOff = stringResource(R.string.status_sunday_off)
    val statusOpenNow = stringResource(R.string.status_open_now)
    val statusOpens5Am = stringResource(R.string.status_opens_5am)
    val statusReopens5Pm = stringResource(R.string.status_reopens_5pm)
    val statusClosed = stringResource(R.string.status_closed)

    val liveStatus = remember(now, statusSundayOff, statusOpenNow, statusOpens5Am, statusReopens5Pm, statusClosed) {
        val h = now.hour
        val day = now.dayOfWeek
        val isMorning = h in 5 until 10
        val isEvening = h in 17 until 22
        when {
            day == DayOfWeek.SUNDAY -> GymLiveStatus(statusSundayOff, GymRed, false)
            isMorning || isEvening -> GymLiveStatus(statusOpenNow, GymGreen, true)
            h < 5 -> GymLiveStatus(statusOpens5Am, GymAmber, false)
            h in 10 until 17 -> GymLiveStatus(statusReopens5Pm, GymAmber, false)
            else -> GymLiveStatus(statusClosed, GymRed, false)
        }
    }

    Scaffold(
        containerColor = GymPaper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EditorialTopBar(
                onWhatsAppClick = { openWhatsAppJoin(context) }
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .navigationBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(36.dp))

                EditorialEyebrow(text = stringResource(R.string.hero_badge_fitness_first))

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "AVYUKT",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 62.sp,
                        lineHeight = 56.sp,
                        letterSpacing = (-2.4).sp,
                        color = GymInk
                    )
                )
                Text(
                    text = "GYM",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 62.sp,
                        lineHeight = 56.sp,
                        letterSpacing = (-2.4).sp,
                        color = GymAccent
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.hero_tagline),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = GymInkSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = GymLine)
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(liveStatus.dotColor.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(liveStatus.dotColor, CircleShape)
                            )
                        }
                        Text(
                            text = liveStatus.label,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = GymInk,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.testTag("gym_live_status")
                        )
                    }

                    Text(
                        text = "$liveTimeText IST",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = GymInkMuted,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.testTag("gym_live_clock")
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                EditorialStatsCard()

                Spacer(modifier = Modifier.height(48.dp))

                EditorialEyebrow(text = stringResource(R.string.section_gym_timing).uppercase())

                Spacer(modifier = Modifier.height(18.dp))

                EditorialTimingCard()

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = GymInk, fontWeight = FontWeight.SemiBold)) {
                            append("Mon Tue Wed Thu Fri Sat")
                        }
                        withStyle(SpanStyle(color = GymInkSecondary)) {
                            append("  ·  Sun Off")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(48.dp))

                EditorialEyebrow(text = stringResource(R.string.section_membership_fees).uppercase())

                Spacer(modifier = Modifier.height(18.dp))

                EditorialMembershipCard()

                Spacer(modifier = Modifier.height(48.dp))

                EditorialEyebrow(text = stringResource(R.string.section_join_now).uppercase())

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.whatsapp_join_title),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        color = GymInk
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.whatsapp_join_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(color = GymInkSecondary)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { openWhatsAppJoin(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("whatsapp_join_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymInk,
                        contentColor = GymPaper
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.whatsapp_btn_strong),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { openPhoneDialer(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("call_gym_button"),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, GymInk),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = GymInk
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Call: ${stringResource(R.string.owner_phone_display)}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(52.dp))
                HorizontalDivider(color = GymLine)
                Spacer(modifier = Modifier.height(22.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = GymInk, fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.gym_full_name))
                            }
                            withStyle(SpanStyle(color = GymInkMuted)) {
                                append(" · ${stringResource(R.string.footer_schedule)}")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "${stringResource(R.string.owner_admin_panel)} · Login",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = GymInkSecondary,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenAdminPanel)
                            .minimumInteractiveComponentSize()
                            .padding(vertical = 8.dp, horizontal = 2.dp)
                            .testTag("owner_admin_button")
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun EditorialTopBar(
    onWhatsAppClick: () -> Unit
) {
    Surface(
        color = GymPaper,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.gym_logo),
                        contentDescription = stringResource(R.string.gym_full_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                    )
                    Text(
                        text = "AVYUKT GYM",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                            color = GymInk
                        )
                    )
                }

                OutlinedButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, GymInk),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = GymInk
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "WhatsApp",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                }
            }
            HorizontalDivider(color = GymLine)
        }
    }
}

@Composable
private fun EditorialEyebrow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(1.dp)
                .background(GymInkSecondary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = GymInkSecondary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.2.sp,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun EditorialStatsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GymSurface,
        border = BorderStroke(1.dp, GymLine)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorialStatCell(
                modifier = Modifier.weight(1f),
                value = "₹${GymConstants.FEES_AMOUNT}",
                label = stringResource(R.string.stat_per_month)
            )
            VerticalDivider(
                modifier = Modifier.height(74.dp),
                color = GymLine
            )
            EditorialStatCell(
                modifier = Modifier.weight(1f),
                value = "6",
                label = stringResource(R.string.stat_days_week)
            )
            VerticalDivider(
                modifier = Modifier.height(74.dp),
                color = GymLine
            )
            EditorialStatCell(
                modifier = Modifier.weight(1f),
                value = "10h",
                label = stringResource(R.string.stat_daily_hrs)
            )
        }
    }
}

@Composable
private fun EditorialStatCell(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Column(
        modifier = modifier.padding(vertical = 22.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                letterSpacing = (-1).sp,
                color = GymInk
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = GymInkMuted,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun EditorialTimingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GymSurface,
        border = BorderStroke(1.dp, GymLine)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            EditorialTimingRow(
                label = stringResource(R.string.morning_label),
                start = stringResource(R.string.morning_start),
                end = stringResource(R.string.morning_end)
            )
            HorizontalDivider(color = GymLine)
            EditorialTimingRow(
                label = stringResource(R.string.evening_label),
                start = stringResource(R.string.evening_start),
                end = stringResource(R.string.evening_end)
            )
        }
    }
}

@Composable
private fun EditorialTimingRow(
    label: String,
    start: String,
    end: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = GymInkMuted,
                letterSpacing = 1.8.sp,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = GymInk, fontWeight = FontWeight.Bold)) {
                    append(start)
                }
                withStyle(
                    SpanStyle(
                        color = GymInkMuted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                ) {
                    append("  ${stringResource(R.string.to_label)}  ")
                }
                withStyle(SpanStyle(color = GymInk, fontWeight = FontWeight.Bold)) {
                    append(end)
                }
            },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 23.sp,
                letterSpacing = (-0.5).sp
            )
        )
    }
}

@Composable
private fun EditorialMembershipCard() {
    val perks = listOf(
        "Morning Batch",
        "Evening Batch",
        "Expert Trainer",
        "All Equipment",
        "Mon–Sat"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GymInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.monthly_membership),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = GymPaper,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Surface(
                    shape = CircleShape,
                    color = GymAccent
                ) {
                    Text(
                        text = stringResource(R.string.best_value_badge).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "₹${GymConstants.FEES_AMOUNT}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 58.sp,
                        lineHeight = 54.sp,
                        letterSpacing = (-2.2).sp,
                        color = GymPaper
                    )
                )
                Text(
                    text = "per month",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFFB5B3AB)
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No hidden charges",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFB5B3AB)
                )
            )

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = Color(0xFF2E2E2E))

            perks.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    pair.forEach { perk ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = perk,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = GymPaper,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(vertical = 14.dp)
                            )
                            HorizontalDivider(color = Color(0xFF282828))
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun openWhatsAppJoin(context: Context) {
    val message = "Namaste Sir, main Avyukt Gym join karna chahta hoon. Kripya details bataiye."
    val url = "https://wa.me/${GymConstants.OWNER_PHONE_RAW}?text=${Uri.encode(message)}"
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun openPhoneDialer(context: Context) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${GymConstants.OWNER_PHONE_DIAL}"))
    runCatching {
        context.startActivity(intent)
    }
}
