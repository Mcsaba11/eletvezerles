package hu.lifeos.app.satori

import kotlinx.serialization.Serializable

/*
 * A Satori állapota. A mezőnevek szándékosan megegyeznek a régi webes Satori mentésével
 * (satori-v1), így a régi mentés közvetlenül betölthető.
 */

@Serializable
data class JournalEntry(
    val date: String,
    val time: String = "",
    val state: String = "nyugodt",
    val note: String = "",
    val source: String = "journal",
)

@Serializable
data class TypeData(
    val streak: Int = 0,
    val reasons: List<String> = emptyList(),
    val dailyCost: Double = 500.0,
    val goal: Double = 500000.0,
    val journal: List<JournalEntry> = emptyList(),
    val pledgeDate: String? = null,
    val dayOutcome: String? = null,
    val dayOutcomeDate: String? = null,
    val streakSnapshot: Int = 0,
    val streakSnapshotDate: String? = null,
    val weeklyAllowance: Int = 0,
    val graceDates: List<String> = emptyList(),
    val ifThenPlan: String = "",
    val restarts: Int = 0,
    val lastRestartDate: String? = null,
)

@Serializable
data class Ally(val name: String = "", val phone: String = "")

@Serializable
data class SatoriReminders(val enabled: Boolean = false, val time: String = "20:00", val lastNotifiedDate: String? = null)

@Serializable
data class MoodRec(val date: String, val mood: Int? = null, val energy: Int? = null)

@Serializable
data class CravingRec(val date: String, val type: String = "", val trigger: String = "", val ts: Long = 0)

@Serializable
data class SatoriState(
    val type: String = "cukor",
    val theme: String = "zen",
    val mode: String = "balanced",
    val onboardingDone: Boolean? = null,
    val data: Map<String, TypeData> = emptyMap(),
    val ally: Ally = Ally(),
    val reminders: SatoriReminders = SatoriReminders(),
    val moodLog: List<MoodRec> = emptyList(),
    val cravingLog: List<CravingRec> = emptyList(),
    val merleg: Map<String, List<String>> = emptyMap(),
    /** A felhasználó által létrehozott saját szokások. */
    val custom: List<TypeDef> = emptyList(),
    /** A követett szokások sorrendben; null = régi mentés, ilyenkor az eddig használt szokások. */
    val followed: List<String>? = null,
) {
    /** A követett szokások azonosítói (régi mentésnél az aktivitással rendelkezők + az aktuális). */
    fun followedIds(): List<String> = followed ?: (data.filter { (_, td) -> td.streak > 0 || td.journal.isNotEmpty() || td.pledgeDate != null }.keys.toList() + type).distinct()

    fun cur(): TypeData = data[type] ?: TypeData()

    /** Kiegészíti a hiányzó kategóriákat, ahogy a régi app loadState() függvénye is tette. */
    fun normalized(typeIds: List<String>): SatoriState {
        val filled = (typeIds + custom.map { it.id }).associateWith { id ->
            data[id] ?: TypeData(weeklyAllowance = if (id == "cukor") 1 else 0)
        } + data.filterKeys { it !in typeIds }
        val done = onboardingDone ?: (typeIds + custom.map { it.id }).any { id ->
            val td = filled.getValue(id); td.streak > 0 || td.journal.isNotEmpty() || td.pledgeDate != null
        }
        return copy(data = filled, onboardingDone = done)
    }

    fun withCur(fn: (TypeData) -> TypeData): SatoriState = copy(data = data + (type to fn(cur())))
}
