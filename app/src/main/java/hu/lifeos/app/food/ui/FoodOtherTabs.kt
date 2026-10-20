package hu.lifeos.app.food.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.food.FoodPacks
import hu.lifeos.app.food.LocalPacks
import hu.lifeos.app.food.packIds
import hu.lifeos.app.food.Profile
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.TimePickDialog
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.theme.StatusColors
import java.time.LocalDate
import java.time.LocalTime

// ---------------- Statisztika ----------------

@Composable
fun FoodStatsTab(vm: FoodViewModel, modifier: Modifier) {
    val s by vm.state.collectAsState()
    val logic = vm.logic
    var range by rememberSaveable { mutableIntStateOf(7) }
    val r = logic.targets(s)
    val keys = (range - 1 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val tots = keys.map { logic.totals(s.day(it.toString()).items) }
    val logged = keys.indices.filter { s.day(keys[it].toString()).items.isNotEmpty() }
    val n = maxOf(1, logged.size)
    fun avg(f: (hu.lifeos.app.food.Nutr) -> Double) = logged.sumOf { f(tots[it]) } / n
    val all = logged.flatMap { s.day(keys[it].toString()).items }
    val inside = all.count { logic.inWindow(s.profile, logic.toMin(it.t)) || it.kcal <= 5 }
    val kHit = logged.count { tots[it].kcal <= r.target * 1.1 }
    val pHit = logged.count { tots[it].p >= r.p }
    val waters = keys.map { s.day(it.toString()).water }.filter { it > 0 }
    val has = logged.isNotEmpty()
    val packs = LocalPacks.current
    val tAvgKcal = stringResource(R.string.food_avg_kcal); val tAvgP = stringResource(R.string.food_avg_p); val tAvgC = stringResource(R.string.food_avg_c)
    val tAvgGl = stringResource(R.string.food_avg_gl); val tBudget = stringResource(R.string.food_in_budget); val tFast = stringResource(R.string.food_fast_rate)
    val tFib = stringResource(R.string.food_avg_fib); val tWater = stringResource(R.string.food_avg_water); val tBal = stringResource(R.string.food_balance)
    val sAvgKcal = stringResource(R.string.food_avg_kcal_sub, fmt(r.target), logged.size); val sAvgP = stringResource(R.string.food_avg_p_sub, r.p, pHit)
    val sGoalC = stringResource(R.string.food_goal_g, r.c); val sGl = stringResource(R.string.food_gl_good); val sDays = stringResource(R.string.food_days)
    val sFast = stringResource(R.string.food_fast_rate_sub); val sFib = stringResource(R.string.food_goal_g, 30)
    val sWater = stringResource(R.string.food_goal_l, fmt(r.water / 1000, 1)); val sBal = stringResource(R.string.food_balance_sub)
    val bgAll = keys.flatMap { s.day(it.toString()).bg }
    val tBg = stringResource(R.string.diab_bg_avg); val sBg = stringResource(R.string.diab_bg_in_range, bgAll.count { it.v in s.profile.bgLow..s.profile.bgHigh }, bgAll.size)
    val tiles = mutableListOf<@Composable (Modifier) -> Unit>(
        { m -> StatTile(tAvgKcal, if (has) fmt(avg { it.kcal }) else "–", sAvgKcal, true, m) },
        { m -> StatTile(tAvgP, if (has) fmt(avg { it.p }) + " g" else "–", sAvgP, modifier = m) },
        { m -> StatTile(tAvgC, if (has) fmt(avg { it.c }) + " g" else "–", sGoalC, modifier = m) },
        { m -> StatTile(tBudget, if (has) "$kHit / ${logged.size}" else "–", sDays, modifier = m) },
        { m -> StatTile(tFib, if (has) fmt(avg { it.fib }) + " g" else "–", sFib, modifier = m) },
        { m -> StatTile(tWater, if (waters.isNotEmpty()) fmt(waters.average() / 1000, 1) + " L" else "–", sWater, modifier = m) },
    )
    if (packs.ir) tiles += { m -> StatTile(tAvgGl, if (has) fmt(avg { it.gl }) else "–", sGl, modifier = m) }
    if (packs.fast) tiles += { m -> StatTile(tFast, if (all.isNotEmpty()) fmt(inside * 100.0 / all.size) + "%" else "–", sFast, modifier = m) }
    if (packs.diab) tiles += { m -> StatTile(tBg, if (bgAll.isNotEmpty()) fmt(bgAll.map { it.v }.average(), 1) + " mmol/l" else "–", sBg, modifier = m) }
    tiles += { m -> StatTile(tBal, if (has) (if (avg { it.kcal } - r.tdee > 0) "+" else "") + fmt((avg { it.kcal } - r.tdee) * 7 / 7700, 2) + " kg" else "–", sBal, modifier = m) }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 14, 30).forEach { d -> FilterChip(range == d, { range = d }, label = { Text(stringResource(R.string.food_n_days, d)) }) }
            }
        }
        item {
            TileGrid(tiles)
        }
        if (has) {
            item { AppCard { Text(stringResource(R.string.food_kcal_chart), style = MaterialTheme.typography.titleSmall); VSpace(6); BarChart(tots.map { it.kcal }, r.target, "max", keys) } }
            item { AppCard { Text(stringResource(R.string.food_p_chart), style = MaterialTheme.typography.titleSmall); VSpace(6); BarChart(tots.map { it.p }, r.p.toDouble(), "min", keys) } }
            val freq = all.groupBy { it.name.replace(" (minta)", "") }.map { (k, v) -> Triple(k, v, v.sumOf { it.kcal }) }.sortedByDescending { it.second.size }
            item {
                AppCard {
                    Text(stringResource(R.string.food_top), style = MaterialTheme.typography.titleSmall)
                    freq.take(8).forEach { (k, v, kc) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(k, modifier = Modifier.weight(1f)); RatePill(v.first().r, vm.content.rtxt[v.first().r])
                            Text("  ${v.size}× · ${fmt(kc)} kcal", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (packs.rating) item {
                val bad = freq.filter { it.second.first().r == "b" }
                AppCard {
                    Text(stringResource(R.string.food_bad_list), style = MaterialTheme.typography.titleSmall)
                    if (bad.isEmpty()) Text(stringResource(R.string.food_no_bad), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    bad.forEach { (k, v, kc) -> Text("$k · ${v.size}× · ${fmt(kc)} kcal", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        } else item { Text(stringResource(R.string.food_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun BarChart(vals: List<Double>, target: Double, mode: String, keys: List<LocalDate>) {
    val base = MaterialTheme.colorScheme.primary
    val none = MaterialTheme.colorScheme.surfaceContainerHighest
    val tgC = MaterialTheme.colorScheme.tertiary
    val mx = maxOf(target * 1.25, vals.maxOrNull() ?: 0.0, 1.0)
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val n = vals.size
        val bw = size.width / n
        val gap = minOf(6f, bw * .25f)
        vals.forEachIndexed { i, v ->
            val h = (v / mx * size.height).toFloat()
            val c: Color = when {
                v == 0.0 -> none
                mode == "max" && v > target * 1.05 -> StatusColors.bad
                mode == "max" && v >= target * .85 -> StatusColors.ok
                mode == "min" && v >= target -> StatusColors.ok
                else -> base
            }
            val hh = if (v == 0.0) 3f else maxOf(2f, h)
            drawRect(c, Offset(i * bw + gap / 2, size.height - hh), Size(bw - gap, hh))
        }
        val ty = (size.height - target / mx * size.height).toFloat()
        drawLine(tgC, Offset(0f, ty), Offset(size.width, ty), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    }
    Row {
        Text("${keys.first().monthValue}.${keys.first().dayOfMonth}.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text("${keys.last().monthValue}.${keys.last().dayOfMonth}.", style = MaterialTheme.typography.labelSmall)
    }
}

// ---------------- Tippek ----------------

@Composable
fun FoodTipsTab(vm: FoodViewModel, modifier: Modifier) {
    val packs = LocalPacks.current
    val tips = vm.content.tips.filter { t ->
        when (t.pack) {
            "" -> true
            "num" -> packs.numbers
            FoodPacks.IR -> packs.ir && !packs.ed
            FoodPacks.FAST -> packs.fast
            FoodPacks.ED -> packs.ed
            FoodPacks.DIAB -> packs.diab
            else -> true
        }
    }.sortedByDescending { it.pack == FoodPacks.ED }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(tips) { sec ->
            AppCard {
                Text(sec.t, style = MaterialTheme.typography.titleMedium)
                sec.items.forEach { it ->
                    VSpace(8)
                    Text(it.getOrElse(0) { "" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(it.getOrElse(1) { "" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text(vm.content.disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

// ---------------- Profil ----------------

@Composable
fun FoodProfileTab(vm: FoodViewModel, modifier: Modifier, toast: (String) -> Unit) {
    val s by vm.state.collectAsState()
    val p = s.profile
    val c = vm.content
    val ctx = LocalContext.current
    val r = vm.logic.targets(s)
    val packs = LocalPacks.current
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var wipeAsk by remember { mutableStateOf(false) }
    var legacy by remember { mutableStateOf(false) }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val ok = runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.csv().toByteArray()) } }.isSuccess
            toast(ctx.getString(if (ok) R.string.backup_saved else R.string.backup_failed))
        }
    }

    @Composable
    fun num(label: String, value: Double, set: (Profile, Double) -> Profile, mod: Modifier) {
        var t by remember(label) { mutableStateOf((if (value % 1 == 0.0) fmt(value) else fmt(value, 1)).replace(" ", "").replace(" ", "")) }
        OutlinedTextField(t, { v -> t = v; v.replace(',', '.').toDoubleOrNull()?.let { d -> vm.setProfile { pp -> set(pp, d) } } },
            mod, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
    }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AppCard {
                Text(stringResource(R.string.pack_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.pack_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(6)
                PackToggles(s.packIds()) { vm.togglePack(it) }
            }
        }
        item {
            AppCard {
                Text(stringResource(R.string.food_your_data), style = MaterialTheme.typography.titleMedium)
                VSpace(6)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(p.sex == "m", { vm.setProfile { it.copy(sex = "m") } }, label = { Text(stringResource(R.string.food_male)) })
                    FilterChip(p.sex == "f", { vm.setProfile { it.copy(sex = "f") } }, label = { Text(stringResource(R.string.food_female)) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    num(stringResource(R.string.food_age), p.age, { pp, d -> pp.copy(age = d) }, Modifier.weight(1f))
                    num(stringResource(R.string.food_height), p.height, { pp, d -> pp.copy(height = d) }, Modifier.weight(1f))
                    num(stringResource(R.string.food_weight), p.weight, { pp, d -> pp.copy(weight = d) }, Modifier.weight(1f))
                }
                VSpace(6)
                Picker(stringResource(R.string.food_activity), c.acts, c.acts.firstOrNull { it[0] == p.act } ?: c.acts[1], { it[1] }, { a -> vm.setProfile { it.copy(act = a[0]) } })
                if (packs.numbers) {
                    VSpace(6)
                    Picker(stringResource(R.string.food_goal), c.goals, c.goals.firstOrNull { it[0] == p.goal } ?: c.goals[0], { it[1] }, { g -> vm.setProfile { it.copy(goal = g[0]) } })
                }
                VSpace(6)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (packs.numbers) num(stringResource(R.string.food_target_kg), p.target, { pp, d -> pp.copy(target = d) }, Modifier.weight(1f))
                    num(stringResource(R.string.food_water_ml), p.water, { pp, d -> pp.copy(water = d) }, Modifier.weight(1f))
                }
                if (packs.fast) {
                    VSpace(6)
                    Text(stringResource(R.string.food_eating_window), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickStart = true }) { Text(p.winStart) }
                        Text("–", modifier = Modifier.padding(top = 10.dp))
                        OutlinedButton(onClick = { pickEnd = true }) { Text(p.winEnd) }
                    }
                }
                if (packs.diab) {
                    VSpace(10)
                    Text(stringResource(R.string.pack_diab), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        num(stringResource(R.string.diab_carb_goal), p.carbGoal, { pp, d -> pp.copy(carbGoal = d) }, Modifier.weight(1f))
                        num(stringResource(R.string.diab_bg_low), p.bgLow, { pp, d -> pp.copy(bgLow = d) }, Modifier.weight(1f))
                        num(stringResource(R.string.diab_bg_high), p.bgHigh, { pp, d -> pp.copy(bgHigh = d) }, Modifier.weight(1f))
                    }
                    Text(stringResource(R.string.diab_profile_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (packs.numbers) item {
            AppCard {
                Text(stringResource(R.string.food_calc_targets), style = MaterialTheme.typography.titleMedium)
                VSpace(6)
                val bmiTxt = stringResource(if (r.bmi < 18.5) R.string.food_bmi_low else if (r.bmi < 25) R.string.food_bmi_ok else if (r.bmi < 30) R.string.food_bmi_over else R.string.food_bmi_obese)
                TileGrid(listOf<@Composable (Modifier) -> Unit>(
                    { m -> StatTile("BMI", fmt(r.bmi, 1), bmiTxt, modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_bmr), fmt(r.bmr), "kcal / nap", modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_tdee), fmt(r.tdee), "kcal / nap", modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_target_kcal), fmt(r.target), "kcal / nap", true, m) },
                    { m -> StatTile(stringResource(R.string.food_protein), "${r.p} g", stringResource(R.string.food_p_rule), modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_carb), "${r.c} g", stringResource(R.string.food_c_rule), modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_fat), "${r.f} g", stringResource(R.string.food_f_rule), modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_fiber), "35+ g", stringResource(R.string.food_min), modifier = m) },
                ))
                if (r.floored) { VSpace(6); Text(stringResource(R.string.food_floor_note, fmt(r.floor.toDouble())), style = MaterialTheme.typography.bodySmall, color = StatusColors.warn) }
                VSpace(6)
                Text(stringResource(R.string.food_formula_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionLabel(stringResource(R.string.set_data))
            AppCard {
                TextButton(onClick = { legacy = true }) { Text(stringResource(R.string.food_import_old)) }
                TextButton(onClick = { csvLauncher.launch("ir-naplo-${LocalDate.now()}.csv") }) { Text(stringResource(R.string.food_export_csv)) }
                Text(stringResource(R.string.food_backup_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(6)
                if (!wipeAsk) TextButton(onClick = { wipeAsk = true }) { Text(stringResource(R.string.food_wipe), color = MaterialTheme.colorScheme.error) }
                else Row {
                    TextButton(onClick = { vm.wipe(); wipeAsk = false; toast(ctx.getString(R.string.food_wiped)) }) { Text(stringResource(R.string.food_wipe_yes), color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { wipeAsk = false }) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
    if (pickStart) TimePickDialog(runCatching { LocalTime.parse(p.winStart) }.getOrDefault(LocalTime.NOON), { pickStart = false }) { t -> vm.setProfile { it.copy(winStart = t.toString()) } }
    if (pickEnd) TimePickDialog(runCatching { LocalTime.parse(p.winEnd) }.getOrDefault(LocalTime.of(20, 0)), { pickEnd = false }) { t -> vm.setProfile { it.copy(winEnd = t.toString()) } }
    if (legacy) LegacyImportDialog(vm, onClose = { legacy = false }, toast = toast)
}

