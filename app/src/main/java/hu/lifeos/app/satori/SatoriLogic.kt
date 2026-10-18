package hu.lifeos.app.satori

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

/** A régi Satori szabályainak hű átirata. Minden függvény tiszta: nem ír semmit, csak számol. */
object SatoriLogic {

    fun today(): String = LocalDate.now().toString()
    fun nowTime(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    fun stageIndex(c: SatoriContent, streak: Int): Int {
        var idx = 0
        c.stages.forEachIndexed { i, s -> if (streak >= s.days) idx = i }
        return idx
    }

    fun nextStageIndex(c: SatoriContent, streak: Int): Int {
        c.stages.forEachIndexed { i, s -> if (s.days > streak) return i }
        return c.stages.lastIndex
    }

    fun progressFraction(c: SatoriContent, streak: Int): Float {
        val s = c.stages[stageIndex(c, streak)]
        val n = c.stages[nextStageIndex(c, streak)]
        if (n.days == s.days) return 1f
        return ((streak - s.days).toFloat() / (n.days - s.days)).coerceIn(0f, 1f)
    }

    fun isRestarter(c: SatoriContent, st: SatoriState, idx: Int) = idx == 0 && st.cur().restarts > 0

    fun stageTitle(c: SatoriContent, st: SatoriState, idx: Int, restarterTitle: String): String {
        if (isRestarter(c, st, idx)) return restarterTitle
        return c.theme(st.theme).stageTitles.getOrNull(idx) ?: c.stages[idx].title
    }

    fun stageSub(c: SatoriContent, st: SatoriState, idx: Int, restarterSub: String): String {
        if (isRestarter(c, st, idx)) return restarterSub
        return c.theme(st.theme).stageSubs.getOrNull(idx) ?: c.stages[idx].sub
    }

    fun stageMotto(c: SatoriContent, st: SatoriState, idx: Int) =
        c.theme(st.theme).stageMottos.getOrNull(idx) ?: c.stages[idx].motto

    fun stageReward(c: SatoriContent, st: SatoriState, idx: Int) =
        c.theme(st.theme).stageRewards.getOrNull(idx) ?: c.stages[idx].reward

    /** Az év napja alapján választ, így egy napon belül mindig ugyanaz marad. */
    fun dayIndex(len: Int): Int = if (len <= 0) 0 else (LocalDate.now().dayOfYear) % len

    fun themeQuotes(c: SatoriContent, st: SatoriState): List<String> =
        c.theme(st.theme).quotes.ifEmpty { c.quotes }

    fun graceUsedThisWeek(d: TypeData, today: LocalDate = LocalDate.now()): Int {
        val cutoff = today.minusDays(6)
        return d.graceDates.count { runCatching { !LocalDate.parse(it).isBefore(cutoff) }.getOrDefault(false) }
    }

    /** A "Nap lezárása" logikája. Visszaadja az új állapotot és a ténylegesen érvényes eredményt. */
    fun applyDayOutcome(st: SatoriState, outcome: String, note: String): Pair<SatoriState, String> {
        val today = today()
        var d = st.cur()
        if (d.streakSnapshotDate != today) d = d.copy(streakSnapshot = d.streak, streakSnapshotDate = today)
        var grace = d.graceDates.filter { it != today }
        d = d.copy(graceDates = grace)

        var effective = outcome
        if (st.mode == "strict" && outcome == "megingas") effective = "sikertelen"
        if (st.mode == "balanced" && effective == "sikertelen" && d.weeklyAllowance > graceUsedThisWeek(d)) {
            effective = "grace"
            grace = grace + today
            d = d.copy(graceDates = grace)
        }
        val newStreak = when {
            effective == "siker" || (effective == "megingas" && st.mode == "gentle") -> d.streakSnapshot + 1
            effective == "megingas" || effective == "grace" -> d.streakSnapshot
            effective == "sikertelen" -> 0
            else -> d.streak
        }
        d = d.copy(streak = newStreak, dayOutcome = effective, dayOutcomeDate = today)

        // Napzárás-bejegyzés a naplóba (napi egy, felülírható).
        val journalState = outcomeJournalState(st, effective)
        val entry = JournalEntry(date = today, time = nowTime(), state = journalState, note = note, source = "dayclose")
        val idx = d.journal.indexOfFirst { it.date == today && it.source == "dayclose" }
        d = d.copy(journal = if (idx >= 0) d.journal.toMutableList().also { it[idx] = entry } else listOf(entry) + d.journal)

        return st.withCur { d } to effective
    }

    fun outcomeJournalState(st: SatoriState, outcome: String): String = when {
        st.mode == "gentle" && outcome == "megingas" -> "nyugodt"
        outcome == "siker" -> "nyugodt"
        outcome == "megingas" || outcome == "grace" -> "kisertes"
        else -> "megbotlas"
    }

    fun outcomeLabel(c: SatoriContent, st: SatoriState, outcome: String): String {
        val m = c.mode(st.mode)
        if (m.id == "gentle" && outcome == "megingas") return m.labels["megingas"] ?: ""
        if (m.id != "balanced" && (outcome == "siker" || outcome == "sikertelen")) m.labels[outcome]?.let { if (it.isNotBlank()) return it }
        return c.outcomeMeta[outcome]?.label ?: outcome
    }

    fun restart(st: SatoriState): SatoriState = st.withCur {
        it.copy(
            streak = 0, streakSnapshot = 0, streakSnapshotDate = null, dayOutcome = null, dayOutcomeDate = null,
            graceDates = emptyList(), pledgeDate = null, restarts = it.restarts + 1, lastRestartDate = today(),
        )
    }

    fun setMode(st: SatoriState, id: String): SatoriState {
        var data = st.data
        if (id == "balanced") data = data.mapValues { (_, v) -> if (v.weeklyAllowance == 0) v.copy(weeklyAllowance = 1) else v }
        return st.copy(mode = id, data = data)
    }

    // ---------- Mintázatok ----------

    fun daypartOf(time: String): String? {
        val h = time.substringBefore(':').toIntOrNull() ?: return null
        return when (h) {
            in 5..10 -> "reggel"
            in 11..16 -> "delutan"
            in 17..21 -> "este"
            else -> "ejszaka"
        }
    }

    data class Risk(val part: String, val count: Int, val share: Int)

    fun topRiskyDaypart(d: TypeData): Risk? {
        val flagged = d.journal.filter { it.state == "kisertes" || it.state == "kuzdelem" || it.state == "megbotlas" }
        if (flagged.size < 3) return null
        val counts = flagged.mapNotNull { daypartOf(it.time) }.groupingBy { it }.eachCount()
        val top = counts.maxByOrNull { it.value } ?: return null
        if (top.value < 2) return null
        return Risk(top.key, top.value, Math.round(top.value * 100f / flagged.size))
    }

    /** Az utolsó 14 nap napzárásai (régebbitől az újabbig), null = nincs bejegyzés. */
    fun last14(d: TypeData): List<Pair<LocalDate, String?>> {
        val byDate = d.journal.filter { it.source == "dayclose" }.associate { it.date to it.state }
        val t = LocalDate.now()
        return (13 downTo 0).map { i -> val dt = t.minusDays(i.toLong()); dt to byDate[dt.toString()] }
    }

    data class BestWeek(val year: Int, val week: Int, val count: Int)

    fun bestWeek(d: TypeData): BestWeek? {
        val ok = d.journal.filter { it.source == "dayclose" && it.state == "nyugodt" }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.distinct()
        val counts = ok.groupingBy { it.get(IsoFields.WEEK_BASED_YEAR) to it.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) }.eachCount()
        val top = counts.maxByOrNull { it.value } ?: return null
        if (top.value < 3) return null
        return BestWeek(top.key.first, top.key.second, top.value)
    }

