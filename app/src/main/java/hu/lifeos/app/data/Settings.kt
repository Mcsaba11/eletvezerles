package hu.lifeos.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val lockEnabled: Boolean = false,
    val showPhoneCalendar: Boolean = false,
    /** Vesszővel elválasztott naptár-azonosítók; üres = mind. */
    val hiddenCalendarIds: Set<Long> = emptySet(),
    /** A telefon naptára, ahová az új eseményt menti (null = a saját naptár). */
    val defaultWriteCalendarId: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "20:00",
    /** "system", "dark" vagy "light". */
    val darkMode: String = "dark",
    /** Havi költési keret forintban; 0 = nincs beállítva. */
    val monthlyBudget: Double = 0.0,
    /** Az a nap, amikor a felhasználó a Sensei mai meglátását „Rendben”-nel lezárta. */
    val insightDismissed: String = "",
    val name: String = "",
    val welcomeDone: Boolean = false,
    /** Bekapcsolt modulok (Modules); null = régi telepítés, ilyenkor minden be van kapcsolva. */
    val modules: List<String>? = null,
    /** A kezdőlap kártyáinak sorrendje (HomeCards); üres = alapsorrend. */
    val homeOrder: List<String> = emptyList(),
    /** Elrejtett kezdőlap-kártyák. */
    val homeHidden: Set<String> = emptySet(),
) {
    fun moduleOn(id: String): Boolean = (modules ?: Modules.LEGACY).contains(id)

    /** A bekapcsolt modulok listája (régi telepítésnél az akkor létező modulok). */
    fun moduleList(): List<String> = modules ?: Modules.LEGACY
}

/** A bekapcsolható modulok azonosítói. */
object Modules {
    const val FOOD = "food"
    const val SATORI = "satori"
    const val MIND = "mind"
    const val MONEY = "money"
    const val TASKS = "tasks"
    const val WHEEL = "wheel"
    const val SLEEP = "sleep"
    const val MOVE = "move"
    const val PEOPLE = "people"
    const val GOALS = "goals"
    const val LEARN = "learn"
    const val HOUSE = "house"
    const val TRACKERS = "trackers"
    val ALL = listOf(FOOD, SATORI, MIND, MONEY, TASKS, SLEEP, MOVE, PEOPLE, GOALS, LEARN, HOUSE, TRACKERS, WHEEL)
    /** A modulrendszer előtti telepítéseken ezek voltak bekapcsolva. */
    val LEGACY = listOf(FOOD, SATORI, MIND, MONEY, TASKS, WHEEL)
}

/** A kezdőlap kártyái és a modul, amelyhez tartoznak (null = mindig elérhető). */
object HomeCards {
    val ALL: List<Pair<String, String?>> = listOf(
        "capture" to null, "insight" to null, "food" to Modules.FOOD, "satori" to Modules.SATORI,
        "breath" to Modules.MIND, "mood" to Modules.MIND, "money" to Modules.MONEY,
        "focus" to Modules.TASKS, "trackers" to Modules.TRACKERS, "sleep" to Modules.SLEEP, "move" to Modules.MOVE,
        "goals" to Modules.GOALS, "people" to Modules.PEOPLE, "learn" to Modules.LEARN, "house" to Modules.HOUSE,
        "wheel" to Modules.WHEEL, "quote" to null,
    )
    fun module(id: String): String? = ALL.firstOrNull { it.first == id }?.second

    /** A felhasználó sorrendje, kiegészítve az új kártyákkal. */
    fun ordered(s: AppSettings): List<String> = (s.homeOrder.filter { id -> ALL.any { it.first == id } } + ALL.map { it.first }).distinct()

    /** A megjelenítendő kártyák sorrendben. */
    fun visible(s: AppSettings): List<String> = ordered(s).filter { id -> id !in s.homeHidden && (module(id)?.let(s::moduleOn) ?: true) }
}

class SettingsStore(private val context: Context) {
    private object K {
        val lock = booleanPreferencesKey("lock")
        val phoneCal = booleanPreferencesKey("phone_cal")
        val hiddenCals = stringPreferencesKey("hidden_cals")
        val writeCal = stringPreferencesKey("write_cal")
        val reminder = booleanPreferencesKey("reminder")
        val reminderTime = stringPreferencesKey("reminder_time")
        val dark = stringPreferencesKey("dark")
        val name = stringPreferencesKey("name")
        val welcome = booleanPreferencesKey("welcome")
        val budget = stringPreferencesKey("budget")
        val insight = stringPreferencesKey("insight")
        val modules = stringPreferencesKey("modules")
        val homeOrder = stringPreferencesKey("home_order")
        val homeHidden = stringPreferencesKey("home_hidden")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p -> read(p) }

    private fun read(p: Preferences) = AppSettings(
        lockEnabled = p[K.lock] ?: false,
        showPhoneCalendar = p[K.phoneCal] ?: false,
        hiddenCalendarIds = (p[K.hiddenCals] ?: "").split(',').mapNotNull { it.trim().toLongOrNull() }.toSet(),
        defaultWriteCalendarId = p[K.writeCal]?.toLongOrNull(),
        reminderEnabled = p[K.reminder] ?: false,
        reminderTime = p[K.reminderTime] ?: "20:00",
        darkMode = p[K.dark] ?: "dark",
        monthlyBudget = p[K.budget]?.toDoubleOrNull() ?: 0.0,
        insightDismissed = p[K.insight] ?: "",
        name = p[K.name] ?: "",
        welcomeDone = p[K.welcome] ?: false,
        modules = p[K.modules]?.split(',')?.filter { it.isNotBlank() },
        homeOrder = p[K.homeOrder]?.split(',')?.filter { it.isNotBlank() }.orEmpty(),
        homeHidden = p[K.homeHidden]?.split(',')?.filter { it.isNotBlank() }?.toSet().orEmpty(),
    )

    suspend fun update(fn: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val n = fn(read(p))
            p[K.lock] = n.lockEnabled
            p[K.phoneCal] = n.showPhoneCalendar
            p[K.hiddenCals] = n.hiddenCalendarIds.joinToString(",")
            if (n.defaultWriteCalendarId == null) p.remove(K.writeCal) else p[K.writeCal] = n.defaultWriteCalendarId.toString()
            p[K.reminder] = n.reminderEnabled
            p[K.reminderTime] = n.reminderTime
            p[K.dark] = n.darkMode
            p[K.name] = n.name
            p[K.welcome] = n.welcomeDone
            p[K.budget] = n.monthlyBudget.toString()
            p[K.insight] = n.insightDismissed
            if (n.modules == null) p.remove(K.modules) else p[K.modules] = n.modules.joinToString(",")
            p[K.homeOrder] = n.homeOrder.joinToString(",")
            p[K.homeHidden] = n.homeHidden.joinToString(",")
        }
    }
}
