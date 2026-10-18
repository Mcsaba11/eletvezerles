package hu.lifeos.app.capture

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class CaptureType { EVENT, TODO, EXPENSE, INCOME, MOOD, NOTE }

data class Parsed(
    val type: CaptureType,
    val title: String,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val amount: Double? = null,
    val mood: Int? = null,
)

/**
 * Szabályalapú, internet nélkül működő értelmező a gyorsbeviteli mezőhöz (magyar nyelv).
 * Pl. „holnap 10-kor fogorvos” → esemény, „tankolás 18 000 Ft” → kiadás,
 * „meg kell hívni Pétert” → teendő, „hangulat 4” → hangulat.
 * Később az AI-társ ezt felülírhatja, de enélkül is használható marad.
 */
object QuickParser {

    private val months = listOf(
        listOf("január", "jan"), listOf("február", "feb"), listOf("március", "márc", "marc", "mar"),
        listOf("április", "ápr", "apr"), listOf("május", "máj", "maj"), listOf("június", "jún", "jun"),
        listOf("július", "júl", "jul"), listOf("augusztus", "aug"), listOf("szeptember", "szept", "szep"),
        listOf("október", "okt"), listOf("november", "nov"), listOf("december", "dec"),
    )

    private val weekdays = listOf(
        DayOfWeek.MONDAY to "hétfő", DayOfWeek.TUESDAY to "kedd", DayOfWeek.WEDNESDAY to "szerd",
        DayOfWeek.THURSDAY to "csütörtök", DayOfWeek.FRIDAY to "péntek", DayOfWeek.SATURDAY to "szombat",
        DayOfWeek.SUNDAY to "vasárnap",
    )

    private val todoWords = listOf(
        "teendő", "todo", "feladat", "meg kell", "el kell", "kell ", "ne felejtsem", "vegyek", "venni", "hívjam",
        "felhívni", "intézni", "elintézni", "befizetni", "kifizetni", "megcsinálni", "megírni", "elküldeni",
    )
    private val eventWords = listOf(
        "szülinap", "születésnap", "névnap", "évforduló", "ünnep", "szabadság", "nyaralás", "buli", "esküvő",
        "koncert", "meccs", "orvos", "fogorvos", "karácsony", "húsvét", "szilveszter", "utazás", "repülő", "vonat", "találkozó", "megbeszélés", "meeting", "edzés", "vacsora", "ebéd",
    )
    private val incomeWords = listOf("bevétel", "fizetés jött", "kaptam", "jóváírás", "fizetésem")

