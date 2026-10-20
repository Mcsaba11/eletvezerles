package hu.lifeos.app.mods

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(10)
fun todayKey(): String = LocalDate.now().toString()

// ======================= Pénz =======================

/** A beépített kategóriáknál a név üres: a felület a nyelvi fájlból tölti (cat_<id>). */
@Serializable data class MoneyCat(val id: String, val name: String, val icon: String, val income: Boolean = false)

/** Rendszeres tétel (pl. lakbér, előfizetés); paid: azok a hónapok (yyyy-MM), amikor már rögzítve lett. */
@Serializable data class Recurring(
    val id: String = newId(), val name: String, val amount: Double, val day: Int = 1,
    val cat: String = "rezsi", val income: Boolean = false, val paid: List<String> = emptyList(),
)

@Serializable data class SavingGoal(val id: String = newId(), val name: String, val target: Double, val saved: Double = 0.0, val deadline: String? = null)

/**
 * A pénzmozgások maguk a közös bejegyzés-táblában vannak (a gyorsbevitel is oda ír);
 * itt csak a kiegészítő adatok: kategóriák, a bejegyzések kategóriája, keretek, rendszeres tételek, célok.
 */
@Serializable data class MoneyDoc(
    val cats: List<MoneyCat> = DEFAULT_CATS,
    val catOf: Map<String, String> = emptyMap(),
    val budgets: Map<String, Double> = emptyMap(),
    val recurring: List<Recurring> = emptyList(),
    val goals: List<SavingGoal> = emptyList(),
)

val DEFAULT_CATS = listOf(
    MoneyCat("elelmiszer", "", "shopping_cart"),
    MoneyCat("etterem", "", "coffee"),
    MoneyCat("kozlekedes", "", "car"),
    MoneyCat("rezsi", "", "home"),
    MoneyCat("egeszseg", "", "favorite"),
    MoneyCat("szorakozas", "", "sports_esports"),
    MoneyCat("ruha", "", "checkroom"),
    MoneyCat("ajandek", "", "gift"),
    MoneyCat("egyeb", "", "more"),
    MoneyCat("fizetes", "", "work", income = true),
    MoneyCat("egyeb_be", "", "savings", income = true),
)

/** Kulcsszavas kategória-tipp a bejegyzés szövegéből (pl. „Tesco 8 500” → Élelmiszer). */
object MoneyGuess {
    private val words = mapOf(
        "elelmiszer" to listOf("tesco", "lidl", "aldi", "spar", "penny", "auchan", "coop", "cba", "bolt", "élelmiszer", "bevásárl", "kenyér", "pékség", "piac", "zöldség"),
        "etterem" to listOf("étterem", "kávé", "kave", "büfé", "pizza", "burger", "wolt", "foodora", "ebéd", "vacsora", "cukrászd", "fagyi", "kocsma", "sör"),
        "kozlekedes" to listOf("tank", "benzin", "dízel", "mol", "omv", "shell", "parkol", "bkv", "bérlet", "vonat", "máv", "busz", "taxi", "bolt taxi", "uber", "autópálya", "matrica", "szerviz", "gumi"),
        "rezsi" to listOf("rezsi", "villany", "gáz", "víz", "internet", "telefon", "albérlet", "lakbér", "közös költség", "netflix", "spotify", "hbo", "előfizet"),
        "egeszseg" to listOf("gyógyszer", "patika", "orvos", "fogorvos", "vitamin", "labor", "edzőterem", "konditerem"),
        "szorakozas" to listOf("mozi", "koncert", "színház", "játék", "steam", "könyv", "jegy", "fesztivál"),
        "ruha" to listOf("ruha", "cipő", "póló", "nadrág", "kabát", "h&m", "zara", "deichmann"),
        "ajandek" to listOf("ajándék", "szülinap", "virág"),
        "fizetes" to listOf("fizetés", "munkabér", "bér"),
    )

    fun guess(text: String, income: Boolean): String {
        val t = text.lowercase()
        val hit = words.entries.firstOrNull { (_, ws) -> ws.any { t.contains(it) } }?.key
        return when {
            income -> if (hit == "fizetes") "fizetes" else "egyeb_be"
            hit != null && hit != "fizetes" -> hit
            else -> "egyeb"
        }
    }
}

// ======================= Alvás =======================

/** Egy éjszaka; date = az ébredés napja. */
@Serializable data class SleepRec(
    val date: String, val bed: String = "23:00", val wake: String = "07:00",
    val quality: Int? = null, val note: String = "",
) {
    /** Alvásidő percben (éjfélen átnyúlva is). */
    fun minutes(): Int {
        val b = runCatching { LocalTime.parse(bed) }.getOrDefault(LocalTime.of(23, 0))
        val w = runCatching { LocalTime.parse(wake) }.getOrDefault(LocalTime.of(7, 0))
        val bm = b.hour * 60 + b.minute; val wm = w.hour * 60 + w.minute
        return ((wm - bm) + 1440) % 1440
    }
}

@Serializable data class SleepDoc(val nights: List<SleepRec> = emptyList(), val targetHours: Double = 8.0, val bedGoal: String = "23:00")

// ======================= Mozgás =======================

@Serializable data class MoveRec(
    val id: String = newId(), val date: String = todayKey(), val t: String = "",
    val type: String = "walk", val minutes: Int = 30, val intensity: Int = 2, val km: Double? = null, val note: String = "",
) {
    /** Mérsékelt percekre átszámítva (az intenzív perc duplán számít, a WHO-ajánlás szerint). */
    fun moderateMinutes(): Int = when (intensity) { 3 -> minutes * 2; 1 -> minutes / 2; else -> minutes }
}

