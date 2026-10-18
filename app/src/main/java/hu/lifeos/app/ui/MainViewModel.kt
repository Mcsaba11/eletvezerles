package hu.lifeos.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.lifeos.app.AppContainer
import hu.lifeos.app.capture.CaptureType
import hu.lifeos.app.capture.Parsed
import hu.lifeos.app.data.AppSettings
import hu.lifeos.app.data.EntryEntity
import hu.lifeos.app.data.EntryKind
import hu.lifeos.app.data.EventEntity
import hu.lifeos.app.data.MoodEntity
import hu.lifeos.app.data.PhoneCalendarInfo
import hu.lifeos.app.data.PhoneEvent
import hu.lifeos.app.data.TodoEntity
import hu.lifeos.app.notify.Reminders
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Egységes naptárelem: saját vagy a telefon naptárából. */
data class AgendaItem(
    val key: String,
    val title: String,
    val start: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String,
    val color: Int?,
    val own: EventEntity?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val c: AppContainer) : ViewModel() {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    val settings: StateFlow<AppSettings?> = c.settings.flow
        .map<AppSettings, AppSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** Az app előtérbe kerülésekor hívjuk: ha éjfél elmúlt, minden nézet az új napra vált. */
    fun refreshDay() {
        val now = LocalDate.now()
        if (now != _today.value) _today.value = now
        refreshPhone()
    }

    val todayMood: StateFlow<MoodEntity?> = _today.flatMapLatest { c.db.moods().observe(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val moodHistory = c.db.moods().observeRecent(60).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val todos: StateFlow<List<TodoEntity>> = c.db.todos().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentEntries: StateFlow<List<EntryEntity>> = c.db.entries().observeRecent(400)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ---------- Naptár ----------
    private val _month = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val month: StateFlow<LocalDate> = _month.asStateFlow()
    private val _selectedDay = MutableStateFlow(LocalDate.now())
    val selectedDay: StateFlow<LocalDate> = _selectedDay.asStateFlow()

    fun selectDay(d: LocalDate) {
        _selectedDay.value = d
        if (d.withDayOfMonth(1) != _month.value) _month.value = d.withDayOfMonth(1)
    }

    fun shiftMonth(delta: Long) {
        _month.value = _month.value.plusMonths(delta)
        refreshPhone()
    }

    private fun startOf(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()

    /** A látható hónap (+ szélek) saját eseményei. */
    private val ownEventsForMonth = _month.flatMapLatest { m ->
        val from = startOf(m.minusDays(7)); val to = startOf(m.plusMonths(1).plusDays(7))
        c.db.events().observeRange(from, to)
    }

    private val ownEventsToday = _today.flatMapLatest { d -> c.db.events().observeRange(startOf(d), startOf(d.plusDays(1))) }

    private val _phoneEvents = MutableStateFlow<List<PhoneEvent>>(emptyList())
    private val _phoneToday = MutableStateFlow<List<PhoneEvent>>(emptyList())
    private val _phoneCalendars = MutableStateFlow<List<PhoneCalendarInfo>>(emptyList())
    val phoneCalendars: StateFlow<List<PhoneCalendarInfo>> = _phoneCalendars.asStateFlow()

    fun refreshPhone() {
        viewModelScope.launch {
            val s = c.settings.flow.first()
            if (!s.showPhoneCalendar || !c.phoneCalendar.canRead()) {
                _phoneEvents.value = emptyList(); _phoneToday.value = emptyList()
                _phoneCalendars.value = if (c.phoneCalendar.canRead()) c.phoneCalendar.calendars() else emptyList()
                return@launch
            }
            _phoneCalendars.value = c.phoneCalendar.calendars()
            val m = _month.value
            _phoneEvents.value = c.phoneCalendar.events(startOf(m.minusDays(7)), startOf(m.plusMonths(1).plusDays(7)), s.hiddenCalendarIds)
            val d = _today.value
            _phoneToday.value = c.phoneCalendar.events(startOf(d), startOf(d.plusDays(1)), s.hiddenCalendarIds)
        }
    }

    private fun merge(own: List<EventEntity>, phone: List<PhoneEvent>): List<AgendaItem> =
        (own.map { AgendaItem("o${it.id}", it.title, it.startMillis, it.endMillis, it.allDay, it.location, null, it) } +
            phone.map { AgendaItem("p${it.eventId}_${it.begin}", it.title, it.begin, it.end, it.allDay, it.location, it.color, null) })
            .sortedWith(compareBy({ !it.allDay }, { it.start }))

    val monthAgenda: StateFlow<List<AgendaItem>> = combine(ownEventsForMonth, _phoneEvents) { o, p -> merge(o, p) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val todayAgenda: StateFlow<List<AgendaItem>> = combine(ownEventsToday, _phoneToday) { o, p -> merge(o, p) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun itemsOn(list: List<AgendaItem>, d: LocalDate): List<AgendaItem> {
        val s = startOf(d); val e = startOf(d.plusDays(1))
        return list.filter { it.start < e && (it.end > s || (it.end == it.start && it.start >= s)) }
    }

    // ---------- Műveletek ----------

    fun setMood(mood: Int?, energy: Int?) = viewModelScope.launch {
        val d = _today.value.toString()
        val cur = c.db.moods().get(d)
        c.db.moods().upsert(
            MoodEntity(d, mood = mood ?: cur?.mood, energy = energy ?: cur?.energy)
        )
    }

    fun addTodo(title: String, due: LocalDate?, time: LocalTime?, important: Boolean = false) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        c.db.todos().insert(TodoEntity(title = title.trim(), dueDate = due?.toString(), dueTime = time?.toString(), important = important))
    }

    fun updateTodo(t: TodoEntity) = viewModelScope.launch { c.db.todos().update(t) }

    fun toggleTodo(t: TodoEntity) = viewModelScope.launch {
        c.db.todos().update(t.copy(done = !t.done, doneAt = if (!t.done) System.currentTimeMillis() else null))
    }

    fun deleteTodo(t: TodoEntity) = viewModelScope.launch { c.db.todos().delete(t) }
    fun clearDoneTodos() = viewModelScope.launch { c.db.todos().clearDone() }

    /** Esemény mentése: a saját naptárba, vagy ha calendarId meg van adva, a telefon naptárába. */
    fun saveEvent(
        existing: EventEntity?, title: String, date: LocalDate, start: LocalTime?, end: LocalTime?,
        location: String, note: String, phoneCalendarId: Long?, onDone: (Boolean) -> Unit = {},
    ) = viewModelScope.launch {
        val allDay = start == null
        val s = if (allDay) startOf(date) else date.atTime(start).atZone(zone).toInstant().toEpochMilli()
        val e = when {
            allDay -> startOf(date.plusDays(1))
            end != null && end.isAfter(start) -> date.atTime(end).atZone(zone).toInstant().toEpochMilli()
            else -> s + 60 * 60 * 1000
        }
        if (existing == null && phoneCalendarId != null) {
            val ok = c.phoneCalendar.insert(phoneCalendarId, title.trim(), s, e, allDay, location, note)
            refreshPhone(); onDone(ok); return@launch
        }
        val ent = EventEntity(
            id = existing?.id ?: 0, title = title.trim(), startMillis = s, endMillis = e, allDay = allDay,
            location = location, note = note, createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        if (existing == null) c.db.events().insert(ent) else c.db.events().update(ent)
        onDone(true)
    }

    suspend fun loadEvent(id: Long): EventEntity? = c.db.events().get(id)

    fun deleteEvent(e: EventEntity) = viewModelScope.launch { c.db.events().delete(e) }

    fun addEntry(kind: String, text: String, amount: Double? = null) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        c.db.entries().insert(EntryEntity(ts = now, date = LocalDate.now().toString(), kind = kind, text = text.trim(), amount = amount))
    }

    fun deleteEntry(e: EntryEntity) = viewModelScope.launch { c.db.entries().delete(e) }

    /** A gyorsbeviteli mező mentése a (felhasználó által esetleg módosított) típus szerint. */
    fun saveCapture(p: Parsed, onDone: (CaptureType) -> Unit) = viewModelScope.launch {
        when (p.type) {
            CaptureType.EVENT -> saveEvent(null, p.title, p.date ?: LocalDate.now(), p.time, null, "", "",
                settings.value?.defaultWriteCalendarId?.takeIf { settings.value?.showPhoneCalendar == true })
            CaptureType.TODO -> addTodo(p.title, p.date, p.time)
            CaptureType.EXPENSE -> addEntry(EntryKind.EXPENSE, p.title, p.amount)
            CaptureType.INCOME -> addEntry(EntryKind.INCOME, p.title, p.amount)
            CaptureType.MOOD -> setMood(p.mood, null)
            CaptureType.NOTE -> addEntry(EntryKind.NOTE, p.title)
        }
        onDone(p.type)
    }

    fun updateSettings(fn: (AppSettings) -> AppSettings) = viewModelScope.launch {
        c.settings.update(fn)
        refreshPhone()
        Reminders.schedule(c.appContext, c.settings.flow.first())
    }

    init { refreshPhone() }
}
