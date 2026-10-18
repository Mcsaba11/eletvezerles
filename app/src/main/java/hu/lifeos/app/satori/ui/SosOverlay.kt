package hu.lifeos.app.satori.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hu.lifeos.app.R
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import kotlinx.coroutines.delay

private val glass = Color(0xCC14171A)

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(glass).padding(16.dp)) { content() }
}

@Composable
private fun FullScreen(onClose: () -> Unit, photo: Int, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0C0E10))) {
            Image(painterResource(photo), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.55f)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66000000), Color(0xEE0C0E10)))))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SosOverlay(vm: SatoriViewModel, onClose: () -> Unit, onBreath: () -> Unit) {
    val st by vm.state.collectAsState()
    val c = vm.content
    val d = st.cur()
    val ctx = LocalContext.current
    val startTs = rememberSaveable { System.currentTimeMillis() }
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { elapsed = (System.currentTimeMillis() - startTs) / 1000; delay(1000) } }
    val pct = SatoriLogic.urgeIntensity(elapsed)
    val phase = SatoriLogic.urgePhase(elapsed, pct)
    val quote = rememberSaveable { c.quotes.random() }
    val reason = rememberSaveable { d.reasons.randomOrNull() ?: "" }.ifBlank { null }
    var microTask by rememberSaveable { mutableStateOf<String?>(null) }
    var editPlan by rememberSaveable { mutableStateOf(d.ifThenPlan.isBlank()) }
    var plan by rememberSaveable { mutableStateOf(d.ifThenPlan) }
    var grounded by remember { mutableStateOf(setOf<Int>()) }
    var trigger by rememberSaveable { mutableStateOf<String?>(null) }
    val white = Color.White
    val soft = Color.White.copy(alpha = 0.75f)
    val accent = MaterialTheme.colorScheme.tertiary

    FullScreen(onClose, themeSosPhoto(st.theme)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sos_kicker).uppercase(), style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, stringResource(R.string.close), tint = white) }
            }
            Text(stringResource(R.string.sos_title), style = MaterialTheme.typography.headlineMedium, color = white)
            Text(stringResource(R.string.sos_intro), style = MaterialTheme.typography.bodyLarge, color = soft)
            Text("„$quote”", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = soft)

            if (reason != null) GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Favorite, null, tint = accent, modifier = Modifier.size(18.dp))
                    Text("  " + stringResource(R.string.sos_your_reason), style = MaterialTheme.typography.labelLarge, color = accent)
                }
                VSpace(6)
                Text("„$reason”", style = MaterialTheme.typography.titleMedium, color = white)
            }

            GlassCard(Modifier.clickable(onClick = onBreath)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Waves, null, tint = accent)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(stringResource(R.string.sos_breath), style = MaterialTheme.typography.titleMedium, color = white)
                        Text(stringResource(R.string.sos_breath_sub), style = MaterialTheme.typography.bodySmall, color = soft)
                    }
                }
            }

            // ---------- Sóvárgás-hullám ----------
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.sos_wave), style = MaterialTheme.typography.titleMedium, color = white, modifier = Modifier.weight(1f))
                    Text("${elapsed / 60}:${(elapsed % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
                }
                VSpace(8)
                UrgeWave(elapsed, pct, accent)
                Row {
                    Text(stringResource(R.string.sos_wave_start), style = MaterialTheme.typography.labelSmall, color = soft, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.sos_wave_peak), style = MaterialTheme.typography.labelSmall, color = soft, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.sos_wave_end), style = MaterialTheme.typography.labelSmall, color = soft)
                }
                VSpace(8)
                val phaseLabel = stringResource(listOf(R.string.sos_phase_rise, R.string.sos_phase_peak, R.string.sos_phase_calm, R.string.sos_phase_gone)[phase])
                Text("$phaseLabel · $pct%", style = MaterialTheme.typography.titleSmall, color = accent)
                Text(stringResource(R.string.sos_wave_help), style = MaterialTheme.typography.bodySmall, color = soft)
            }

            GlassCard {
                Text(c.defusionLines[SatoriLogic.dayIndex(c.defusionLines.size)], style = MaterialTheme.typography.bodyLarge, color = white, fontStyle = FontStyle.Italic)
            }

            // ---------- Ha… akkor… terv ----------
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Directions, null, tint = accent, modifier = Modifier.size(18.dp))
                    Text("  " + stringResource(R.string.sos_plan), style = MaterialTheme.typography.titleMedium, color = white, modifier = Modifier.weight(1f))
                    if (!editPlan) TextButton(onClick = { editPlan = true }) { Text(stringResource(R.string.edit)) }
                }
                if (editPlan) {
                    Text(stringResource(R.string.sos_plan_help), style = MaterialTheme.typography.bodySmall, color = soft)
                    VSpace(6)
                    OutlinedTextField(plan, { plan = it }, Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.sos_plan_placeholder)) }, minLines = 2)
                    TextButton(onClick = { vm.setIfThen(plan); editPlan = plan.isBlank() }) { Text(stringResource(R.string.save)) }
                } else {
                    Text(plan, style = MaterialTheme.typography.bodyLarge, color = white)
                }
            }

            // ---------- Gyors segítség ----------
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FlashOn, null, tint = accent, modifier = Modifier.size(18.dp))
                    Text("  " + stringResource(R.string.sos_quick), style = MaterialTheme.typography.titleMedium, color = white)
                }
                VSpace(8)
                c.typeTips[st.type].orEmpty().forEach { tip ->
                    Row(Modifier.padding(bottom = 8.dp)) {
                        Icon(iconFor(tip.icon), null, tint = accent, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Text("  " + tip.text, style = MaterialTheme.typography.bodyMedium, color = white)
                    }
                }
                VSpace(4)
                Text(stringResource(R.string.sos_distract), style = MaterialTheme.typography.labelLarge, color = accent)
                Text(microTask ?: stringResource(R.string.sos_distract_help), style = MaterialTheme.typography.bodyMedium, color = if (microTask != null) white else soft)
                FilledTonalButton(onClick = { microTask = c.microTasks.random() }, modifier = Modifier.padding(top = 6.dp)) { Text(stringResource(R.string.sos_give_task)) }
            }

            // ---------- 5-4-3-2-1 ----------
            GlassCard {
                Row {
                    Text(stringResource(R.string.sos_grounding), style = MaterialTheme.typography.titleMedium, color = white, modifier = Modifier.weight(1f))
                    Text("${grounded.size} / ${c.grounding.size}", style = MaterialTheme.typography.labelLarge, color = accent)
                }
                Text(stringResource(R.string.sos_grounding_help), style = MaterialTheme.typography.bodySmall, color = soft)
                VSpace(6)
                c.grounding.forEachIndexed { i, g ->
                    val done = i in grounded
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { grounded = if (done) grounded - i else grounded + i }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (done) accent else soft)
                        Text("  ${g.n}  ", style = MaterialTheme.typography.labelLarge, color = accent)
                        Text(g.text, style = MaterialTheme.typography.bodyMedium, color = if (done) soft else white,
                            textDecoration = if (done) TextDecoration.LineThrough else null)
                    }
                }
            }

            // ---------- Kiváltó ok ----------
            GlassCard {
                Text(stringResource(R.string.sos_trigger), style = MaterialTheme.typography.titleMedium, color = white)
                VSpace(6)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    c.cravingTriggers.forEach { t ->
                        FilterChip(trigger == t, {
                            if (trigger == null) vm.logCraving(t)
                            trigger = t
                        }, label = { Text(t) })
                    }
                }
            }

            if (st.ally.phone.isNotBlank()) {
                Button(
                    onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + st.ally.phone))) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                ) {
                    Icon(Icons.Outlined.Call, null); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.sos_call_ally, st.ally.name.ifBlank { st.ally.phone }))
                }
            } else {
                Text(stringResource(R.string.sos_no_ally), style = MaterialTheme.typography.bodySmall, color = soft)
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = MaterialTheme.colorScheme.onTertiary)) {
                Text(stringResource(R.string.sos_relief))
            }
            Text(stringResource(R.string.satori_mentor_disclaimer), style = MaterialTheme.typography.bodySmall, color = soft)
            VSpace(24)
        }
    }
}

