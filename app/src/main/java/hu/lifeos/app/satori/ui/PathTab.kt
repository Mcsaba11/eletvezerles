package hu.lifeos.app.satori.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.lifeos.app.R
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CardHeader
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.screens.EnergyRow
import hu.lifeos.app.ui.screens.MoodRow
import hu.lifeos.app.ui.theme.StatusColors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PathTab(
    vm: SatoriViewModel, modifier: Modifier, onDayClose: () -> Unit, onBody: () -> Unit,
    resetPulse: Boolean, onPulseDone: () -> Unit,
) {
    val st by vm.state.collectAsState()
    val c = vm.content
    val th = c.theme(st.theme)
    val d = st.cur()
    val today = SatoriLogic.today()
    val idx = SatoriLogic.stageIndex(c, d.streak)
    val nIdx = SatoriLogic.nextStageIndex(c, d.streak)
    val frac = SatoriLogic.progressFraction(c, d.streak)
    val closed = d.dayOutcomeDate == today
    val restarter = stringResource(R.string.satori_restarter)
    val restarterSub = stringResource(R.string.satori_restarter_sub)
    val todayMood by vm.todayMood.collectAsState()

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LocalTime.now() } }

    if (resetPulse) LaunchedEffect(Unit) { delay(1800); onPulseDone() }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(170.dp)) {
                Image(painterResource(themePhoto(st.theme)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))))
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 8.dp)) {
                    val h = now.hour
                    Text(
                        stringResource(when (h) { in 5..9 -> R.string.greet_morning; in 10..17 -> R.string.greet_day; else -> R.string.greet_evening }) + ", " + th.addressTerm,
                        style = MaterialTheme.typography.headlineSmall, color = Color.White,
                    )
                    Text(Fmt.dayLong(LocalDate.now()), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }

        val risk = SatoriLogic.topRiskyDaypart(d)
        if (risk != null && !closed && risk.part == SatoriLogic.daypartOf(now.format(Fmt.time))) item {
            AppCard(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.satori_risky_now), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ---------- Enso és a mai nap gyűrűje ----------
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1.25f), contentAlignment = Alignment.Center) {
                    EnsoRing(frac, MaterialTheme.colorScheme.tertiary, Modifier.size(210.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.satori_current_stage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val animated by animateIntAsState(d.streak, tween(700), label = "streak")
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$animated", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold)
                            Text(" " + stringResource(R.string.satori_day_word), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
                        }
                        Text(SatoriLogic.stageSub(c, st, idx, restarterSub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(Modifier.weight(0.75f), contentAlignment = Alignment.Center) {
                    val dayFrac = (now.toSecondOfDay() / 86400f).coerceIn(0f, 1f)
                    val ringColor = if (closed) when (d.dayOutcome) {
                        "siker" -> StatusColors.ok
                        "sikertelen" -> StatusColors.bad
                        else -> StatusColors.warn
                    } else MaterialTheme.colorScheme.secondary
                    DayRing(dayFrac, ringColor, Modifier.size(118.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.satori_today_ring), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val left = 24 * 60 - (now.hour * 60 + now.minute)
                        Text(stringResource(R.string.satori_hours_left, left / 60, left % 60), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---------- Rang ----------
        item {
            AppCard(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Text(th.rankLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                VSpace(4)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(SatoriLogic.stageTitle(c, st, idx, restarter), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (nIdx != idx) Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("${d.streak} / ${c.stages[nIdx].days}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("„${SatoriLogic.stageMotto(c, st, idx)}”", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(10)
                val animFrac by animateFloatAsState(frac, tween(900), label = "frac")
                LinearProgressIndicator(
                    progress = { animFrac }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.tertiary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
                )
                VSpace(6)
                val remain = c.stages[nIdx].days - d.streak
                Text(
                    when {
                        nIdx == idx -> stringResource(R.string.satori_max_level)
                        remain in 1..3 -> stringResource(R.string.satori_almost, remain)
                        else -> stringResource(R.string.satori_remain_unknown, remain)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remain in 1..3) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VSpace(12)
                if (closed) {
                    OutlinedButton(onClick = onDayClose, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.satori_closed_label, SatoriLogic.outcomeLabel(c, st, d.dayOutcome ?: "siker")))
                    }
                } else {
                    Button(
                        onClick = onDayClose, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary),
                    ) { Text(stringResource(R.string.satori_close_day)) }
                }
            }
        }

        if (resetPulse) item {
            Text(
                stringResource(R.string.satori_reset_pulse), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
            )
        }

        // ---------- Napi szándék ----------
        item {
            val sealed = d.pledgeDate == today
            AppCard(Modifier.padding(horizontal = 16.dp)) {
                CardHeader(Icons.Outlined.LocalFlorist, stringResource(R.string.satori_pledge)) {
                    Text(stringResource(if (sealed) R.string.satori_sealed else R.string.satori_not_sealed), style = MaterialTheme.typography.labelMedium,
                        color = if (sealed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                VSpace(8)
                Text("„${th.pledgeText}”", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
                VSpace(8)
                OutlinedButton(onClick = { vm.togglePledge() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (sealed) R.string.satori_pledge_undo else R.string.satori_pledge_do))
                }
            }
        }

        // ---------- Hangulat ----------
        item {
            AppCard(Modifier.padding(horizontal = 16.dp)) {
                CardHeader(Icons.Outlined.Mood, stringResource(R.string.mood_title))
                VSpace(10)
                MoodRow(selected = todayMood?.mood, onSelect = { vm.setMood(it, null) }, tint = MaterialTheme.colorScheme.secondary)
                VSpace(8)
                EnergyRow(selected = todayMood?.energy, onSelect = { vm.setMood(null, it) }, tint = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.satori_mood_shared), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // ---------- Testi idővonal ----------
        item {
            val body = c.body(st.type)
            AppCard(Modifier.padding(horizontal = 16.dp), onClick = onBody) {
                CardHeader(Icons.Outlined.Timeline, body.cardTitle)
                VSpace(6)
                Text(SatoriLogic.bodyStage(c, st.type, d.streak).whenText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }
        }

        // ---------- Napi bölcselet + visszanyert idő ----------
        item {
            val qs = SatoriLogic.themeQuotes(c, st)
            AppCard(Modifier.padding(horizontal = 16.dp)) {
                CardHeader(Icons.Outlined.FormatQuote, stringResource(R.string.satori_daily_wisdom)) {
                    Text(th.quoteCaption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                VSpace(8)
                Text("„${qs[SatoriLogic.dayIndex(qs.size)]}”", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            }
        }
        item {
            AppCard(Modifier.padding(horizontal = 16.dp)) {
                CardHeader(Icons.Outlined.Schedule, stringResource(R.string.satori_hours_won)) {
                    Text(stringResource(R.string.satori_hours_value, d.streak * 24), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

/** Ecsetvonásszerű enso-kör: soha nem zárul be teljesen (max. 93%), a végén izzó ponttal. */
@Composable
fun EnsoRing(frac: Float, color: Color, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(frac) { anim.animateTo(frac, tween(1000)) }
    val glow = rememberInfiniteTransition(label = "glow").animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse), label = "g")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val dots = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val stroke = size.minDimension * 0.055f
        val r = size.minDimension / 2 - stroke * 1.6f
        val tl = Offset(center.x - r, center.y - r)
        val sz = Size(r * 2, r * 2)
        drawCircle(dots, radius = r + stroke * 1.3f, style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 12f)), cap = StrokeCap.Round))
        drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(stroke))
        val sweep = 360f * 0.93f * anim.value
        if (sweep > 0.5f) {
            drawArc(color.copy(alpha = glow.value * 0.35f), -90f, sweep, false, tl, sz, style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(color.copy(alpha = 0.75f), color, Color.White.copy(alpha = 0.9f), color.copy(alpha = 0.75f)), center),
                -90f, sweep, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round),
            )
            val a = Math.toRadians((sweep - 90f).toDouble())
            val p = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
            drawCircle(color.copy(alpha = glow.value), radius = stroke * 0.75f, center = p)
            drawCircle(Color(0xFFFFF8E0), radius = stroke * 0.35f, center = p)
        }
    }
}

@Composable
fun DayRing(frac: Float, color: Color, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier) {
        val stroke = size.minDimension * 0.08f
        val r = size.minDimension / 2 - stroke
        val tl = Offset(center.x - r, center.y - r)
        val sz = Size(r * 2, r * 2)
        drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(stroke))
        drawArc(color, -90f, 360f * 0.93f * frac, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