@Serializable data class MoveDoc(val items: List<MoveRec> = emptyList(), val weeklyGoal: Int = 150)

val MOVE_TYPES = listOf(
    "walk" to "directions_walk", "run" to "directions_run", "bike" to "bike", "gym" to "fitness_center",
    "yoga" to "self_improvement", "swim" to "pool", "sport" to "sports", "other" to "bolt",
)

// ======================= Kapcsolatok =======================

@Serializable data class Contact(val date: String, val kind: String = "call", val note: String = "")

@Serializable data class Person(
    val id: String = newId(), val name: String, val relation: String = "friend",
    /** Hány naponta szeretnél jelentkezni; 0 = nincs cél. */
    val every: Int = 14,
    val phone: String = "", val birthday: String = "", val note: String = "",
    val contacts: List<Contact> = emptyList(),
) {
    fun last(): String? = contacts.maxOfOrNull { it.date }
    /** Hány nap múlva esedékes (negatív = késésben); null, ha nincs cél. */
    fun dueIn(today: LocalDate = LocalDate.now()): Int? {
        if (every <= 0) return null
        val l = last()?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return 0
        return every - ChronoUnit.DAYS.between(l, today).toInt()
    }
    /** Hány nap múlva van a születésnapja (MM-dd); null, ha nincs megadva. */
    fun birthdayIn(today: LocalDate = LocalDate.now()): Int? {
        val p = birthday.split("-").mapNotNull { it.toIntOrNull() }
        if (p.size < 2) return null
        val (m, d) = p.takeLast(2)
        var next = runCatching { LocalDate.of(today.year, m, d) }.getOrNull() ?: return null
        if (next.isBefore(today)) next = next.plusYears(1)
        return ChronoUnit.DAYS.between(today, next).toInt()
    }
}

@Serializable data class PeopleDoc(val people: List<Person> = emptyList())

// ======================= Célok =======================

@Serializable data class GoalStep(val id: String = newId(), val text: String, val done: Boolean = false)

@Serializable data class Goal(
    val id: String = newId(), val title: String, val area: Int = 0, val why: String = "",
    val deadline: String? = null, val steps: List<GoalStep> = emptyList(),
    val done: Boolean = false, val created: String = todayKey(), val doneAt: String? = null,
) {
    fun progress(): Float = if (done) 1f else if (steps.isEmpty()) 0f else steps.count { it.done }.toFloat() / steps.size
}

@Serializable data class GoalsDoc(val goals: List<Goal> = emptyList())

// ======================= Tanulás =======================

@Serializable data class LearnItem(
    val id: String = newId(), val title: String, val kind: String = "book",
    val total: Int = 0, val progress: Int = 0, val notes: List<String> = emptyList(), val done: Boolean = false,
)

@Serializable data class LearnSession(val id: String = newId(), val date: String = todayKey(), val item: String = "", val minutes: Int = 20, val units: Int = 0)

@Serializable data class LearnDoc(val items: List<LearnItem> = emptyList(), val sessions: List<LearnSession> = emptyList(), val dailyGoal: Int = 20)

val LEARN_KINDS = listOf("book" to "menu_book", "course" to "school", "language" to "translate", "skill" to "build")

// ======================= Otthon =======================

@Serializable data class Chore(val id: String = newId(), val name: String, val every: Int = 7, val last: String? = null, val room: String = "") {
    fun dueIn(today: LocalDate = LocalDate.now()): Int {
        val l = last?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return 0
        return every - ChronoUnit.DAYS.between(l, today).toInt()
    }
}

@Serializable data class ShopItem(val id: String = newId(), val text: String, val done: Boolean = false)

@Serializable data class HouseDoc(val chores: List<Chore> = emptyList(), val shopping: List<ShopItem> = emptyList())

// ======================= Saját követők (Modulépítő) =======================

/** kind: check (igen/nem), count (számláló), number (szám mértékegységgel), scale (1–5). */
@Serializable data class Tracker(
    val id: String = newId(), val name: String, val icon: String = "star", val kind: String = "check",
    val unit: String = "", val goal: Double = 1.0, val color: Int = 0, val onHome: Boolean = true,
)

@Serializable data class TrackersDoc(
    val trackers: List<Tracker> = emptyList(),
    /** követő-azonosító → nap → érték */
    val logs: Map<String, Map<String, Double>> = emptyMap(),
) {
    fun value(id: String, date: String): Double = logs[id]?.get(date) ?: 0.0
    fun set(id: String, date: String, v: Double): TrackersDoc {
        val m = logs[id].orEmpty().toMutableMap()
        if (v == 0.0) m.remove(date) else m[date] = v
        return copy(logs = logs + (id to m))
    }
    /** Egymást követő napok, amikor a cél teljesült (a mai nap nem töri meg, ha még nincs kész). */
    fun streak(t: Tracker, today: LocalDate = LocalDate.now()): Int {
        fun hit(d: LocalDate) = value(t.id, d.toString()).let { v -> if (t.kind == "scale") v > 0 else v >= t.goal }
        var d = if (hit(today)) today else today.minusDays(1)
        var n = 0
        while (hit(d) && n < 3650) { n++; d = d.minusDays(1) }
        return n
    }
}
