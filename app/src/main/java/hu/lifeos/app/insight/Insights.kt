package hu.lifeos.app.insight

import hu.lifeos.app.data.EntryEntity
import hu.lifeos.app.data.EntryKind
import hu.lifeos.app.data.MoodEntity
import hu.lifeos.app.data.TodoEntity
import hu.lifeos.app.food.FoodLogic
import hu.lifeos.app.food.FoodState
import hu.lifeos.app.satori.SatoriContent
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriState
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.sqrt

/**
 * A Sensei offline „agya”: valós adatokból, egyszerű statisztikával számol meglátásokat.
 * Nem talál ki semmit; ha kevés az adat, kevesebbet mond.
 */
object Insights {

    data class Fast(
        val eating: Boolean, val minutesIn: Int, val minutesLeft: Int, val fastLength: Int,
        val opens: String, val closes: String,
    )

    fun fast(food: FoodState, logic: FoodLogic, now: LocalTime = LocalTime.now()): Fast {
        val p = food.profile
        val m = now.hour * 60 + now.minute
        val s = logic.toMin(p.winStart); val e = logic.toMin(p.winEnd)
        val eating = logic.inWindow(p, m)
        val fastLen = ((s - e) + 1440) % 1440
        return if (eating) Fast(true, ((m - s) + 1440) % 1440, ((e - m) + 1440) % 1440, fastLen, p.winStart, p.winEnd)
        else Fast(false, ((m - e) + 1440) % 1440, ((s - m) + 1440) % 1440, fastLen, p.winStart, p.winEnd)
    }

    data class Pattern(val title: String, val value: String, val text: String, val strength: Double)

    /** Pearson-korreláció; null, ha kevés a páros adat. */
    fun corr(xs: List<Double>, ys: List<Double>): Double? {
        if (xs.size < 5 || xs.size != ys.size) return null
        val mx = xs.average(); val my = ys.average()
        var num = 0.0; var dx = 0.0; var dy = 0.0
        for (i in xs.indices) { val a = xs[i] - mx; val b = ys[i] - my; num += a * b; dx += a * a; dy += b * b }
        if (dx == 0.0 || dy == 0.0) return null
        return num / sqrt(dx * dy)
    }

    data class DaySeries(val date: LocalDate, val mood: Double?, val gl: Double?, val kcal: Double?, val cravings: Int, val spend: Double)

