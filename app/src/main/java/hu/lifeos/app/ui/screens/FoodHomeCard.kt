package hu.lifeos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.food.FoodLogic
import hu.lifeos.app.food.FoodState
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.food.packFlags
import hu.lifeos.app.insight.Insights
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CardHeader
import hu.lifeos.app.ui.Meter
import hu.lifeos.app.ui.MetricRing
import hu.lifeos.app.ui.Pill
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate
import java.time.LocalTime

/** Az Étkezés kártyája a kezdőlapon; a bekapcsolt csomagokhoz igazodik. */
@Composable
fun FoodHomeCard(food: FoodViewModel, fs: FoodState, now: LocalTime, onOpen: () -> Unit) {
    val packs = fs.packFlags()
    val logic = food.logic
    val day = fs.day(LocalDate.now().toString())
    val items = day.items
    val t = logic.totals(items)
    val r = logic.targets(fs)

    AppCard(onClick = onOpen, glow = if (!fs.setupDone) Noc.good else null) {
        if (!fs.setupDone) {
            CardHeader(Icons.Outlined.Restaurant, stringResource(R.string.app_food), tint = Noc.good)
            VSpace(8)
            Text(stringResource(R.string.food_home_setup), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
            return@AppCard
        }
        if (packs.fast) {
            val f = Insights.fast(fs, logic, now)
            CardHeader(Icons.Outlined.Timer, stringResource(R.string.card_fast), tint = Noc.cyan) {
                Pill(stringResource(if (f.eating) R.string.food_can_eat else if (f.minutesIn >= 12 * 60 && !packs.ed) R.string.phase_autophagy else R.string.food_fasting),
                    textColor = if (f.eating) Noc.good else Noc.amber)
            }
            VSpace(10)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%d:%02d".format(f.minutesIn / 60, f.minutesIn % 60), style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"), color = Noc.text)
                        Text(" / " + (if (f.eating) (24 * 60 - f.fastLength) / 60 else f.fastLength / 60) + "h", style = MaterialTheme.typography.titleSmall, color = Noc.text2, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Text(stringResource(if (f.eating) R.string.hours_in_window else R.string.hours_in_fast), style = MaterialTheme.typography.bodyMedium, color = if (f.eating) Noc.good else Noc.cyan)
                    VSpace(4)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Restaurant, null, Modifier.size(15.dp), tint = Noc.text2)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (f.eating) R.string.window_closes else R.string.window_opens, if (f.eating) f.closes else f.opens), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                    }
                }
                val total = if (f.eating) 24 * 60 - f.fastLength else f.fastLength
                MetricRing(if (total > 0) f.minutesIn.toFloat() / total else 0f, if (f.eating) Noc.good else Noc.cyan, Modifier.size(86.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${if (total > 0) (f.minutesIn * 100 / total).coerceAtMost(100) else 0}%", style = MaterialTheme.typography.titleMedium, color = Noc.text)
                        Text(stringResource(R.string.goal_short), style = MaterialTheme.typography.labelSmall, color = Noc.text2)
                    }
                }
            }
        } else {
            CardHeader(Icons.Outlined.Restaurant, stringResource(R.string.app_food), tint = Noc.good) {
                Text(stringResource(R.string.food_n_items, items.size), style = MaterialTheme.typography.labelMedium, color = Noc.text2)
            }
        }

        if (packs.ed) {
            // Számok nélkül: melyik étkezés volt már ma.
            VSpace(10)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                logic.meals(packs).dropLast(1).forEach { m ->
                    val done = items.any { it.meal == m }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(20.dp), tint = if (done) Noc.good else Noc.text3)
                        Text(m, style = MaterialTheme.typography.labelSmall, color = if (done) Noc.text else Noc.text3, maxLines = 1)
                    }
                }
            }
        } else {
            VSpace(10)
            WellRow(stringResource(R.string.kcal_today), "${FoodLogic.fmt(t.kcal)} / ${FoodLogic.fmt(r.target)} kcal")
            if (!packs.fast) { VSpace(6); Meter((t.kcal / r.target).toFloat(), if (t.kcal > r.target * 1.05) Noc.coral else Noc.good) }
            if (packs.ir) {
                VSpace(6)
                val glTxt = stringResource(if (t.gl < 80) R.string.gl_low_stable else if (t.gl < 120) R.string.gl_mid else R.string.gl_high)
                WellRow(stringResource(R.string.food_gl), glTxt, if (t.gl < 80) Noc.good else if (t.gl < 120) Noc.amber else Noc.coral)
            }
        }
        if (packs.diab) {
            VSpace(6)
            val p = fs.profile
            WellRow(stringResource(R.string.diab_ch_today), if (p.carbGoal > 0) "${FoodLogic.fmt(t.c)} / ${FoodLogic.fmt(p.carbGoal)} g" else "${FoodLogic.fmt(t.c)} g")
            day.bg.lastOrNull()?.let { b ->
                VSpace(6)
                val col = if (b.v < p.bgLow) Noc.coral else if (b.v > p.bgHigh) Noc.amber else Noc.good
                WellRow(stringResource(R.string.diab_bg_last, b.t), "${FoodLogic.fmt(b.v, 1)} mmol/l", col)
            }
        }
    }
}

@Composable
private fun WellRow(label: String, value: String, color: androidx.compose.ui.graphics.Color = Noc.text) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Noc.well).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Noc.text2, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = color)
    }
}
