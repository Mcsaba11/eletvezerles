package hu.lifeos.app.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.food.WeightRec
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.DatePickDialog
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.VSpace
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

@Composable
fun FoodWeightTab(vm: FoodViewModel, modifier: Modifier, toast: (String) -> Unit) {
    val s by vm.state.collectAsState()
    val logic = vm.logic
    val ctx = LocalContext.current
    var date by remember { mutableStateOf(LocalDate.now()) }
    var kg by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var pick by remember { mutableStateOf(false) }
    val l = s.weights
    val tr = logic.trend(l)
    val tg = s.profile.target.takeIf { it > 0 }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AppCard {
                Text(stringResource(R.string.food_weight_add), style = MaterialTheme.typography.titleMedium)
                VSpace(6)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    OutlinedButton(onClick = { pick = true }) { Text(Fmt.dayShort(date)) }
                    OutlinedTextField(kg, { kg = it }, Modifier.width(100.dp), label = { Text("kg") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(waist, { waist = it }, Modifier.width(110.dp), label = { Text(stringResource(R.string.food_waist)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                VSpace(6)
                Button(onClick = {
                    kg.replace(',', '.').toDoubleOrNull()?.let { k ->
                        vm.addWeight(date, k, waist.replace(',', '.').toDoubleOrNull())
                        kg = ""; waist = ""; toast(ctx.getString(R.string.food_weight_saved))
                    }
                }, enabled = (kg.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0) { Text(stringResource(R.string.save)) }
            }
        }
        if (l.isEmpty()) item { EmptyHint(stringResource(R.string.food_weight_empty, fmt(s.profile.weight, 1))) }
        else {
            item { AppCard { WeightChart(l, tr, tg) } }
            item {
                val li = l.lastIndex
                val first = l.first(); val last = l[li]
                val chg = last.kg - first.kg
                val sl = logic.slopePerDay(l)
                val bmi = last.kg / ((s.profile.height / 100) * (s.profile.height / 100))
                val proj = if (sl != null && sl < 0 && tg != null && tr[li] > tg)
                    Fmt.dayShort(LocalDate.now().plusDays(((tr[li] - tg) / -sl).roundToLong())) + " " + LocalDate.now().plusDays(((tr[li] - tg) / -sl).roundToLong()).year
                else "–"
                val ws = l.filter { it.waist != null }
                val wchg = if (ws.size > 1) ws.last().waist!! - ws.first().waist!! else null
                TileGrid(listOf<@Composable (Modifier) -> Unit>(
                    { m -> StatTile(stringResource(R.string.food_w_trend), "${fmt(tr[li], 1)} kg", stringResource(R.string.food_w_last, fmt(last.kg, 1)), true, m) },
                    { m -> StatTile(stringResource(R.string.food_w_change), (if (chg > 0) "+" else "") + fmt(chg, 1) + " kg", stringResource(R.string.food_w_since, Fmt.dayShort(LocalDate.parse(first.d))), modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_w_pace), if (sl == null) "–" else (if (sl > 0) "+" else "") + fmt(sl * 7, 2) + " kg", stringResource(if (sl == null) R.string.food_w_pace_need else R.string.food_w_pace_sub), modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_w_left), if (tg != null) fmt(maxOf(0.0, tr[li] - tg), 1) + " kg" else "–", if (tg != null) stringResource(R.string.food_w_goal, fmt(tg, 1)) else "", modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_w_eta), proj, stringResource(R.string.food_w_eta_sub), modifier = m) },
                    { m -> StatTile("BMI", fmt(bmi, 1), if (wchg != null) stringResource(R.string.food_w_waist_chg, (if (wchg > 0) "+" else "") + fmt(wchg, 1)) else stringResource(R.string.food_w_no_waist), modifier = m) },
                ))
            }
            items(l.indices.reversed().toList()) { i ->
                val x = l[i]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Fmt.dayShort(LocalDate.parse(x.d)) + " " + LocalDate.parse(x.d).year, modifier = Modifier.weight(1f))
                    Text("${fmt(x.kg, 1)} kg", modifier = Modifier.width(70.dp))
                    Text(stringResource(R.string.food_w_trend_short, fmt(tr[i], 1)), modifier = Modifier.width(90.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(x.waist?.let { fmt(it, 1) + " cm" } ?: "–", modifier = Modifier.width(64.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { vm.deleteWeight(x.d) }) { Icon(Icons.Outlined.Close, stringResource(R.string.delete)) }
                }
            }
        }
    }
    if (pick) DatePickDialog(date, { pick = false }) { date = if (it.isAfter(LocalDate.now())) LocalDate.now() else it }
}

@Composable
private fun WeightChart(l: List<WeightRec>, tr: List<Double>, tg: Double?) {
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val trendC = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val tgC = MaterialTheme.colorScheme.tertiary
    val d0 = LocalDate.parse(l.first().d)
    val days = l.map { ChronoUnit.DAYS.between(d0, LocalDate.parse(it.d)).toFloat() }
    val span = maxOf(6f, days.last())
    val kgs = l.map { it.kg } + listOfNotNull(tg)
    val y0 = floor(kgs.min() - 1).toFloat(); val y1 = ceil(kgs.max() + 1).toFloat()
    Column {
        Row {
            Text("${fmt(y1.toDouble())} kg", style = MaterialTheme.typography.labelSmall, color = line, modifier = Modifier.weight(1f))
            if (tg != null) Text(stringResource(R.string.food_w_goal, fmt(tg, 1)), style = MaterialTheme.typography.labelSmall, color = tgC)
        }
        Canvas(Modifier.fillMaxWidth().height(200.dp)) {
            fun x(d: Float) = size.width * d / span
            fun y(v: Double) = size.height * (y1 - v.toFloat()) / (y1 - y0)
            for (k in 0..4) { val yy = size.height * k / 4f; drawLine(grid, Offset(0f, yy), Offset(size.width, yy), 1f) }
            if (tg != null) drawLine(tgC, Offset(0f, y(tg)), Offset(size.width, y(tg)), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            val p = Path(); val pt = Path()
            l.forEachIndexed { i, w ->
                if (i == 0) { p.moveTo(x(days[i]), y(w.kg)); pt.moveTo(x(days[i]), y(tr[i])) }
                else { p.lineTo(x(days[i]), y(w.kg)); pt.lineTo(x(days[i]), y(tr[i])) }
            }
            drawPath(p, line.copy(alpha = 0.5f), style = Stroke(2f))
            drawPath(pt, trendC, style = Stroke(5f, cap = StrokeCap.Round))
            l.forEachIndexed { i, w -> drawCircle(line, 4f, Offset(x(days[i]), y(w.kg))) }
            drawCircle(trendC, 9f, Offset(x(days.last()), y(tr.last())))
        }
        Row {
            Text("${fmt(y0.toDouble())} kg · ${Fmt.dayShort(d0)}", style = MaterialTheme.typography.labelSmall, color = line, modifier = Modifier.weight(1f))
            Text(Fmt.dayShort(LocalDate.parse(l.last().d)), style = MaterialTheme.typography.labelSmall, color = line)
        }
    }
}
