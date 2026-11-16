package com.example.ui.public

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GymConstants
import com.example.ui.theme.GymAmber
import com.example.ui.theme.GymCardBg
import com.example.ui.theme.GymGold
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymGreenDark
import com.example.ui.theme.GymObsidian
import com.example.ui.theme.GymOrange
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymTextMuted
import com.example.ui.theme.GymTextPrimary
import com.example.ui.theme.RajdhaniFontFamily
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class GymLiveStatus(
    val label: String,
    val accentColor: Color,
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
        now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }
    val statusChecking = stringResource(R.string.status_checking)
    val statusSundayOff = stringResource(R.string.status_sunday_off)
    val statusOpenNow = stringResource(R.string.status_open_now)
    val statusOpens5Am = stringResource(R.string.status_opens_5am)
    val statusReopens5Pm = stringResource(R.string.status_reopens_5pm)
    val statusClosed = stringResource(R.string.status_closed)

    val liveStatus = remember(now, statusChecking, statusSundayOff, statusOpenNow, statusOpens5Am, statusReopens5Pm, statusClosed) {
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
        containerColor = GymObsidian,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeroSection(
                liveStatus = liveStatus,
                liveTimeText = liveTimeText
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                SectionHeader(
                    icon = Icons.Rounded.AccessTime,
                    title = stringResource(R.string.section_gym_timing)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimingCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.WbSunny,
                        iconTint = GymAmber,
                        sessionLabel = stringResource(R.string.morning_label),
                        startTime = stringResource(R.string.morning_start),
                        endTime = stringResource(R.string.morning_end)
                    )
                    TimingCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.NightsStay,
                        iconTint = GymOrange,
                        sessionLabel = stringResource(R.string.evening_label),
                        startTime = stringResource(R.string.evening_start),
                        endTime = stringResource(R.string.evening_end)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                WeeklyDaysCard()

                Spacer(modifier = Modifier.height(32.dp))

                SectionHeader(
                    icon = Icons.Rounded.CurrencyRupee,
                    title = stringResource(R.string.section_membership_fees)
                )

                Spacer(modifier = Modifier.height(14.dp))

                MembershipFeesCard()

                Spacer(modifier = Modifier.height(32.dp))

                SectionHeader(
                    icon = Icons.AutoMirrored.Rounded.Chat,
                    title = stringResource(R.string.section_join_now)
                )

                Spacer(modifier = Modifier.height(14.dp))

                WhatsAppJoinCard(
                    onWhatsAppClick = { openWhatsAppJoin(context) },
                    onCallClick = { openPhoneDialer(context) }
                )

                Spacer(modifier = Modifier.height(28.dp))

                FooterSection(onOpenAdminPanel = onOpenAdminPanel)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun HeroSection(
    liveStatus: GymLiveStatus,
    liveTimeText: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_effects")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_glow"
    )
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    val particleSeeds = remember {
        listOf(
            0.12f to 0.2f, 0.25f to 0.7f, 0.38f to 0.4f, 0.52f to 0.85f,
            0.66f to 0.3f, 0.78f to 0.65f, 0.89f to 0.15f, 0.45f to 0.55f,
            0.18f to 0.9f, 0.73f to 0.45f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GymObsidian),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.gym_hero_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.32f,
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GymObsidian.copy(alpha = 0.45f),
                            GymOrange.copy(alpha = 0.08f),
                            GymObsidian
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.matchParentSize()) {
            particleSeeds.forEachIndexed { index, (xFrac, phase) ->
                val yFrac = ((1f - particleProgress + phase) % 1f)
                val radius = if (index % 2 == 0) 3.5f else 2.2f
                drawCircle(
                    color = GymOrange.copy(alpha = 0.45f * (1f - yFrac).coerceIn(0.15f, 0.7f)),
                    radius = radius,
                    center = Offset(x = size.width * xFrac, y = size.height * yFrac)
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GymOrange.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.gym_logo),
                    contentDescription = stringResource(R.string.gym_full_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(3.dp, GymOrange, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(listOf(GymOrange, GymRed)),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.hero_badge_fitness_first),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "AVYUKT",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = GymTextPrimary,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "GYM",
                style = TextStyle(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    letterSpacing = 3.sp,
                    brush = Brush.linearGradient(listOf(GymOrange, GymAmber))
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.hero_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = liveStatus.accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, liveStatus.accentColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(liveStatus.accentColor)
                    )
                    Text(
                        text = liveStatus.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = liveStatus.accentColor,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.testTag("gym_live_status")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = liveTimeText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    letterSpacing = 3.sp,
                    color = GymTextPrimary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("gym_live_clock")
            )

            Spacer(modifier = Modifier.height(26.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = GymCardBg.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .border(1.dp, GymOrange.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroStatItem(
                    modifier = Modifier.weight(1f),
                    value = "₹${GymConstants.FEES_AMOUNT}",
                    label = stringResource(R.string.stat_per_month)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
                HeroStatItem(
                    modifier = Modifier.weight(1f),
                    value = "6",
                    label = stringResource(R.string.stat_days_week)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
                HeroStatItem(
                    modifier = Modifier.weight(1f),
                    value = "10h",
                    label = stringResource(R.string.stat_daily_hrs)
                )
            }
        }
    }
}

@Composable
private fun HeroStatItem(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                color = GymOrange,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(color = GymTextMuted)
        )
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = Brush.linearGradient(listOf(GymOrange, GymRed)),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = GymTextPrimary,
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
private fun TimingCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    sessionLabel: String,
    startTime: String,
    endTime: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = GymCardBg,
        border = BorderStroke(1.dp, GymOrange.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = sessionLabel,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = sessionLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GymOrange,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = startTime,
                style = MaterialTheme.typography.titleLarge.copy(color = GymTextPrimary)
            )
            Text(
                text = stringResource(R.string.to_label),
                style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted, fontSize = 11.sp),
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text(
                text = endTime,
                style = MaterialTheme.typography.titleLarge.copy(color = GymTextPrimary)
            )
        }
    }
}

@Composable
private fun WeeklyDaysCard() {
    val openDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GymCardBg,
        border = BorderStroke(1.dp, GymOrange.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            openDays.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(GymOrange, Color(0xFFFF4500))),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SUN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GymTextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = stringResource(R.string.sunday_off_tag),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GymRed,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MembershipFeesCard() {
    val perks = listOf(
        "Morning Batch",
        "Evening Batch",
        "Expert Trainer",
        "All Equipment",
        "Mon–Sat"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GymCardBg,
        border = BorderStroke(1.dp, GymOrange.copy(alpha = 0.32f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(GymOrange.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 26.dp)
        ) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = RoundedCornerShape(50),
                color = GymGold
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = stringResource(R.string.best_value_badge).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.monthly_membership).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = GymTextMuted,
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = GymOrange,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "${GymConstants.FEES_AMOUNT}",
                        style = TextStyle(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 68.sp,
                            brush = Brush.linearGradient(listOf(GymOrange, GymAmber))
                        )
                    )
                }
                Text(
                    text = stringResource(R.string.fees_period),
                    style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted)
                )
                Spacer(modifier = Modifier.height(20.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    perks.forEach { perk ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = GymOrange.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, GymOrange.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = GymGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = perk,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = GymTextPrimary,
                                        fontWeight = FontWeight.Medium
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

@Composable
private fun WhatsAppJoinCard(
    onWhatsAppClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GymGreen.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, GymGreen.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.whatsapp_join_title).uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(color = GymTextPrimary),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.whatsapp_join_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onWhatsAppClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("whatsapp_join_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GymGreen,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.whatsapp_btn_small),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                    Text(
                        text = stringResource(R.string.whatsapp_btn_strong),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onCallClick)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("call_gym_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = null,
                    tint = GymGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.call_whatsapp_label),
                    style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted)
                )
                Text(
                    text = stringResource(R.string.owner_phone_display),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = GymGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun FooterSection(
    onOpenAdminPanel: () -> Unit
) {
    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.gym_full_name),
        style = MaterialTheme.typography.titleMedium.copy(
            color = GymOrange,
            fontWeight = FontWeight.Bold
        )
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.footer_schedule),
        style = MaterialTheme.typography.labelSmall.copy(color = GymTextMuted),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(18.dp))
    OutlinedButton(
        onClick = onOpenAdminPanel,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("owner_admin_button"),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, GymOrange.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GymOrange.copy(alpha = 0.12f),
            contentColor = GymOrange
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.AdminPanelSettings,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.owner_admin_panel),
            style = MaterialTheme.typography.labelLarge
        )
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
