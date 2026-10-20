package hu.lifeos.app.mods

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/*
 * Mini-appok: a felhasználó építőkockákból rak össze saját appot (pl. leszokás, rutin, edzésnapló).
 * Az app leírása (MiniApp) és az adatai (BlockData) külön vannak, így a leírás megosztható sablonként.
 */

object BlockTypes {
    const val TEXT = "text"
    const val STREAK = "streak"
    const val TRACKER = "tracker"
    const val CHECKLIST = "checklist"
    const val JOURNAL = "journal"
    const val SAVINGS = "savings"
    const val SOS = "sos"
    const val TIMER = "timer"
    const val CHART = "chart"
    const val REMINDER = "reminder"
    val ALL = listOf(STREAK, TRACKER, CHECKLIST, JOURNAL, SAVINGS, SOS, TIMER, CHART, TEXT, REMINDER)
}

@Serializable data class BlockCfg(
    /** Tiszta napok: szintek napokban és (opcionálisan) a nevük. */
    val milestones: List<Int> = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365),
    val levelNames: List<String> = emptyList(),
    /** Követő: check, count, number, scale. */
    val kind: String = "check",
    val unit: String = "",
    val goal: Double = 1.0,
    /** Megtakarítás: napi összeg. */
    val perDay: Double = 0.0,
    /** SOS-tippek, ellenőrzőlista elemei, naplócímkék. */
    val items: List<String> = emptyList(),
    /** Időzítő percben. */
    val minutes: Int = 10,
    /** Emlékeztető időpontja és szövege; szövegkocka tartalma. */
    val time: String = "20:00",
    val text: String = "",
    /** Grafikon: forráskocka és napok száma. */
    val source: String = "",
    val days: Int = 14,
)

@Serializable data class Block(val id: String = newId(), val type: String, val title: String = "", val cfg: BlockCfg = BlockCfg())

@Serializable data class MiniApp(
    val id: String = newId(), val name: String, val icon: String = "star", val color: Int = 0,
    val blocks: List<Block> = emptyList(), val onHome: Boolean = true, val created: String = todayKey(),
)

@Serializable data class JEntry(val id: String = newId(), val date: String = todayKey(), val time: String = "", val text: String = "", val mood: Int? = null, val tag: String? = null)

@Serializable data class BlockData(
    /** Tiszta napok kezdete (yyyy-MM-dd); null = még nem indult. */
    val start: String? = null,
    val best: Int = 0,
    val resets: List<String> = emptyList(),
    /** Követő: nap → érték. */
    val values: Map<String, Double> = emptyMap(),
    /** Ellenőrzőlista: nap → kipipált elemek sorszáma. */
    val checks: Map<String, List<Int>> = emptyMap(),
    val entries: List<JEntry> = emptyList(),
    /** Időzítő: nap → perc. */
    val sessions: Map<String, Int> = emptyMap(),
)

@Serializable data class MiniAppsDoc(val apps: List<MiniApp> = emptyList(), val data: Map<String, BlockData> = emptyMap()) {
    fun data(blockId: String): BlockData = data[blockId] ?: BlockData()
    fun withData(blockId: String, fn: (BlockData) -> BlockData) = copy(data = data + (blockId to fn(data(blockId))))
    fun app(id: String): MiniApp? = apps.firstOrNull { it.id == id }
}

object MiniLogic {
    fun streakDays(d: BlockData, today: LocalDate = LocalDate.now()): Int =
        d.start?.let { runCatching { ChronoUnit.DAYS.between(LocalDate.parse(it), today).toInt().coerceAtLeast(0) }.getOrNull() } ?: 0

    /** Elért szint sorszáma (-1 = még egy sem). */
    fun level(cfg: BlockCfg, days: Int): Int = cfg.milestones.indexOfLast { days >= it }

    fun nextMilestone(cfg: BlockCfg, days: Int): Int? = cfg.milestones.firstOrNull { it > days }

    fun reset(d: BlockData, today: LocalDate = LocalDate.now()): BlockData {
        val days = streakDays(d, today)
        return d.copy(start = today.toString(), best = maxOf(d.best, days), resets = (d.resets + today.toString()).takeLast(200))
    }