@Composable
private fun UrgeWave(elapsed: Long, pct: Int, accent: Color) {
    val frac = (elapsed.toFloat() / SatoriLogic.URGE_WAVE_SECONDS).coerceIn(0f, 1f)
    Canvas(Modifier.fillMaxWidth().height(70.dp)) {
        val w = size.width; val h = size.height
        val base = h * 0.9f; val peak = h * 0.12f
        val path = Path()
        val n = 80
        for (i in 0..n) {
            val t = i / n.toFloat()
            val v = SatoriLogic.urgeIntensity((t * SatoriLogic.URGE_WAVE_SECONDS).toLong()) / 100f
            val x = w * t; val y = base - (base - peak) * v
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color.White.copy(alpha = 0.25f), style = Stroke(3f, cap = StrokeCap.Round))
        val pm = PathMeasure().apply { setPath(path, false) }
        val covered = Path()
        pm.getSegment(0f, pm.length * frac, covered, true)
        drawPath(covered, accent, style = Stroke(5f, cap = StrokeCap.Round))
        val x = w * frac; val y = base - (base - peak) * (pct / 100f)
        drawCircle(accent.copy(alpha = 0.35f), 14f, Offset(x, y))
        drawCircle(accent, 7f, Offset(x, y))
    }
}

// ---------------- Vezetett légzés ----------------

