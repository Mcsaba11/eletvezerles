package hu.lifeos.app.mods.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.data.EntryEntity
import hu.lifeos.app.data.EntryKind
import hu.lifeos.app.mods.MoneyCat
import hu.lifeos.app.mods.MoneyDoc
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.mods.Recurring
import hu.lifeos.app.mods.SavingGoal
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.DatePickDialog
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.KpiTile
import hu.lifeos.app.ui.Meter
import hu.lifeos.app.ui.MetricRing
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.Segmented
import hu.lifeos.app.ui.TonalButton
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun catName(c: MoneyCat): String = c.name.ifBlank {
    stringResource(
        when (c.id) {
            "elelmiszer" -> R.string.cat_elelmiszer; "etterem" -> R.string.cat_etterem; "kozlekedes" -> R.string.cat_kozlekedes
            "rezsi" -> R.string.cat_rezsi; "egeszseg" -> R.string.cat_egeszseg; "szorakozas" -> R.string.cat_szorakozas
            "ruha" -> R.string.cat_ruha; "ajandek" -> R.string.cat_ajandek; "fizetes" -> R.string.cat_fizetes
            "egyeb_be" -> R.string.cat_egyeb_be; else -> R.string.cat_egyeb
        }
    )
}

private fun MoneyDoc.cat(id: String): MoneyCat = cats.firstOrNull { it.id == id } ?: cats.first { it.id == "egyeb" }

@Composable
fun MoneyScreen(
    vm: ModsViewModel, budget: Double, onSetBudget: (Double) -> Unit, satoriSaved: Double?, onBack: () -> Unit,
) {
    val doc by vm.money.state.collectAsState()
    val tx by vm.monthTx.collectAsState()
    val half by vm.halfYearTx.collectAsState()
    val ym by vm.month.collectAsState()
    var addOpen by remember { mutableStateOf(false) }
    var editTx by remember { mutableStateOf<EntryEntity?>(null) }
    var recOpen by remember { mutableStateOf<Recurring?>(null) }
    var newRec by remember { mutableStateOf(false) }
    var goalOpen by remember { mutableStateOf<SavingGoal?>(null) }
    var newGoal by remember { mutableStateOf(false) }
    var budgetCat by remember { mutableStateOf<MoneyCat?>(null) }
    var totalBudget by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val tabs = listOf(
        stringResource(R.string.money_tab_overview), stringResource(R.string.money_tab_tx), stringResource(R.string.money_tab_budgets),
        stringResource(R.string.money_tab_recurring), stringResource(R.string.money_tab_goals),
    )
    ModScaffold(
        stringResource(R.string.mod_money), stringResource(R.string.money_kicker), Noc.sapphire, onBack, tabs,
        onAdd = { p -> when (p) { 3 -> newRec = true; 4 -> newGoal = true; else -> addOpen = true } },
        snackbar = snackbar,
    ) { page ->
        when (page) {
            0 -> Overview(vm, doc, tx, half, ym, budget, satoriSaved, onBudget = { totalBudget = true })
            1 -> TxList(vm, doc, tx, ym, onEdit = { editTx = it })
            2 -> Budgets(doc, tx, budget, onCat = { budgetCat = it }, onTotal = { totalBudget = true })
            3 -> RecurringList(vm, doc, onEdit = { recOpen = it })
            else -> GoalsList(doc, onOpen = { goalOpen = it })
        }
    }

    if (addOpen) TxSheet(vm, doc, null, onDismiss = { addOpen = false })
    editTx?.let { e -> TxSheet(vm, doc, e, onDismiss = { editTx = null }) }
    if (newRec || recOpen != null) RecurringSheet(vm, recOpen, onDismiss = { newRec = false; recOpen = null })
    if (newGoal || goalOpen != null) GoalSheet(vm, goalOpen, onDismiss = { newGoal = false; goalOpen = null })
    budgetCat?.let { c ->
        AmountSheet(stringResource(R.string.money_budget_for, catName(c)), doc.budgets[c.id] ?: 0.0, onDismiss = { budgetCat = null }) { v ->
            vm.money.update { d -> d.copy(budgets = if (v <= 0) d.budgets - c.id else d.budgets + (c.id to v)) }
            budgetCat = null
        }
    }
    if (totalBudget) AmountSheet(stringResource(R.string.set_budget), budget, onDismiss = { totalBudget = false }) { v -> onSetBudget(v); totalBudget = false }
}

