package hu.lifeos.app.food.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.food.FoodDay
import hu.lifeos.app.food.FoodEntry
import hu.lifeos.app.food.FoodPacks
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.food.packIds
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.Meter
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.TimePickDialog
import hu.lifeos.app.ui.TonalButton
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.theme.Noc
import hu.lifeos.app.ui.theme.StatusColors
import java.time.LocalTime

// ---------------- Csomagválasztó ----------------

/** A csomagok neve és leírása (string-azonosítók). */
fun packLabel(id: String): Pair<Int, Int> = when (id) {
    FoodPacks.IR -> R.string.pack_ir to R.string.pack_ir_desc
    FoodPacks.FAST -> R.string.pack_fast to R.string.pack_fast_desc
    FoodPacks.ED -> R.string.pack_ed to R.string.pack_ed_desc
    else -> R.string.pack_diab to R.string.pack_diab_desc
}

/** Kapcsolók a fókuszcsomagokhoz. Közvetlenül a tárolót módosítja. */
@Composable
fun PackToggles(selected: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FoodPacks.ALL.forEach { id ->
            val (t, d) = packLabel(id)
            val on = id in selected
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onToggle(id) }.padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(t), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(d), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                }
                Spacer(Modifier.width(8.dp))
                Switch(on, { onToggle(id) })
            }
        }
        if (FoodPacks.ED in selected) {
            Text(stringResource(R.string.pack_ed_note), style = MaterialTheme.typography.bodySmall, color = Noc.amber, modifier = Modifier.padding(4.dp))
        }
    }
}

/** Az Étkezés első megnyitásakor: mire figyeljen, régi adatok betöltése. */
@Composable
fun PackSetupCard(vm: FoodViewModel, onImport: () -> Unit, onProfile: () -> Unit) {
    val s by vm.state.collectAsState()
    var sel by remember { mutableStateOf(s.packIds()) }
    AppCard(glow = Noc.good) {
        Text(stringResource(R.string.food_welcome_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.pack_setup_text), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
        VSpace(8)
        PackToggles(sel) { id -> sel = if (id in sel) sel - id else sel + id }
        VSpace(8)
        PrimaryButton(stringResource(R.string.pack_setup_go), onClick = { vm.setPacks(sel) }, modifier = Modifier.fillMaxWidth(), color = Noc.good)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onProfile) { Text(stringResource(R.string.food_set_profile)) }
            TextButton(onClick = onImport) { Text(stringResource(R.string.food_import_old)) }
        }
    }
}

// ---------------- Étkezési zavar csomag ----------------

private val hungerLabels = listOf(R.string.hunger_1, R.string.hunger_2, R.string.hunger_3, R.string.hunger_4, R.string.hunger_5)
private val fullLabels = listOf(R.string.full_1, R.string.full_2, R.string.full_3, R.string.full_4, R.string.full_5)

/** Ritmus: melyik étkezés volt már ma, számok nélkül. */
@Composable
fun EdRhythmCard(items: List<FoodEntry>, meals: List<String>) {
    AppCard(glow = Noc.good) {
        Text(stringResource(R.string.ed_rhythm), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.ed_rhythm_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
        VSpace(10)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            meals.dropLast(1).forEach { m ->
                val done = items.any { it.meal == m }
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(if (done) Noc.good.copy(alpha = 0.16f) else Noc.well)
                        .border(1.dp, if (done) Noc.good else Noc.stroke, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(16.dp), tint = if (done) Noc.good else Noc.text3)
                    Spacer(Modifier.width(6.dp))
                    Text(m, style = MaterialTheme.typography.labelLarge, color = if (done) Noc.text else Noc.text2)
                }
            }
        }
    }
}

@Composable
private fun Scale(title: String, labels: List<Int>, value: Int?, onChange: (Int?) -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = Noc.text2)
    VSpace(4)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { n ->
            val on = value == n
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) Noc.good.copy(alpha = 0.2f) else Noc.well)
                    .border(1.dp, if (on) Noc.good else Noc.stroke, RoundedCornerShape(12.dp))
                    .clickable { onChange(if (on) null else n) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { Text("$n", style = MaterialTheme.typography.titleSmall, color = if (on) Noc.good else Noc.text2) }
        }
    }
    value?.let { Text(stringResource(labels[it - 1]), style = MaterialTheme.typography.bodySmall, color = Noc.good, modifier = Modifier.padding(top = 2.dp)) }
}

