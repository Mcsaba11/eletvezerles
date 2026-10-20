package hu.lifeos.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.lifeos.app.R
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.insight.Insights
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CardHeader
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.KpiTile
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.Meter
import hu.lifeos.app.ui.Pill
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.SheetHandle
import hu.lifeos.app.ui.TonalButton
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

/** Az életkerék dimenziói (a tárolt kulcs a megjelenített név). */
val WHEEL_DIMS = listOf(R.string.wd_time, R.string.wd_body, R.string.wd_soul, R.string.wd_money, R.string.wd_learn, R.string.wd_work, R.string.wd_social, R.string.wd_home, R.string.wd_fun)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternsScreen(
    vm: MainViewModel, satori: SatoriViewModel, food: FoodViewModel, contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
) {
    val moods by vm.moodHistory.collectAsState()
    val entries by vm.recentEntries.collectAsState()
    val wheel by vm.lifeWheel.collectAsState()
    val st by satori.state.collectAsState()
    val fs by food.state.collectAsState()
    val c = satori.content
    var rate by remember { mutableStateOf(false) }
    val ser = Insights.series(14, moods, fs, food.logic, st, entries)
    val patterns = Insights.patterns(Insights.series(60, moods, fs, food.logic, st, entries), st, c)
    val dimNames = WHEEL_DIMS.map { stringResource(it) }
    val mk = LocalDate.now().toString().take(7)
    val cur = wheel[mk]

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Noc.card).border(1.dp, Noc.stroke, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lock, null, Modifier.size(14.dp), tint = Noc.cyan)
                Text("  " + stringResource(R.string.offline_engine), style = MaterialTheme.typography.labelMedium, color = Noc.cyan)
            }
            VSpace(10)
            Text(stringResource(R.string.patterns_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.patterns_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
        }

        // ---------- Észrevett mintázatok ----------
        item {
            AppCard(glow = Noc.amber) {
                CardHeader(Icons.Outlined.Lightbulb, stringResource(R.string.patterns_found), tint = Noc.amber) {
                    if (patterns.isNotEmpty()) Pill(stringResource(R.string.patterns_n, patterns.size), textColor = Noc.cyan)
                }
                VSpace(10)
                if (patterns.isEmpty()) Text(stringResource(R.string.patterns_none), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
                patterns.take(4).forEachIndexed { i, p ->
                    if (i > 0) VSpace(10)
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Noc.well).border(1.dp, Noc.stroke, RoundedCornerShape(16.dp)).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.title, style = MaterialTheme.typography.labelLarge, color = Noc.text2, modifier = Modifier.weight(1f))
                            Text(p.value, style = MaterialTheme.typography.titleLarge, color = Noc.amber)
                        }
                        VSpace(4)
                        Text(p.text, style = MaterialTheme.typography.bodyMedium, color = Noc.text)
                    }
                }
            }
        }

        // ---------- Életkerék ----------
        item {
            AppCard {
                CardHeader(Icons.Outlined.Tune, stringResource(R.string.wheel_title), tint = Noc.cyan) {
                    if (cur != null) Pill(stringResource(R.string.wheel_avg, String.format(Fmt.hu, "%.1f", cur.values.average())))
                }
                VSpace(6)
                Text(stringResource(R.string.wheel_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                VSpace(8)
                Radar(dimNames, cur, Modifier.fillMaxWidth().aspectRatio(1.1f))
                if (cur != null) {
                    val best = cur.maxByOrNull { it.value }; val weak = cur.minByOrNull { it.value }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiTile(stringResource(R.string.wheel_strongest), best?.key.orEmpty(), "${best?.value ?: 0}/10", Modifier.weight(1f), accent = Noc.good)
                        KpiTile(stringResource(R.string.wheel_focus), weak?.key.orEmpty(), "${weak?.value ?: 0}/10", Modifier.weight(1f), accent = Noc.amber)
                    }
                    VSpace(8)
                }
                PrimaryButton(stringResource(if (cur == null) R.string.wheel_rate else R.string.wheel_rerate), onClick = { rate = true }, modifier = Modifier.fillMaxWidth())
            }
        }

        // ---------- Görbék ----------
        item {
            AppCard {
                CardHeader(Icons.Outlined.ShowChart, stringResource(R.string.curves_title), tint = Noc.sapphire)
                Text(stringResource(R.string.curves_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                VSpace(10)
                val gls = ser.mapNotNull { it.gl }
                val maxGl = (gls.maxOrNull() ?: 100.0).coerceAtLeast(100.0)
                val maxCr = (ser.maxOfOrNull { it.cravings } ?: 1).coerceAtLeast(1).toDouble()
                LineChart(
                    listOf(
                        ser.map { it.mood?.let { m -> (m - 1) / 4.0 } } to Noc.amber,
                        ser.map { it.gl?.let { g -> g / maxGl } } to Noc.coral,
                        ser.map { it.cravings / maxCr } to Noc.cyan,
                    ),
                    Modifier.fillMaxWidth().height(170.dp),
                )
                VSpace(8)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(Noc.amber, stringResource(R.string.type_mood)); Legend(Noc.coral, stringResource(R.string.food_gl)); Legend(Noc.cyan, stringResource(R.string.legend_cravings))
                }
            }
        }

        // ---------- Megtakarítás és Satori ----------
        item {
            val d = st.cur()
            val saved = d.dailyCost * d.streak
            val days = ser.count { s -> s.cravings == 0 }
            AppCard {
                CardHeader(Icons.Outlined.AccountBalanceWallet, stringResource(R.string.synergy_title), tint = Noc.good)
                VSpace(10)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiTile(stringResource(R.string.synergy_saved), Fmt.money(saved), stringResource(R.string.synergy_saved_sub), Modifier.weight(1f), accent = Noc.good)
                    KpiTile(stringResource(R.string.synergy_clean), "${d.streak} " + stringResource(R.string.satori_day_word), stringResource(R.string.synergy_calm, days), Modifier.weight(1f), accent = Noc.amber, icon = Icons.Outlined.Spa)
                }
                VSpace(10)
                val idx = SatoriLogic.stageIndex(c, d.streak)
                Text(stringResource(R.string.synergy_level, SatoriLogic.stageTitle(c, st, idx, stringResource(R.string.satori_restarter))), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                VSpace(6)
                Meter(SatoriLogic.progressFraction(c, d.streak), Noc.amber)
            }
        }

        // ---------- Adatbiztonság ----------
        item {
            AppCard {
                CardHeader(Icons.Outlined.Lock, stringResource(R.string.privacy_title), tint = Noc.cyan) { Pill(stringResource(R.string.privacy_pill), textColor = Noc.good) }
                VSpace(8)
                Text(stringResource(R.string.privacy_text), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
                VSpace(10)
                TonalButton(stringResource(R.string.backup_export), onClick = onOpenSettings)
            }
        }
    }

    if (rate) {
        val values = remember { mutableStateOf(dimNames.associateWith { cur?.get(it) ?: 6 }) }
        ModalBottomSheet(onDismissRequest = { rate = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Noc.cardElevated, dragHandle = { SheetHandle() }) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()) {
                Text(stringResource(R.string.wheel_rate_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.wheel_rate_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                VSpace(12)
                dimNames.forEach { n ->
                    val v = values.value[n] ?: 6
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(n, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text("$v", style = MaterialTheme.typography.titleMedium, color = Noc.cyan)
                    }
                    Slider(
                        value = v.toFloat(), onValueChange = { values.value = values.value + (n to it.toInt()) }, valueRange = 1f..10f, steps = 8,
                        colors = SliderDefaults.colors(thumbColor = Noc.cyan, activeTrackColor = Noc.cyan, inactiveTrackColor = Noc.well),
                    )
                }
                VSpace(8)
                PrimaryButton(stringResource(R.string.save), onClick = { vm.rateWheel(mk, values.value); rate = false }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Legend(c: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Noc.text2)
    }
}

/** Több sorozat 0..1 közé normálva; a hiányzó napok kimaradnak. */
@Composable
private fun LineChart(series: List<Pair<List<Double?>, Color>>, modifier: Modifier) {
    val grid = Noc.stroke
    Canvas(modifier) {
        for (k in 0..3) { val y = size.height * k / 3f; drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))) }
        series.forEach { (vals, col) ->
            val n = vals.size
            if (n < 2) return@forEach
            val path = Path(); var started = false
            vals.forEachIndexed { i, v ->
                if (v == null) return@forEachIndexed
                val x = size.width * i / (n - 1); val y = size.height * (1f - v.toFloat().coerceIn(0f, 1f))
                if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
            }
            if (started) {
                drawPath(path, col.copy(alpha = 0.25f), style = Stroke(9f, cap = StrokeCap.Round))
                drawPath(path, col, style = Stroke(4f, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun Radar(names: List<String>, values: Map<String, Int>?, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    val grid = Noc.stroke; val cyan = Noc.cyan; val label = Noc.text2
    Canvas(modifier) {
        val n = names.size
        val cx = size.width / 2; val cy = size.height / 2
        val r = minOf(size.width, size.height) / 2 * 0.68f
        fun pt(i: Int, f: Float): Offset {
            val a = -Math.PI / 2 + 2 * Math.PI * i / n
            return Offset(cx + (r * f * cos(a)).toFloat(), cy + (r * f * sin(a)).toFloat())
        }
        for (ring in 1..4) {
            val p = Path()
            (0 until n).forEach { i -> val q = pt(i, ring / 4f); if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }
            p.close(); drawPath(p, grid, style = Stroke(1.2f))
        }
        (0 until n).forEach { i -> drawLine(grid, Offset(cx, cy), pt(i, 1f), 1f) }
        if (values != null) {
            val p = Path()
            names.forEachIndexed { i, nm -> val q = pt(i, (values[nm] ?: 0) / 10f); if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }
            p.close()
            drawPath(p, Brush.radialGradient(listOf(cyan.copy(alpha = 0.35f), cyan.copy(alpha = 0.08f)), Offset(cx, cy), r))
            drawPath(p, cyan, style = Stroke(3f))
            names.forEachIndexed { i, nm -> drawCircle(cyan, 5f, pt(i, (values[nm] ?: 0) / 10f)) }
        }
        names.forEachIndexed { i, nm ->
            val q = pt(i, 1.2f)
            val txt = if (values != null) "$nm ${values[nm] ?: ""}" else nm
            drawLabel(tm, txt, q, label)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(tm: TextMeasurer, text: String, at: Offset, color: Color) {
    val res = tm.measure(text, TextStyle(fontSize = 11.sp, color = color))
    drawText(res, topLeft = Offset(at.x - res.size.width / 2f, at.y - res.size.height / 2f))
}