    fun value(d: BlockData, date: String) = d.values[date] ?: 0.0

    fun setValue(d: BlockData, date: String, v: Double): BlockData {
        val m = d.values.toMutableMap()
        if (v <= 0.0) m.remove(date) else m[date] = v
        return d.copy(values = m)
    }

    fun hit(cfg: BlockCfg, v: Double) = if (cfg.kind == "scale") v > 0 else v >= cfg.goal

    /** Egymást követő teljesített napok egy követő vagy lista alapján (a mai nap nem töri meg). */
    fun runStreak(today: LocalDate = LocalDate.now(), ok: (String) -> Boolean): Int {
        var d = if (ok(today.toString())) today else today.minusDays(1)
        var n = 0
        while (ok(d.toString()) && n < 3650) { n++; d = d.minusDays(1) }
        return n
    }

    /** A megtakarítás napjai: az app első tiszta-napok kockája, ha van, egyébként az app létrehozása óta. */
    fun savingsDays(app: MiniApp, doc: MiniAppsDoc): Int {
        val s = app.blocks.firstOrNull { it.type == BlockTypes.STREAK }
        if (s != null) return streakDays(doc.data(s.id))
        return runCatching { ChronoUnit.DAYS.between(LocalDate.parse(app.created), LocalDate.now()).toInt() }.getOrDefault(0)
    }

    /** Egy kocka napi értéke a grafikonhoz. */
    fun seriesValue(b: Block, d: BlockData, date: String): Double = when (b.type) {
        BlockTypes.TRACKER -> value(d, date)
        BlockTypes.TIMER -> (d.sessions[date] ?: 0).toDouble()
        BlockTypes.CHECKLIST -> (d.checks[date]?.size ?: 0).toDouble()
        BlockTypes.JOURNAL -> d.entries.filter { it.date == date }.mapNotNull { it.mood }.let { if (it.isEmpty()) 0.0 else it.average() }
        BlockTypes.STREAK -> if (d.resets.contains(date)) 1.0 else 0.0
        else -> 0.0
    }

    /** Új azonosítók a sablonból létrehozott apphoz (a grafikon forrását is átírja). */
    fun instantiate(t: MiniApp): MiniApp {
        val map = t.blocks.associate { it.id to newId() }
        return t.copy(
            id = newId(), created = todayKey(),
            blocks = t.blocks.map { b -> b.copy(id = map.getValue(b.id), cfg = b.cfg.copy(source = map[b.cfg.source] ?: b.cfg.source)) },
        )
    }
}

/** Megosztható formátum: csak a leírás, adatok nélkül. */
@Serializable data class MiniAppShare(val meder_miniapp: Int = 1, val app: MiniApp)

object MiniShare {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = false; prettyPrint = false }
    fun export(app: MiniApp): String = json.encodeToString(MiniAppShare.serializer(), MiniAppShare(app = app))
    /** Beillesztett szövegből; elfogadja a puszta app-leírást is. Hibánál null. */
    fun import(text: String): MiniApp? {
        val t = text.trim().let { s -> s.substring(s.indexOf('{').coerceAtLeast(0), (s.lastIndexOf('}') + 1).coerceAtLeast(0)) }
        return runCatching { json.decodeFromString(MiniAppShare.serializer(), t).app }.getOrNull()
            ?: runCatching { json.decodeFromString(MiniApp.serializer(), t) }.getOrNull()
    }
}

/** Beépített sablonok: assets/content/<nyelv>/miniapps.json. */
@Serializable data class MiniTemplate(val desc: String = "", val app: MiniApp)

object MiniTemplates {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private var cache: List<MiniTemplate>? = null
    fun load(context: Context): List<MiniTemplate> = cache ?: run {
        val lang = Locale.getDefault().language
        val text = runCatching { context.assets.open("content/$lang/miniapps.json").bufferedReader().use { it.readText() } }
            .getOrElse { context.assets.open("content/hu/miniapps.json").bufferedReader().use { it.readText() } }
        runCatching { json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(MiniTemplate.serializer()), text) }.getOrDefault(emptyList())
            .also { cache = it }
    }
}