@Composable
private fun FeelChips(feelings: List<String>, value: String?, onChange: (String?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        feelings.forEach { f ->
            val on = value == f
            Text(
                f, style = MaterialTheme.typography.labelLarge, color = if (on) Noc.abyss else Noc.text2,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Noc.good else Noc.card)
                    .border(1.dp, if (on) Noc.good else Noc.stroke, RoundedCornerShape(50))
                    .clickable { onChange(if (on) null else f) }.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

/** Számok nélküli rögzítés: mit ettem, éhség előtte, teltség utána, érzés. */
@Composable
fun EdLogCard(vm: FoodViewModel, toast: (String) -> Unit) {
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current
    val meals = vm.logic.meals(s)
    var desc by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(vm.logic.defaultMeal(s)) }
    var time by remember { mutableStateOf(vm.nowHM()) }
    var hunger by remember { mutableStateOf<Int?>(null) }
    var full by remember { mutableStateOf<Int?>(null) }
    var feel by remember { mutableStateOf<String?>(null) }
    var pickTime by remember { mutableStateOf(false) }
    AppCard {
        Text(stringResource(R.string.ed_log_title), style = MaterialTheme.typography.titleMedium)
        VSpace(8)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            Picker(stringResource(R.string.food_meal), meals, meal, { it }, { meal = it }, Modifier.weight(1f))
            OutlinedButton(onClick = { pickTime = true }) { Text(time) }
        }
        VSpace(8)
        OutlinedTextField(
            desc, { desc = it }, Modifier.fillMaxWidth(), minLines = 2,
            label = { Text(stringResource(R.string.ed_what)) }, placeholder = { Text(stringResource(R.string.ed_what_hint)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        VSpace(10)
        Scale(stringResource(R.string.ed_hunger), hungerLabels, hunger) { hunger = it }
        VSpace(10)
        Scale(stringResource(R.string.ed_full), fullLabels, full) { full = it }
        VSpace(10)
        Text(stringResource(R.string.ed_feel), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        VSpace(4)
        FeelChips(vm.content.feelings, feel) { feel = it }
        VSpace(12)
        PrimaryButton(stringResource(R.string.food_record), onClick = {
            vm.logMeal(desc.ifBlank { meal }, meal, time, hunger, full, feel)
            toast(ctx.getString(R.string.ed_saved))
            desc = ""; hunger = null; full = null; feel = null; time = vm.nowHM(); meal = vm.logic.defaultMeal(s)
        }, modifier = Modifier.fillMaxWidth(), color = Noc.good)
    }
    if (pickTime) TimePickDialog(runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.now()), { pickTime = false }) { time = it.format(Fmt.time) }
}

@Composable
fun EdEntryRow(x: FoodEntry, onEdit: () -> Unit) {
    ClickRow(onClick = onEdit) {
        Text(x.t, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f)) {
            Text(x.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            val h = x.hunger?.let { stringResource(R.string.ed_hunger_short, it) }
            val f = x.full?.let { stringResource(R.string.ed_full_short, it) }
            val parts = listOfNotNull(h, f, x.feel)
            if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EdEditDialog(vm: FoodViewModel, e: FoodEntry, onClose: () -> Unit) {
    var desc by remember(e.id) { mutableStateOf(e.name) }
    var hunger by remember(e.id) { mutableStateOf(e.hunger) }
    var full by remember(e.id) { mutableStateOf(e.full) }
    var feel by remember(e.id) { mutableStateOf(e.feel) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(e.meal) },
        text = {
            Column {
                OutlinedTextField(desc, { desc = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.ed_what)) })
                VSpace(8)
                Scale(stringResource(R.string.ed_hunger), hungerLabels, hunger) { hunger = it }
                VSpace(8)
                Scale(stringResource(R.string.ed_full), fullLabels, full) { full = it }
                VSpace(8)
                FeelChips(vm.content.feelings, feel) { feel = it }
            }
        },
        confirmButton = { TextButton(onClick = { vm.updateEntry(e.copy(name = desc.ifBlank { e.meal }, hunger = hunger, full = full, feel = feel)); onClose() }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = { vm.deleteEntry(e); onClose() }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
    )
}

/** Segítség, ha nehéz: telefonszám, szakember. */
@Composable
fun EdSupportCard() {
    val ctx = LocalContext.current
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Favorite, null, tint = Noc.coral)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.ed_support_title), style = MaterialTheme.typography.titleMedium)
        }
        VSpace(6)
        Text(stringResource(R.string.ed_support_text), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
        VSpace(10)
        TonalButton(stringResource(R.string.ed_support_call), onClick = {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:116123")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }, leading = Icons.Outlined.Call, tint = Noc.coral)
    }
}

// ---------------- Cukorbetegség csomag ----------------

private val bgCtx = listOf("ehgyomor" to R.string.bg_fasting, "elotte" to R.string.bg_before, "utana" to R.string.bg_after, "lefekves" to R.string.bg_bed)

fun bgColor(v: Double, low: Double, high: Double): Color = when {
    v < low -> StatusColors.bad
    v > high -> StatusColors.warn
    else -> StatusColors.ok
}

/** Szénhidrát-keret és vércukornapló. */
@Composable
fun DiabCard(vm: FoodViewModel, day: FoodDay) {
    val s by vm.state.collectAsState()
    val p = s.profile
    val ch = day.items.sumOf { it.c }
    var add by remember { mutableStateOf(false) }
    AppCard(glow = Noc.sapphire) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Opacity, null, tint = Noc.sapphire)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.diab_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                if (p.carbGoal > 0) "${fmt(ch)} / ${fmt(p.carbGoal)} g" else "${fmt(ch)} g",
                style = MaterialTheme.typography.titleSmall, color = if (p.carbGoal > 0 && ch > p.carbGoal) StatusColors.warn else Noc.text,
            )
        }
        if (p.carbGoal > 0) { VSpace(8); Meter((ch / p.carbGoal).toFloat(), if (ch > p.carbGoal) StatusColors.warn else Noc.sapphire) }
        VSpace(12)
        Text(stringResource(R.string.diab_bg), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        if (day.bg.isEmpty()) Text(stringResource(R.string.diab_bg_empty), style = MaterialTheme.typography.bodySmall, color = Noc.text3)
        day.bg.forEach { b ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text(b.t, style = MaterialTheme.typography.labelLarge, color = Noc.text2, modifier = Modifier.width(48.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(bgColor(b.v, p.bgLow, p.bgHigh)))
                Spacer(Modifier.width(8.dp))
                Text("${fmt(b.v, 1)} mmol/l", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(bgCtx.firstOrNull { it.first == b.ctx }?.let { stringResource(it.second) } ?: "", style = MaterialTheme.typography.bodySmall, color = Noc.text2, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.deleteBg(b.id) }) { Icon(Icons.Outlined.Close, stringResource(R.string.delete), Modifier.size(18.dp), tint = Noc.text3) }
            }
        }
        VSpace(8)
        TonalButton(stringResource(R.string.diab_bg_add), onClick = { add = true }, tint = Noc.sapphire)
        VSpace(4)
        Text(stringResource(R.string.diab_note, fmt(p.bgLow, 1), fmt(p.bgHigh, 1)), style = MaterialTheme.typography.bodySmall, color = Noc.text3)
    }
    if (add) {
        var v by remember { mutableStateOf("") }
        var ctxSel by remember { mutableStateOf(if (LocalTime.now().hour < 9) "ehgyomor" else "elotte") }
        var time by remember { mutableStateOf(vm.nowHM()) }
        AlertDialog(
            onDismissRequest = { add = false },
            title = { Text(stringResource(R.string.diab_bg_add)) },
            text = {
                Column {
                    OutlinedTextField(v, { v = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("mmol/l") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    VSpace(8)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        bgCtx.forEach { (id, l) ->
                            val on = id == ctxSel
                            Text(
                                stringResource(l), style = MaterialTheme.typography.labelLarge, color = if (on) Noc.abyss else Noc.text2,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Noc.sapphire else Noc.card)
                                    .clickable { ctxSel = id }.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                    VSpace(8)
                    OutlinedTextField(time, { time = it }, Modifier.width(120.dp), singleLine = true, label = { Text(stringResource(R.string.f_time)) })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    v.replace(',', '.').toDoubleOrNull()?.let { vm.addBg(it, ctxSel, time) }
                    add = false
                }, enabled = (v.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { add = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
