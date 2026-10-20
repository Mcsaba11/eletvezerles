package hu.lifeos.app.mods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.lifeos.app.AppContainer
import hu.lifeos.app.data.EntryEntity
import hu.lifeos.app.data.EntryKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/** A Pénz, Alvás, Mozgás, Kapcsolatok, Célok, Tanulás, Otthon és Saját követők modulok közös nézetmodellje. */
@OptIn(ExperimentalCoroutinesApi::class)
class ModsViewModel(private val c: AppContainer) : ViewModel() {
    val stores = c.mods
    val money = c.mods.money
    val sleep = c.mods.sleep
    val move = c.mods.move
    val people = c.mods.people
    val goals = c.mods.goals
    val learn = c.mods.learn
    val house = c.mods.house
    val trackers = c.mods.trackers

    // ---------- Pénz ----------
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month
    fun shiftMonth(d: Long) { _month.value = _month.value.plusMonths(d).let { if (it.isAfter(YearMonth.now())) YearMonth.now() else it } }

    /** A kiválasztott hónap pénzmozgásai. */
    val monthTx: StateFlow<List<EntryEntity>> = _month.flatMapLatest { m ->
        c.db.entries().observeMoney(m.atDay(1).toString(), m.atEndOfMonth().toString())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Az elmúlt 6 hónap pénzmozgásai a trendhez. */
    val halfYearTx: StateFlow<List<EntryEntity>> = c.db.entries()
        .observeMoney(YearMonth.now().minusMonths(5).atDay(1).toString(), LocalDate.now().toString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun catOf(e: EntryEntity, doc: MoneyDoc): String =
        doc.catOf[e.id.toString()] ?: MoneyGuess.guess(e.text, e.kind == EntryKind.INCOME)

    fun addTx(income: Boolean, amount: Double, text: String, cat: String, date: LocalDate = LocalDate.now()) = viewModelScope.launch {
        if (amount <= 0) return@launch
        val zone = ZoneId.systemDefault()
        val ts = if (date == LocalDate.now()) System.currentTimeMillis() else date.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
        val id = c.db.entries().insert(EntryEntity(ts = ts, date = date.toString(), kind = if (income) EntryKind.INCOME else EntryKind.EXPENSE, text = text.trim(), amount = amount, app = "money"))
        money.update { it.copy(catOf = it.catOf + (id.toString() to cat)) }
    }

    fun updateTx(e: EntryEntity, cat: String) = viewModelScope.launch {
        c.db.entries().update(e)
        money.update { it.copy(catOf = it.catOf + (e.id.toString() to cat)) }
    }

    fun deleteTx(e: EntryEntity) = viewModelScope.launch {
        c.db.entries().delete(e)
        money.update { it.copy(catOf = it.catOf - e.id.toString()) }
    }

    /** Rendszeres tétel rögzítése az adott hónapra. */
    fun payRecurring(r: Recurring, ym: YearMonth = YearMonth.now()) {
        val day = r.day.coerceIn(1, ym.lengthOfMonth())
        val date = ym.atDay(day).let { if (it.isAfter(LocalDate.now())) LocalDate.now() else it }
        addTx(r.income, r.amount, r.name, r.cat, date)
        money.update { d -> d.copy(recurring = d.recurring.map { if (it.id == r.id) it.copy(paid = (it.paid + ym.toString()).distinct().takeLast(24)) else it }) }
    }
}