    fun parse(input: String, today: LocalDate = LocalDate.now()): Parsed {
        val raw = input.trim()
        if (raw.isEmpty()) return Parsed(CaptureType.NOTE, "")
        val lower = raw.lowercase()
        val used = BooleanArray(raw.length)
        fun consume(r: IntRange) { for (i in r) if (i in used.indices) used[i] = true }

        // Hangulat: „hangulat 4”, „hangulatom 4/5”
        Regex("""hangulat\w*\s*:?\s*([1-5])(\s*/\s*5)?""").find(lower)?.let { m ->
            consume(m.range)
            return Parsed(CaptureType.MOOD, cleanup(raw, used), mood = m.groupValues[1].toInt())
        }

        // Összeg
        var amount: Double? = null
        Regex("""(\d{1,3}(?:[ . ]\d{3})+|\d+)(?:[,.](\d{1,2}))?\s*(ft|huf|forint|,-|eur|€)""").find(lower)?.let { m ->
            val whole = m.groupValues[1].replace(Regex("""[ . ]"""), "")
            val frac = m.groupValues[2]
            amount = (whole + if (frac.isNotEmpty()) ".$frac" else "").toDoubleOrNull()
            consume(m.range)
        }

        // Dátum
        var date: LocalDate? = null
        fun setDate(d: LocalDate, r: IntRange) { if (date == null) { date = d; consume(r) } }

        Regex("""\b(\d{4})[.\-/](\d{1,2})[.\-/](\d{1,2})\.?(-?(á|é)n)?""").find(lower)?.let { m ->
            runCatching { LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }
                .getOrNull()?.let { setDate(it, m.range) }
        }
        if (date == null) {
            for ((i, names) in months.withIndex()) {
                val alt = names.joinToString("|") { Regex.escape(it) }
                val m = Regex("""(?<![\p{L}])($alt)\.?\s*(\d{1,2})\.?(-?(á|é)n|-?(e|é|a|á)|-ig)?(?![\d])""").find(lower) ?: continue
                val day = m.groupValues[2].toInt()
                runCatching { LocalDate.of(today.year, i + 1, day) }.getOrNull()?.let {
                    setDate(if (it.isBefore(today)) it.plusYears(1) else it, m.range)
                }
                if (date != null) break
            }
        }
        if (date == null) {
            Regex("""(?<![\d:])(\d{1,2})\.(\d{1,2})\.?(-?(á|é)n)?(?![\d:])""").find(lower)?.let { m ->
                val mo = m.groupValues[1].toInt(); val d = m.groupValues[2].toInt()
                if (mo in 1..12) runCatching { LocalDate.of(today.year, mo, d) }.getOrNull()?.let {
                    setDate(if (it.isBefore(today)) it.plusYears(1) else it, m.range)
                }
            }
        }
        if (date == null) {
            listOf("holnapután" to 2L, "holnap" to 1L, "ma" to 0L, "tegnap" to -1L).forEach { (w, add) ->
                if (date != null) return@forEach
                val m = Regex("""(?<![\p{L}])$w(?![\p{L}])""").find(lower)
                if (m != null) setDate(today.plusDays(add), m.range)
            }
        }
        if (date == null) {
            Regex("""(\d{1,2})\s*nap\s*múlva""").find(lower)?.let { m -> setDate(today.plusDays(m.groupValues[1].toLong()), m.range) }
        }
        if (date == null) {
            for ((dow, stem) in weekdays) {
                val m = Regex("""(?<![\p{L}])(jövő\s+)?$stem\p{L}*""").find(lower) ?: continue
                var d = today
                while (d.dayOfWeek != dow) d = d.plusDays(1)
                if (m.groupValues[1].isNotBlank() && d == today) d = d.plusWeeks(1)
                setDate(d, m.range); break
            }
        }

        // Időpont
        var time: LocalTime? = null
        var pm = false
        Regex("""(?<![\p{L}])(délután|du\.?|este|éjjel)(?![\p{L}])""").find(lower)?.let { m ->
            if (m.groupValues[1] != "éjjel") pm = true
            consume(m.range)
        }
        Regex("""(?<![\p{L}])(reggel|délelőtt|de\.?)(?![\p{L}])""").find(lower)?.let { consume(it.range) }
        val tm = Regex("""(?<![\d.])(\d{1,2}):(\d{2})(\s*-?(kor|órakor|ra|re|ig))?""").find(lower)
            ?: Regex("""(?<![\d.:])(\d{1,2})\s*(?:h|óra|órakor|-kor|kor|-ra|-re|-ig)(?:\s*(\d{1,2})\s*(?:perc|p)(?:kor)?)?(?![\p{L}])""").find(lower)
        if (tm != null) {
            var h = tm.groupValues[1].toInt()
            val mi = tm.groupValues[2].toIntOrNull() ?: 0
            if (pm && h in 1..11) h += 12
            if (h in 0..23 && mi in 0..59) { time = LocalTime.of(h, mi); consume(tm.range) }
        }

        val title = cleanup(raw, used)
        val l = " $lower "
        val type = when {
            amount != null && incomeWords.any { lower.contains(it) } -> CaptureType.INCOME
            amount != null -> CaptureType.EXPENSE
            todoWords.any { l.contains(it) } -> CaptureType.TODO
            time != null -> CaptureType.EVENT
            date != null && eventWords.any { lower.contains(it) } -> CaptureType.EVENT
            date != null -> CaptureType.TODO
            else -> CaptureType.NOTE
        }
        val finalTitle = if (type == CaptureType.TODO) stripTodoPrefix(title) else title
        return Parsed(type, finalTitle, date, time, amount)
    }

    private fun stripTodoPrefix(t: String): String {
        val m = Regex("""^(teendő|todo|feladat)\s*:?\s*""", RegexOption.IGNORE_CASE).find(t)
        return capitalize(if (m != null) t.substring(m.range.last + 1) else t)
    }

    private fun cleanup(raw: String, used: BooleanArray): String {
        val sb = StringBuilder()
        raw.forEachIndexed { i, ch -> sb.append(if (used[i]) ' ' else ch) }
        val s = sb.toString().replace(Regex("""\s+"""), " ").trim().trim(',', '-', ':', '.', ' ')
        return capitalize(s)
    }

    private fun capitalize(s: String) = s.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