@Composable
private fun MonthSwitcher(vm: ModsViewModel, ym: YearMonth) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { vm.shiftMonth(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null) }
        Text(Fmt.monthYear(ym.atDay(1)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { vm.shiftMonth(1) }, enabled = ym < YearMonth.now()) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
    }
}

@Composable
private fun Overview(
    vm: ModsViewModel, doc: MoneyDoc, tx: List<EntryEntity>, half: List<EntryEntity>, ym: YearMonth,
    budget: Double, satoriSaved: Double?, onBudget: () -> Unit,
) {
    val exp = tx.filter { it.kind == EntryKind.EXPENSE }
    val inc = tx.filter { it.kind == EntryKind.INCOME }
    val spent = exp.sumOf { it.amount ?: 0.0 }
    val earned = inc.sumOf { it.amount ?: 0.0 }
    val isNow = ym == YearMonth.now()
    val today = LocalDate.now()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MonthSwitcher(vm, ym) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiTile(stringResource(R.string.money_income), Fmt.money(earned), modifier = Modifier.weight(1f), accent = Noc.good)
                KpiTile(stringResource(R.string.money_expense), Fmt.money(spent), modifier = Modifier.weight(1f), accent = Noc.coral)
            }
        }
        item {
            AppCard(onClick = onBudget, glow = Noc.sapphire) {
                Text(stringResource(R.string.money_balance), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
                Text(Fmt.money(earned - spent), style = MaterialTheme.typography.headlineMedium, color = if (earned - spent >= 0) Noc.text else Noc.coral)
                VSpace(10)
                if (budget > 0) {
                    val frac = (spent / budget).toFloat()
                    val pace = if (isNow) today.dayOfMonth.toDouble() / ym.lengthOfMonth() else 1.0
                    Meter(frac, if (spent <= budget * pace) Noc.sapphire else Noc.amber)
                    VSpace(6)
                    Row {
                        Text(stringResource(R.string.money_of_budget, Fmt.money(spent), Fmt.money(budget)), style = MaterialTheme.typography.bodySmall, color = Noc.text2, modifier = Modifier.weight(1f))
                        if (isNow) {
                            val left = (budget - spent) / (ym.lengthOfMonth() - today.dayOfMonth + 1)
                            Text(stringResource(R.string.money_per_day, Fmt.money(maxOf(0.0, left))), style = MaterialTheme.typography.labelLarge, color = Noc.good)
                        }
                    }
                } else Text(stringResource(R.string.money_set_budget_hint), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            }
        }
        // Kategóriák
        item {
            val byCat = exp.groupBy { vm.catOf(it, doc) }.mapValues { (_, l) -> l.sumOf { it.amount ?: 0.0 } }.toList().sortedByDescending { it.second }
            AppCard {
                Text(stringResource(R.string.money_by_cat), style = MaterialTheme.typography.titleMedium)
                VSpace(8)
                if (byCat.isEmpty()) EmptyHint(stringResource(R.string.money_empty))
                byCat.forEach { (cid, sum) ->
                    val c = doc.cat(cid)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        IconBadge(iconFor(c.icon), Noc.sapphire, 30)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(catName(c), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(Fmt.money(sum), style = MaterialTheme.typography.labelLarge)
                            }
                            val lim = doc.budgets[cid]
                            Meter(((sum / (lim ?: spent.coerceAtLeast(1.0)))).toFloat(), if (lim != null && sum > lim) Noc.coral else Noc.sapphire, height = 4.dp)
                        }
                    }
                }
            }
        }
        // Trend
        item {
            val months = (5 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) }
            val vals = months.map { m -> half.filter { it.kind == EntryKind.EXPENSE && it.date.startsWith(m.toString()) }.sumOf { it.amount ?: 0.0 } }
            AppCard {
                Text(stringResource(R.string.money_trend), style = MaterialTheme.typography.titleMedium)
                VSpace(10)
                MiniBars(vals, months.map { it.monthValue.toString() + "." }, Noc.sapphire, goal = if (budget > 0) budget else null)
            }
        }
        if (satoriSaved != null && satoriSaved > 0) item {
            AppCard(glow = Noc.amber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.Savings, Noc.amber)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.money_satori_saved), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.money_satori_saved_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                    }
                    Text(Fmt.money(satoriSaved), style = MaterialTheme.typography.titleMedium, color = Noc.amber)
                }
            }
        }
    }
}