@Composable
fun BreathOverlay(vm: SatoriViewModel, onClose: () -> Unit) {
    val c = vm.content
    var tech by rememberSaveable { mutableStateOf("box") }
    var phaseIdx by remember { mutableIntStateOf(0) }
    var frac by remember { mutableStateOf(0f) }
    var remaining by remember { mutableIntStateOf(0) }
    val plan = c.techniques[tech].orEmpty()

    LaunchedEffect(tech) {
        phaseIdx = 0
        var start = -1L
        while (true) {
            withFrameNanos { now ->
                if (start < 0) start = now
                val ph = plan.getOrNull(phaseIdx)
                if (ph != null) {
                    val el = (now - start) / 1e9f
                    if (el >= ph.d) {
                        phaseIdx = (phaseIdx + 1) % plan.size
                        start = now
                        frac = 0f
                    } else {
                        frac = (el / ph.d).coerceIn(0f, 1f)
                        remaining = maxOf(1, kotlin.math.ceil(ph.d - el).toInt())
                    }
                }
                Unit
            }
        }
    }
    val ph = plan.getOrNull(phaseIdx)
    val scale = when (ph?.k) { "in" -> 1f + 0.12f * frac; "out" -> 1.12f - 0.12f * frac; "hold" -> 1.12f; else -> 1f }
    val accent = MaterialTheme.colorScheme.tertiary

    FullScreen(onClose, R.drawable.photo_sos_breathbg) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.breath_title), style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, stringResource(R.string.close), tint = Color.White) }
            }
            VSpace(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                c.techniqueDefs.forEach { t ->
                    val on = t.id == tech
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(if (on) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { tech = t.id }.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(t.title, color = if (on) Color.White else Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.titleSmall)
                        Text(t.sub, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(260.dp).scale(scale), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10f
                    val r = size.minDimension / 2 - stroke
                    drawCircle(Color.White.copy(alpha = 0.08f), r)
                    drawCircle(Color.White.copy(alpha = 0.18f), r, style = Stroke(stroke))
                    drawArc(accent, -90f, 360f * frac, false, Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(ph?.n.orEmpty(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.breath_sec, remaining), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium)
                }
            }
            VSpace(16)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.indices.forEach { i ->
                    Box(Modifier.size(if (i == phaseIdx) 10.dp else 7.dp).clip(CircleShape).background(if (i == phaseIdx) accent else Color.White.copy(alpha = 0.35f)))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.breath_help), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
            VSpace(16)
        }
    }
}

// ---------------- Testi idővonal ----------------

@Composable
fun BodyTimelineOverlay(vm: SatoriViewModel, onClose: () -> Unit) {
    val st by vm.state.collectAsState()
    val c = vm.content
    val cfg = c.body(st.type)
    val days = st.cur().streak
    val active = SatoriLogic.bodyStage(c, st.type, days)
    val accent = MaterialTheme.colorScheme.tertiary
    FullScreen(onClose, R.drawable.photo_splash) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconFor(cfg.icon), null, tint = accent)
                Text("  " + cfg.badge, color = accent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, stringResource(R.string.close), tint = Color.White) }
            }
            Text(cfg.heading, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            VSpace(8)
            Text(cfg.intro, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            cfg.safety?.let {
                VSpace(10)
                GlassCard { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            }
            VSpace(10)
            Text(stringResource(R.string.body_now, active.whenText, days), style = MaterialTheme.typography.titleSmall, color = accent)
            VSpace(14)
            cfg.stages.forEach { s ->
                val isActive = s == active
                val isPast = active.minDay > s.minDay
                Row(Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(when { isActive -> accent; isPast -> MaterialTheme.colorScheme.secondary; else -> Color.White.copy(alpha = 0.3f) }))
                    }
                    Column(
                        Modifier.weight(1f).padding(bottom = 18.dp).let {
                            if (isActive) it.clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.14f)).padding(12.dp) else it
                        },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(iconFor(s.icon), null, Modifier.size(16.dp), tint = if (isActive) accent else Color.White.copy(alpha = 0.7f))
                            Text(
                                "  " + s.whenText + if (isActive) " · " + stringResource(R.string.body_here) else "",
                                style = MaterialTheme.typography.labelLarge, color = if (isActive) accent else Color.White.copy(alpha = 0.7f),
                            )
                        }
                        VSpace(4)
                        Text(s.text, style = MaterialTheme.typography.bodyMedium, color = if (isActive) Color.White else Color.White.copy(alpha = 0.75f))
                    }
                }
            }
            Image(painterResource(R.drawable.photo_body_diagram), null, Modifier.fillMaxWidth().padding(top = 8.dp), contentScale = ContentScale.FillWidth)
            VSpace(24)
        }
    }
}