    fun bodyStage(c: SatoriContent, type: String, days: Int): BodyStage {
        val cfg = c.body(type)
        return cfg.stages.firstOrNull { days >= it.minDay && (it.maxDay == null || days <= it.maxDay) } ?: cfg.stages.last()
    }

    // ---------- SOS hullám ----------
    const val URGE_WAVE_SECONDS = 18 * 60
    private const val URGE_PEAK_FRACTION = 12f / 18f

    fun urgeIntensity(elapsedSec: Long): Int {
        val t = (elapsedSec.toFloat() / URGE_WAVE_SECONDS).coerceAtMost(1f)
        if (t < URGE_PEAK_FRACTION) return Math.round(t / URGE_PEAK_FRACTION * 100)
        val decay = (t - URGE_PEAK_FRACTION) / (1 - URGE_PEAK_FRACTION)
        return maxOf(5, Math.round(100 * Math.exp(-decay * 2.2).toFloat()))
    }

    /** 0 = emelkedik, 1 = tetőzik, 2 = csillapodik, 3 = elengedve */
    fun urgePhase(elapsedSec: Long, pct: Int): Int = when {
        elapsedSec < URGE_WAVE_SECONDS * URGE_PEAK_FRACTION -> 0
        pct > 55 -> 1
        pct > 20 -> 2
        else -> 3
    }
}