    fun series(days: Int, moods: List<MoodEntity>, food: FoodState, logic: FoodLogic, satori: SatoriState, entries: List<EntryEntity>): List<DaySeries> {
        val moodBy = moods.associateBy { it.date }
        val cravBy = satori.cravingLog.groupingBy { it.date }.eachCount()
        val journalHard = satori.data.values.flatMap { it.journal }.filter { it.state == "kisertes" || it.state == "kuzdelem" || it.state == "megbotlas" }.groupingBy { it.date }.eachCount()
        val spendBy = entries.filter { it.kind == EntryKind.EXPENSE }.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.amount ?: 0.0 } }
        val t = LocalDate.now()
        return (days - 1 downTo 0).map { i ->
            val d = t.minusDays(i.toLong()); val k = d.toString()
            val items = food.day(k).items
            val tot = logic.totals(items)
            DaySeries(
                d, moodBy[k]?.mood?.toDouble(),
                if (items.isEmpty()) null else tot.gl, if (items.isEmpty()) null else tot.kcal,
                (cravBy[k] ?: 0) + (journalHard[k] ?: 0), spendBy[k] ?: 0.0,
            )
        }
    }

    /** Észrevett mintázatok, a legerősebb elöl. A szövegek magyarok (a tartalom része). */
    fun patterns(ser: List<DaySeries>, satori: SatoriState, content: SatoriContent): List<Pattern> {
        val out = mutableListOf<Pattern>()
        // Hangulat és glikémiás terhelés
        val mg = ser.filter { it.mood != null && it.gl != null }
        corr(mg.map { it.gl!! }, mg.map { it.mood!! })?.let { r ->
            val low = mg.filter { it.gl!! < 80 }.mapNotNull { it.mood }.average()
            val high = mg.filter { it.gl!! >= 100 }.mapNotNull { it.mood }.average()
            if (!low.isNaN() && !high.isNaN() && kotlin.math.abs(low - high) >= 0.3) {
                out += Pattern("Hangulat és vércukor", "${fmt1(low)} vs ${fmt1(high)}",
                    "Az alacsony glikémiás terhelésű napokon átlagosan ${fmt1(low)}, a magasakon ${fmt1(high)} a hangulatod (1–5 skálán).", kotlin.math.abs(r))
            }
        }
        // Sóvárgás és hangulat
        val mc = ser.filter { it.mood != null }
        corr(mc.map { it.cravings.toDouble() }, mc.map { it.mood!! })?.let { r ->
            if (r < -0.25) out += Pattern("Sóvárgás és hangulat", "r = ${fmt2(r)}",
                "A nehezebb, sóvárgással teli napjaidon rosszabb a hangulatod. Ilyenkor érdemes előre betervezni egy sétát vagy a légzést.", kotlin.math.abs(r))
        }
        // Napszak
        SatoriLogic.topRiskyDaypart(satori.cur())?.let { risk ->
            out += Pattern("Kockázatos napszak", "${risk.share}%",
                "A nehezebb pillanataid ${risk.share}%-a ${content.daypartLabel[risk.part] ?: risk.part} jelentkezik. Erre az időszakra készíts egy Ha… akkor… tervet.", risk.share / 100.0)
        }
        // Költés és hangulat
        val ms = ser.filter { it.mood != null && it.spend > 0 }
        if (ms.size >= 5) {
            val bad = ms.filter { it.mood!! <= 2 }.map { it.spend }.average()
            val good = ms.filter { it.mood!! >= 4 }.map { it.spend }.average()
            if (!bad.isNaN() && !good.isNaN() && bad > good * 1.2) {
                out += Pattern("Költés és hangulat", "+${((bad / good - 1) * 100).toInt()}%",
                    "Rosszabb hangulatú napokon többet költesz, mint a jókon. Egy nap várakozás nagyobb vásárlás előtt sokat segíthet.", 0.5)
            }
        }
        return out.sortedByDescending { it.strength }
    }

    /** A Sensei mai meglátása: 2–3 mondat a valós adatokból. */
    fun dailyInsight(
        food: FoodState, logic: FoodLogic, satori: SatoriState, content: SatoriContent,
        mood: MoodEntity?, todos: List<TodoEntity>, nextEvent: String?, now: LocalTime = LocalTime.now(),
    ): String {
        val parts = mutableListOf<String>()
        val f = fast(food, logic, now)
        parts += if (f.eating) "Az étkezési ablakod ${f.closes}-kor zárul, még ${f.minutesLeft / 60} ó ${f.minutesLeft % 60} p van hátra."
        else "A mai étkezési ablakod ${f.opens}-kor nyílik, addig víz, fekete kávé vagy tea."
        val d = satori.cur()
        val risk = SatoriLogic.topRiskyDaypart(d)
        val nowPart = SatoriLogic.daypartOf("%02d:%02d".format(now.hour, now.minute))
        if (risk != null && risk.part == nowPart) parts += "Ez a napszak szokott neked a legnehezebb lenni: tartsd kéznél az SOS-t és a légzést."
        else if (d.streak > 0) parts += "${d.streak}. napja tartod a(z) ${content.type(satori.type).label} utat."
        when {
            mood?.mood != null && mood.mood <= 2 -> parts += "Ma nehezebb napod van, legyél kedves magadhoz: egy rövid séta vagy pár perc légzés is számít."
            nextEvent != null -> parts += "Következő programod: $nextEvent."
        }
        val today = LocalDate.now().toString()
        val overdue = todos.count { !it.done && it.dueDate != null && it.dueDate < today }
        if (overdue > 0) parts += "$overdue lejárt teendőd vár, válassz ki egyet, és zárd le ma."
        return parts.take(3).joinToString(" ")
    }

    /** A „Mederben” állapot: egyszerű összkép a mai napról. */
    fun inFlow(satori: SatoriState, mood: MoodEntity?): Boolean {
        val d = satori.cur()
        val failed = d.dayOutcomeDate == SatoriLogic.today() && d.dayOutcome == "sikertelen"
        val low = (mood?.mood ?: 3) <= 2
        return !failed && !low
    }

    private fun fmt1(v: Double) = String.format(java.util.Locale("hu", "HU"), "%.1f", v)
    private fun fmt2(v: Double) = String.format(java.util.Locale("hu", "HU"), "%.2f", v)
}