@Composable
private fun TxList(vm: ModsViewModel, doc: MoneyDoc, tx: List<EntryEntity>, ym: YearMonth, onEdit: (EntryEntity) -> Unit) {
    val byDay = tx.groupBy { it.date }.toSortedMap(compareByDescending { it })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { MonthSwitcher(vm, ym) }
        if (tx.isEmpty()) item { EmptyHint(stringResource(R.string.money_empty)) }
        byDay.forEach { (d, list) ->
            item(key = "h$d") {
                val sum = list.sumOf { (if (it.kind == EntryKind.INCOME) 1 else -1) * (it.amount ?: 0.0) }
                Row(Modifier.padding(top = 8.dp)) {
                    SectionLabel(runCatching { Fmt.dayLong(LocalDate.parse(d)) }.getOrDefault(d), Modifier.weight(1f))
                    Text(Fmt.money(sum), style = MaterialTheme.typography.labelMedium, color = Noc.text3)
                }
            }
            items(list, key = { it.id }) { e ->
                val c = doc.cat(vm.catOf(e, doc))
                val income = e.kind == EntryKind.INCOME
                AppCard(onClick = { onEdit(e) }, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(iconFor(c.icon), if (income) Noc.good else Noc.sapphire, 34)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.text.ifBlank { catName(c) }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(catName(c), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        }
                        Text((if (income) "+" else "−") + Fmt.money(e.amount ?: 0.0), style = MaterialTheme.typography.titleSmall, color = if (income) Noc.good else Noc.text)
                    }
                }
            }
        }
    }
}

