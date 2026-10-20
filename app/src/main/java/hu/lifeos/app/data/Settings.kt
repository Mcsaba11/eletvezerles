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
)

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
        }
    }
}
