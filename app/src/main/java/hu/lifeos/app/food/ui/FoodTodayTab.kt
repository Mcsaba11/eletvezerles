package hu.lifeos.app.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.food.Food
import hu.lifeos.app.food.FoodEntry
import hu.lifeos.app.food.FoodLogic
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.TimePickDialog
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.theme.StatusColors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

private data class Row_(val chunk: FoodLogic.Chunk, val food: Food?, val g: Double, val est: String)

fun fmtDur(min: Int): String { val m = maxOf(0, min); return "${m / 60} ó ${(m % 60).toString().padStart(2, '0')} p" }

@Composable
fun FoodTodayTab(vm: FoodViewModel, modifier: Modifier, toast: (String) -> Unit, onProfile: () -> Unit) {
    val s by vm.state.collectAsState()
    val view by vm.view.collectAsState()
    val logic = vm.logic
    val c = vm.content
    val ctx = LocalContext.current
    val today = LocalDate.now()
    val isToday = view == today
    val day = s.day(view.toString())
    val items = day.items
    val r = logic.targets(s.profile)
    val t = logic.totals(items)
    var editEntry by remember { mutableStateOf<FoodEntry?>(null) }
    var legacyOpen by remember { mutableStateOf(false) }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!s.setupDone) item {
            AppCard(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                Text(stringResource(R.string.food_welcome_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.food_welcome_text), style = MaterialTheme.typography.bodyMedium)
                VSpace(8)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { legacyOpen = true }) { Text(stringResource(R.string.food_import_old)) }
                    OutlinedButton(onClick = onProfile) { Text(stringResource(R.string.food_set_profile)) }
                }
                TextButton(onClick = { vm.markSetup() }) { Text(stringResource(R.string.food_start_fresh)) }
            }
        }

        // ---------- Böjtsáv ----------
        item { FastBar(vm) }

        // ---------- Napváltó ----------
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.setView(view.minusDays(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null) }
                Text(
                    if (isToday) stringResource(R.string.today) + ", " + Fmt.dayShort(view) else Fmt.dayLong(view),
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                )
                if (!isToday) TextButton(onClick = { vm.setView(today) }) { Text(stringResource(R.string.food_go_today)) }
                IconButton(onClick = { vm.setView(view.plusDays(1)) }, enabled = !isToday) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
            }
        }

        // ---------- Makrók ----------
        item {
            AppCard {
                MacroBar(stringResource(R.string.food_kcal), t.kcal, r.target, "kcal", "max")
                MacroBar(stringResource(R.string.food_protein), t.p, r.p.toDouble(), "g", "min")
                MacroBar(stringResource(R.string.food_carb), t.c, r.c.toDouble(), "g", "max")
                MacroBar(stringResource(R.string.food_fat), t.f, r.f.toDouble(), "g", "max")
                VSpace(8)
                val goodK = items.filter { it.r == "g" }.sumOf { it.kcal }
                val outside = items.filter { !logic.inWindow(s.profile, logic.toMin(it.t)) && it.kcal > 5 }
                val glLabel = if (items.isEmpty()) "" else " · " + stringResource(if (t.gl < 80) R.string.food_low else if (t.gl < 120) R.string.food_mid else R.string.food_high)
                TileGrid(listOf<@Composable (Modifier) -> Unit>(
                    { m -> StatTile(stringResource(R.string.food_fiber), "${fmt(t.fib)} / 35 g", modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_gl), fmt(t.gl) + glLabel, modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_good_ratio), if (t.kcal > 0) fmt(goodK / t.kcal * 100) + "%" else "–", modifier = m) },
                    { m -> StatTile(stringResource(R.string.food_fast_kept), if (items.isEmpty()) "–" else if (outside.isEmpty()) stringResource(R.string.food_yes) else stringResource(R.string.food_exceptions, outside.size), modifier = m) },
                ))
            }
        }

        // ---------- Tanácsok ----------
        item {
            AppCard {
                logic.insights(s, items).forEach { ins ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Box(Modifier.padding(top = 7.dp).width(6.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(
                            when (ins.kind) { "warn" -> StatusColors.warn; "ok" -> StatusColors.ok; else -> MaterialTheme.colorScheme.primary }))
                        Spacer(Modifier.width(10.dp))
                        Text(ins.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // ---------- Szabad szöveg ----------
        item { FreeTextCard(vm, toast) }

        // ---------- Keresés listából ----------
        item { QuickAddCard(vm, toast) }

        // ---------- Saját tétel ----------
        item { ManualCard(vm) }

        // ---------- Napló ----------
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.food_meals) + if (items.isNotEmpty()) " · " + stringResource(R.string.food_n_items, items.size) else "", Modifier.weight(1f))
                TextButton(onClick = {
                    val n = vm.copyPrev()
                    toast(if (n == 0) ctx.getString(R.string.food_prev_empty) else ctx.getString(R.string.food_copied, n))
                }) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.padding(end = 4.dp)); Text(stringResource(R.string.food_copy_prev))
                }
            }
        }
        if (items.isEmpty()) item {
            AppCard {
                EmptyHint(stringResource(R.string.food_log_empty))
                OutlinedButton(onClick = { vm.loadSample(ctx.getString(R.string.food_sample_suffix)) }) { Text(stringResource(R.string.food_load_sample)) }
            }
        }
        c.meals.forEach { meal ->
            val its = items.filter { it.meal.ifBlank { c.meals[1] } == meal }.sortedBy { it.t }
            if (its.isNotEmpty()) {
                item {
                    val mt = logic.totals(its)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                        Text(meal, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text("${fmt(mt.kcal)} kcal · F ${fmt(mt.p)} g · Sz ${fmt(mt.c)} g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(its) { x -> EntryRow(vm, x, c.rtxt, onEdit = { editEntry = x }) }
            }
        }

        // ---------- Víz ----------
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.food_water), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${fmt(day.water / 1000, 2)} / ${fmt(r.water / 1000, 1)} L", style = MaterialTheme.typography.labelLarge)
                }
                VSpace(6)
                MacroBar("", day.water, r.water, "ml", "min")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.water(-250.0) }) { Text("−250 ml") }
                    OutlinedButton(onClick = { vm.water(250.0) }) { Text("+250 ml") }
                    OutlinedButton(onClick = { vm.water(500.0) }) { Text("+500 ml") }
                }
            }
        }
    }

    editEntry?.let { e ->
        var g by remember(e.id) { mutableStateOf(fmt(e.g, 1).replace(" ", "").replace(',', '.')) }
        AlertDialog(
            onDismissRequest = { editEntry = null },
            title = { Text(e.name) },
            text = {
                Column {
                    if (e.g > 0) OutlinedTextField(g, { g = it }, label = { Text(stringResource(R.string.food_grams)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    else Text(stringResource(R.string.food_manual_item))
                }
            },
            confirmButton = {
                TextButton(onClick = { g.replace(',', '.').toDoubleOrNull()?.let { vm.setGrams(e, it) }; editEntry = null }, enabled = e.g > 0) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { vm.deleteEntry(e); editEntry = null }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
        )
    }
    if (legacyOpen) LegacyImportDialog(vm, onClose = { legacyOpen = false }, toast = toast)
}

@Composable
private fun EntryRow(vm: FoodViewModel, x: FoodEntry, rtxt: Map<String, String>, onEdit: () -> Unit) {
    val s by vm.state.collectAsState()
    val out = !vm.logic.inWindow(s.profile, vm.logic.toMin(x.t)) && x.kcal > 5
    ClickRow(onClick = onEdit) {
        Text(x.t, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(x.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(6.dp)); RatePill(x.r, rtxt[x.r])
            }
            val sub = buildString {
                if (!x.est.isNullOrBlank()) append("≈ ${x.est} · ")
                if (x.g > 0) append("${fmt(x.g)} g · ")
                append("F ${fmt(x.p)} · Sz ${fmt(x.c)} · Zs ${fmt(x.f)} · GL ${fmt(x.gl)}")
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (out) Text(stringResource(R.string.food_in_fast), style = MaterialTheme.typography.labelSmall, color = StatusColors.warn)
        }
        Text("${fmt(x.kcal)} kcal", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun FastBar(vm: FoodViewModel) {
    val s by vm.state.collectAsState()
    val logic = vm.logic
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LocalTime.now() } }
    val m = now.hour * 60 + now.minute
    val st = logic.toMin(s.profile.winStart); val en = logic.toMin(s.profile.winEnd)
    val eat = logic.inWindow(s.profile, m)
    val fastC = Color(0xFF7E8FD8); val goodC = StatusColors.ok
    AppCard(color = (if (eat) goodC else fastC).copy(alpha = 0.14f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (eat) stringResource(R.string.food_window, s.profile.winStart, s.profile.winEnd) else stringResource(R.string.food_fast_since, fmtDur(((m - en) + 1440) % 1440)),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(if (eat) R.string.food_can_eat else R.string.food_fasting), style = MaterialTheme.typography.headlineSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(if (eat) R.string.food_until_close else R.string.food_until_open), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmtDur(if (eat) ((en - m) + 1440) % 1440 else ((st - m) + 1440) % 1440), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        VSpace(10)
        Canvas(Modifier.fillMaxWidth().height(12.dp)) {
            val w = size.width; val h = size.height
            fun x(min: Int) = w * min / 1440f
            drawRoundRect(fastC.copy(alpha = 0.6f), size = Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2))
            if (st <= en) drawRect(goodC, Offset(x(st), 0f), Size(x(en) - x(st), h))
            else { drawRect(goodC, Offset(0f, 0f), Size(x(en), h)); drawRect(goodC, Offset(x(st), 0f), Size(w - x(st), h)) }
            drawRect(Color.White, Offset(x(m) - 1.5f, -2f), Size(3f, h + 4f))
        }
    }
}

@Composable
private fun FreeTextCard(vm: FoodViewModel, toast: (String) -> Unit) {
    val s by vm.state.collectAsState()
    val logic = vm.logic
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<Row_>?>(null) }
    var meal by remember { mutableStateOf(logic.defaultMeal(s.profile)) }
    var time by remember { mutableStateOf(vm.nowHM()) }
    var pickTime by remember { mutableStateOf(false) }

    AppCard {
        Text(stringResource(R.string.food_free_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.food_free_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        VSpace(8)
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.food_free_placeholder)) }, minLines = 2)
        VSpace(6)
        Button(onClick = {
            rows = logic.splitText(text).map { ch ->
                val info = logic.parseChunk(s, ch); val f = info.cands.firstOrNull(); val e = logic.estimate(s, info, f)
                Row_(info, f, Math.round(e.g).toDouble(), e.txt)
            }
            meal = logic.defaultMeal(s.profile); time = vm.nowHM()
        }, enabled = text.isNotBlank()) { Text(stringResource(R.string.food_parse)) }
        rows?.let { list ->
            VSpace(8)
            var total = 0.0
            list.forEachIndexed { i, rw ->
                val n = rw.food?.let { logic.nutr(it, rw.g) }
                if (n != null) total += n.kcal
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(rw.chunk.raw, style = MaterialTheme.typography.bodyMedium)
                            if (rw.est.isNotBlank()) Text("≈ ${rw.est}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { rows = list.filterIndexed { j, _ -> j != i }.ifEmpty { null } }) { Icon(Icons.Outlined.Close, null) }
                    }
                    if (rw.chunk.cands.isEmpty()) Text(stringResource(R.string.food_not_found), color = StatusColors.warn, style = MaterialTheme.typography.bodySmall)
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        Picker("", rw.chunk.cands, rw.food ?: rw.chunk.cands.first(), { it.name }, { f ->
                            val e = logic.estimate(s, rw.chunk, f)
                            rows = list.toMutableList().also { it[i] = rw.copy(food = f, g = Math.round(e.g).toDouble(), est = e.txt) }
                        }, Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            fmt(rw.g).replace(" ", ""), { v -> v.replace(',', '.').toDoubleOrNull()?.let { g -> rows = list.toMutableList().also { it[i] = rw.copy(g = g) } } },
                            Modifier.width(96.dp), label = { Text("g") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    if (n != null) Text("${fmt(n.kcal)} kcal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(stringResource(R.string.food_total_kcal, fmt(total)), style = MaterialTheme.typography.titleSmall)
            VSpace(6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                Picker(stringResource(R.string.food_meal), vm.content.meals, meal, { it }, { meal = it }, Modifier.weight(1f))
                OutlinedButton(onClick = { pickTime = true }) { Text(time) }
            }
            VSpace(6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val ok = list.filter { it.food != null && it.g > 0 }
                    vm.addEntries(ok.map { Triple(it.food!!, it.g, it.est) }, meal, time)
                    toast(ctx.getString(R.string.food_added_n, ok.size, fmt(ok.sumOf { logic.nutr(it.food!!, it.g).kcal })))
                    rows = null; text = ""
                }, enabled = list.any { it.food != null && it.g > 0 }) { Text(stringResource(R.string.food_add_all)) }
                TextButton(onClick = { rows = null }) { Text(stringResource(R.string.cancel)) }
            }
        }
        VSpace(6)
        Text(stringResource(R.string.food_free_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (pickTime) TimePickDialog(runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.now()), { pickTime = false }) { time = it.format(Fmt.time) }
}

@Composable
fun QuickAddCard(vm: FoodViewModel, toast: (String) -> Unit, title: Int = R.string.food_quick_title) {
    val s by vm.state.collectAsState()
    val logic = vm.logic
    val ctx = LocalContext.current
    var food by remember { mutableStateOf<Food?>(null) }
    var qty by remember { mutableStateOf("100") }
    val opts = logic.unitOpts(food)
    var unit by remember(food) { mutableStateOf(if (food != null && food!!.ug > 0 && food!!.u.isNotBlank()) opts.getOrNull(1) ?: opts.first() else opts.first()) }
    var sizeK by remember { mutableStateOf(1.0) }
    var meal by remember { mutableStateOf(logic.defaultMeal(s.profile)) }
    var time by remember { mutableStateOf(vm.nowHM()) }
    var pickTime by remember { mutableStateOf(false) }
    LaunchedEffect(food) { if (food != null) qty = if (food!!.ug > 0 && food!!.u.isNotBlank()) "1" else "100" }

    val q = qty.replace(',', '.').toDoubleOrNull() ?: 0.0
    val g = q * unit.g * (if (unit.v != "g") sizeK else 1.0)
    val est = if (unit.v == "g") null else "${fmt(q, if (q % 1 != 0.0) 1 else 0)} ${if (sizeK < 1) "kicsi " else if (sizeK > 1) "nagy " else ""}${unit.name}"

    AppCard {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        VSpace(6)
        FoodSearchField(s, logic, vm.content.rtxt, stringResource(R.string.food_food), onPick = { food = it })
        VSpace(6)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(qty, { qty = it }, Modifier.width(90.dp), label = { Text(stringResource(R.string.food_qty)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Picker(stringResource(R.string.food_unit), opts, unit, { it.label }, { unit = it }, Modifier.weight(1f))
        }
        if (unit.v != "g") {
            VSpace(6)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.7 to R.string.food_small, 1.0 to R.string.food_medium, 1.4 to R.string.food_big).forEach { (k, l) ->
                    FilterChip(sizeK == k, { sizeK = k }, label = { Text(stringResource(l)) })
                }
            }
            if (unit.hint.isNotBlank()) Text(stringResource(R.string.food_unit_hint, unit.name, unit.hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        VSpace(6)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            Picker(stringResource(R.string.food_meal), vm.content.meals, meal, { it }, { meal = it }, Modifier.weight(1f))
            OutlinedButton(onClick = { pickTime = true }) { Text(time) }
        }
        VSpace(6)
        val f = food
        if (f == null) Text(stringResource(R.string.food_pick_food), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            val n = logic.nutr(f, g)
            Text("${fmt(g)} g → ${fmt(n.kcal)} kcal · F ${fmt(n.p, 1)} · Sz ${fmt(n.c, 1)} · Zs ${fmt(n.f, 1)} g", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        VSpace(6)
        Button(onClick = {
            food?.let { ff ->
                vm.addEntry(ff, g, meal, time, est = est)
                toast(ctx.getString(R.string.food_added, ff.name, fmt(logic.nutr(ff, g).kcal)))
            }
        }, enabled = food != null && g > 0) { Text(stringResource(R.string.add)) }
    }
    if (pickTime) TimePickDialog(runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.now()), { pickTime = false }) { time = it.format(Fmt.time) }
}

@Composable
private fun ManualCard(vm: FoodViewModel) {
    val s by vm.state.collectAsState()
    var open by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var cc by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    AppCard {
        TextButton(onClick = { open = !open }) { Text(stringResource(R.string.food_manual_title)) }
        if (open) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_title)) }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple("kcal", kcal) { v: String -> kcal = v }, Triple(stringResource(R.string.food_p_short), p) { v: String -> p = v },
                    Triple(stringResource(R.string.food_c_short), cc) { v: String -> cc = v }, Triple(stringResource(R.string.food_f_short), f) { v: String -> f = v },
                ).forEach { (l, v, set) ->
                    OutlinedTextField(v, set, Modifier.weight(1f), label = { Text(l) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
            VSpace(6)
            Button(onClick = {
                fun d(x: String) = x.replace(',', '.').toDoubleOrNull() ?: 0.0
                vm.addManual(name, d(kcal), d(p), d(cc), d(f), vm.logic.defaultMeal(s.profile), vm.nowHM())
                name = ""; kcal = ""; p = ""; cc = ""; f = ""; open = false
            }, enabled = name.isNotBlank()) { Text(stringResource(R.string.food_record)) }
        }
    }
}

@Composable
fun LegacyImportDialog(vm: FoodViewModel, onClose: () -> Unit, toast: (String) -> Unit) {
    val ctx = LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            if (text.isNullOrBlank()) toast(ctx.getString(R.string.backup_bad))
            else vm.importLegacy(text) { n -> toast(if (n == null) ctx.getString(R.string.backup_bad) else ctx.getString(R.string.food_imported_n, n)) }
            onClose()
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.food_import_old)) },
        text = { Text(stringResource(R.string.food_import_help)) },
        confirmButton = { TextButton(onClick = { launcher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text(stringResource(R.string.food_choose_file)) } },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    )
}