@Composable
private fun Budgets(doc: MoneyDoc, tx: List<EntryEntity>, budget: Double, onCat: (MoneyCat) -> Unit, onTotal: () -> Unit) {
    val exp = tx.filter { it.kind == EntryKind.EXPENSE }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            AppCard(onClick = onTotal) {
                Text(stringResource(R.string.set_budget), style = MaterialTheme.typography.titleMedium)
                Text(if (budget > 0) Fmt.money(budget) else stringResource(R.string.money_not_set), style = MaterialTheme.typography.headlineSmall, color = Noc.sapphire)
                Text(stringResource(R.string.money_budgets_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            }
        }
        items(doc.cats.filter { !it.income }, key = { it.id }) { c ->
            val spent = exp.filter { (doc.catOf[it.id.toString()] ?: hu.lifeos.app.mods.MoneyGuess.guess(it.text, false)) == c.id }.sumOf { it.amount ?: 0.0 }
            val lim = doc.budgets[c.id]
            AppCard(onClick = { onCat(c) }, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(iconFor(c.icon), Noc.sapphire, 34)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(catName(c), style = MaterialTheme.typography.bodyLarge)
                        Text(if (lim != null) stringResource(R.string.money_of_budget, Fmt.money(spent), Fmt.money(lim)) else Fmt.money(spent),
                            style = MaterialTheme.typography.bodySmall, color = if (lim != null && spent > lim) Noc.coral else Noc.text2)
                        if (lim != null) { VSpace(4); Meter((spent / lim).toFloat(), if (spent > lim) Noc.coral else Noc.sapphire, height = 4.dp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringList(vm: ModsViewModel, doc: MoneyDoc, onEdit: (Recurring) -> Unit) {
    val ym = YearMonth.now().toString()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(stringResource(R.string.money_recurring_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
            if (doc.recurring.isNotEmpty()) {
                val monthly = doc.recurring.filter { !it.income }.sumOf { it.amount }
                Text(stringResource(R.string.money_recurring_total, Fmt.money(monthly)), style = MaterialTheme.typography.titleSmall, color = Noc.sapphire)
            }
        }
        if (doc.recurring.isEmpty()) item { EmptyHint(stringResource(R.string.money_recurring_empty)) }
        items(doc.recurring.sortedBy { it.day }, key = { it.id }) { r ->
            val paid = ym in r.paid
            AppCard(onClick = { onEdit(r) }, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(iconFor(doc.cat(r.cat).icon), if (r.income) Noc.good else Noc.sapphire, 34)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.money_every_month_day, r.day) + " · " + Fmt.money(r.amount), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                    }
                    if (paid) Icon(Icons.Outlined.CheckCircle, stringResource(R.string.money_paid), tint = Noc.good)
                    else TextButton(onClick = { vm.payRecurring(r) }) { Text(stringResource(R.string.money_record)) }
                }
            }
        }
    }
}

@Composable
private fun GoalsList(doc: MoneyDoc, onOpen: (SavingGoal) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(stringResource(R.string.money_goals_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2) }
        if (doc.goals.isEmpty()) item { EmptyHint(stringResource(R.string.money_goals_empty)) }
        items(doc.goals, key = { it.id }) { g ->
            val frac = if (g.target > 0) (g.saved / g.target).toFloat() else 0f
            AppCard(onClick = { onOpen(g) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(g.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.money_of_budget, Fmt.money(g.saved), Fmt.money(g.target)), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        g.deadline?.let { d -> Text(stringResource(R.string.money_deadline, runCatching { Fmt.dayLong(LocalDate.parse(d)) }.getOrDefault(d)), style = MaterialTheme.typography.bodySmall, color = Noc.text3) }
                    }
                    MetricRing(frac, Noc.good, Modifier.size(64.dp), stroke = 6.dp) {
                        Text("${(frac * 100).toInt().coerceAtMost(100)}%", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ---------------- Űrlapok ----------------

@Composable
private fun TxSheet(vm: ModsViewModel, doc: MoneyDoc, e: EntryEntity?, onDismiss: () -> Unit) {
    var income by remember { mutableStateOf(e?.kind == EntryKind.INCOME) }
    var amount by remember { mutableStateOf(e?.amount?.let { it.toLong().toString() } ?: "") }
    var text by remember { mutableStateOf(e?.text ?: "") }
    var cat by remember { mutableStateOf(e?.let { vm.catOf(it, doc) }) }
    var date by remember { mutableStateOf(e?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()) }
    var pick by remember { mutableStateOf(false) }
    val guessed = hu.lifeos.app.mods.MoneyGuess.guess(text, income)
    val effCat = cat ?: guessed
    val expT = stringResource(R.string.money_expense); val incT = stringResource(R.string.money_income)
    FormSheet(stringResource(if (e == null) R.string.money_add else R.string.money_edit), onDismiss) {
        Segmented(listOf(false, true), income, { if (it) incT else expT }, { income = it; cat = null }, Modifier.fillMaxWidth())
        NumIn(amount, { amount = it }, stringResource(R.string.f_amount), suffix = "Ft", decimal = true)
        TextIn(text, { text = it }, stringResource(R.string.money_what), placeholder = stringResource(R.string.money_what_hint))
        Text(stringResource(R.string.money_category), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        ChipRow(doc.cats.filter { it.income == income }, doc.cats.firstOrNull { it.id == effCat }, { catName(it) }, Noc.sapphire) { cat = it.id }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val todayT = stringResource(R.string.today); val yT = stringResource(R.string.yesterday)
            ChipRow(listOf(0, 1, 2), when (date) { LocalDate.now() -> 0; LocalDate.now().minusDays(1) -> 1; else -> 2 },
                { when (it) { 0 -> todayT; 1 -> yT; else -> if (date < LocalDate.now().minusDays(1)) Fmt.dayShort(date) else "…" } }, Noc.sapphire) {
                when (it) { 0 -> date = LocalDate.now(); 1 -> date = LocalDate.now().minusDays(1); else -> pick = true }
            }
        }
        PrimaryButton(stringResource(R.string.save), onClick = {
            val a = amount.toNum() ?: 0.0
            if (e == null) vm.addTx(income, a, text, effCat, date)
            else vm.updateTx(e.copy(amount = a, text = text.trim(), date = date.toString(), kind = if (income) EntryKind.INCOME else EntryKind.EXPENSE), effCat)
            onDismiss()
        }, enabled = (amount.toNum() ?: 0.0) > 0, modifier = Modifier.fillMaxWidth(), color = Noc.sapphire)
        if (e != null) TextButton(onClick = { vm.deleteTx(e); onDismiss() }) { Text(stringResource(R.string.delete), color = Noc.coral) }
    }
    if (pick) DatePickDialog(date, { pick = false }) { if (!it.isAfter(LocalDate.now())) date = it }
}

@Composable
private fun RecurringSheet(vm: ModsViewModel, r: Recurring?, onDismiss: () -> Unit) {
    val doc by vm.money.state.collectAsState()
    var name by remember { mutableStateOf(r?.name ?: "") }
    var amount by remember { mutableStateOf(r?.amount?.toLong()?.toString() ?: "") }
    var day by remember { mutableStateOf((r?.day ?: 1).toString()) }
    var income by remember { mutableStateOf(r?.income ?: false) }
    var cat by remember { mutableStateOf(r?.cat ?: "rezsi") }
    val expT = stringResource(R.string.money_expense); val incT = stringResource(R.string.money_income)
    FormSheet(stringResource(R.string.money_recurring_new), onDismiss) {
        Segmented(listOf(false, true), income, { if (it) incT else expT }, { income = it; cat = if (it) "fizetes" else "rezsi" }, Modifier.fillMaxWidth())
        TextIn(name, { name = it }, stringResource(R.string.habit_name), placeholder = stringResource(R.string.money_recurring_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumIn(amount, { amount = it }, stringResource(R.string.f_amount), Modifier.weight(1f), suffix = "Ft")
            NumIn(day, { day = it }, stringResource(R.string.money_day_of_month), Modifier.weight(1f))
        }
        ChipRow(doc.cats.filter { it.income == income }, doc.cats.firstOrNull { it.id == cat }, { catName(it) }, Noc.sapphire) { cat = it.id }
        PrimaryButton(stringResource(R.string.save), onClick = {
            val item = (r ?: Recurring(name = "", amount = 0.0)).copy(
                name = name.trim(), amount = amount.toNum() ?: 0.0, day = (day.toIntOrNull() ?: 1).coerceIn(1, 31), income = income, cat = cat,
            )
            vm.money.update { d -> d.copy(recurring = if (r == null) d.recurring + item else d.recurring.map { if (it.id == r.id) item else it }) }
            onDismiss()
        }, enabled = name.isNotBlank() && (amount.toNum() ?: 0.0) > 0, modifier = Modifier.fillMaxWidth(), color = Noc.sapphire)
        if (r != null) TextButton(onClick = { vm.money.update { d -> d.copy(recurring = d.recurring.filter { it.id != r.id }) }; onDismiss() }) {
            Text(stringResource(R.string.delete), color = Noc.coral)
        }
    }
}

@Composable
private fun GoalSheet(vm: ModsViewModel, g: SavingGoal?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(g?.name ?: "") }
    var target by remember { mutableStateOf(g?.target?.toLong()?.toString() ?: "") }
    var add by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf(g?.deadline?.let { runCatching { LocalDate.parse(it) }.getOrNull() }) }
    var pick by remember { mutableStateOf(false) }
    FormSheet(stringResource(if (g == null) R.string.money_goal_new else R.string.money_goal), onDismiss) {
        TextIn(name, { name = it }, stringResource(R.string.habit_name), placeholder = stringResource(R.string.money_goal_hint))
        NumIn(target, { target = it }, stringResource(R.string.money_target), suffix = "Ft")
        if (g != null) {
            Text(stringResource(R.string.money_saved_now, Fmt.money(g.saved)), style = MaterialTheme.typography.titleSmall, color = Noc.good)
            NumIn(add, { add = it }, stringResource(R.string.money_deposit), suffix = "Ft")
        }
        TonalButton(deadline?.let { stringResource(R.string.money_deadline, Fmt.dayLong(it)) } ?: stringResource(R.string.money_deadline_set), onClick = { pick = true })
        PrimaryButton(stringResource(R.string.save), onClick = {
            val item = (g ?: SavingGoal(name = "", target = 0.0)).let { it.copy(name = name.trim(), target = target.toNum() ?: 0.0, saved = it.saved + (add.toNum() ?: 0.0), deadline = deadline?.toString()) }
            vm.money.update { d -> d.copy(goals = if (g == null) d.goals + item else d.goals.map { if (it.id == g.id) item else it }) }
            onDismiss()
        }, enabled = name.isNotBlank() && (target.toNum() ?: 0.0) > 0, modifier = Modifier.fillMaxWidth(), color = Noc.good)
        if (g != null) TextButton(onClick = { vm.money.update { d -> d.copy(goals = d.goals.filter { it.id != g.id }) }; onDismiss() }) {
            Text(stringResource(R.string.delete), color = Noc.coral)
        }
    }
    if (pick) DatePickDialog(deadline ?: LocalDate.now().plusMonths(3), { pick = false }) { deadline = it }
}

@Composable
fun AmountSheet(title: String, initial: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var v by remember { mutableStateOf(if (initial > 0) initial.toLong().toString() else "") }
    FormSheet(title, onDismiss) {
        NumIn(v, { v = it }, stringResource(R.string.f_amount), suffix = "Ft")
        PrimaryButton(stringResource(R.string.save), onClick = { onSave(v.toNum() ?: 0.0) }, modifier = Modifier.fillMaxWidth(), color = Noc.sapphire)
    }
}
