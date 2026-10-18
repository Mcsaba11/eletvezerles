package hu.lifeos.app.food

import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Az IR Napló számításainak hű átirata (tápérték, célok, böjtablak, szabad szöveges értelmezés). */
class FoodLogic(val content: FoodContent) {

    private val base: List<Food> = content.foods.map { it.copy(src = "base") }
    private val baseById = base.associateBy { it.id }

    // ---------- Tápérték ----------

    fun nutr(f: Food, g: Double): Nutr {
        val k = g / 100.0
        return Nutr(f.kcal * k, f.p * k, f.c * k, f.f * k, f.fib * k, f.gi * f.c * k / 100.0)
    }

    fun rateFrom(t: Nutr): String {
        if (t.kcal == 0.0) return "g"
        val pd = t.p / t.kcal * 100
        val gld = t.gl / t.kcal * 100
        if (gld > 6) return "b"
        if (pd >= 6 && gld < 3.5) return "g"
        return "m"
    }

    fun findFood(s: FoodState, id: String, noRecipe: Boolean = false): Food? {
        baseById[id]?.let { return it }
        s.custom.firstOrNull { it.id == id }?.let { return it.copy(src = "custom", cat = "sajat") }
        if (noRecipe) return null
        return allFoods(s).firstOrNull { it.id == id }
    }

    data class RecipeCalc(val food: Food, val total: Nutr, val weight: Double)

    fun recipeCalc(s: FoodState, r: Recipe): RecipeCalc {
        var tot = Nutr(); var w = 0.0
        r.items.forEach { it ->
            val f = findFood(s, it.fid, true) ?: return@forEach
            tot += nutr(f, it.g); w += it.g
        }
        val ww = if (w == 0.0) 1.0 else w
        val k = 100.0 / ww
        val food = Food(
            id = "r:" + r.id, name = r.name, cat = "recept", r = rateFrom(tot),
            kcal = tot.kcal * k, p = tot.p * k, c = tot.c * k, f = tot.f * k, fib = tot.fib * k,
            gi = if (tot.c > 0) (tot.gl * 100 / tot.c).roundToInt().toDouble() else 0.0,
            u = "adag", ug = (ww / maxOf(1, r.servings)).roundToInt().toDouble(), note = "", src = "recipe",
        )
        return RecipeCalc(food, tot, ww)
    }

    private var cacheKey: Any? = null
    private var cache: List<Food> = emptyList()

    fun allFoods(s: FoodState): List<Food> {
        val key = s.custom to s.recipes
        if (key == cacheKey) return cache
        val custom = s.custom.map { it.copy(cat = "sajat", src = "custom") }
        val rec = (content.samples + s.recipes).map { recipeCalc(s, it).food }
        cache = base + custom + rec
        cacheKey = key
        return cache
    }

    // ---------- Célok ----------

    fun targets(p: Profile): Targets {
        val w = p.weight; val h = p.height; val a = p.age
        val bmr = 10 * w + 6.25 * h - 5 * a + if (p.sex == "m") 5 else -161
        val tdee = bmr * (p.act.toDoubleOrNull() ?: 1.375)
        val floor = if (p.sex == "m") 1800 else 1400
        val raw = tdee + (p.goal.toDoubleOrNull() ?: 0.0)
        val target = maxOf(raw, floor.toDouble())
        val prot = (w * 1.3).roundToInt()
        val carb = (target * .30 / 4).roundToInt()
        val fat = maxOf(0, ((target - prot * 4 - carb * 4) / 9).roundToInt())
        return Targets(bmr, tdee, target, prot, carb, fat, w / ((h / 100) * (h / 100)), raw < floor, floor, if (p.water > 0) p.water else 3500.0)
    }

    fun toMin(s: String): Int {
        val parts = s.split(":")
        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    fun inWindow(p: Profile, min: Int): Boolean {
        val s = toMin(p.winStart); val e = toMin(p.winEnd)
        return if (s <= e) min in s until e else (min >= s || min < e)
    }

    fun defaultMeal(p: Profile, now: LocalTime = LocalTime.now()): String {
        val m = now.hour * 60 + now.minute
        val meals = content.meals
        if (!inWindow(p, m)) return meals.last()
        val since = ((m - toMin(p.winStart)) + 1440) % 1440
        return when {
            since < 90 -> meals[0]
            since < 200 -> meals[1]
            since < 330 -> meals[2]
            else -> meals[3]
        }
    }

    fun totals(items: List<FoodEntry>): Nutr = items.fold(Nutr()) { a, x -> a + Nutr(x.kcal, x.p, x.c, x.f, x.fib, x.gl) }

    fun entry(f: Food, g: Double, meal: String, t: String, est: String?): FoodEntry {
        val n = nutr(f, g)
        return FoodEntry(
            id = rid(), fid = f.id, name = f.name, r = f.r, g = Math.round(g * 10) / 10.0,
            kcal = n.kcal, p = n.p, c = n.c, f = n.f, fib = n.fib, gl = n.gl, meal = meal, t = t, est = est?.ifBlank { null },
        )
    }

    fun rescale(e: FoodEntry, newG: Double): FoodEntry {
        if (e.g <= 0) return e
        val k = newG / e.g
        return e.copy(g = newG, kcal = e.kcal * k, p = e.p * k, c = e.c * k, f = e.f * k, fib = e.fib * k, gl = e.gl * k)
    }

    // ---------- Mértékegységek ----------

    data class UnitOpt(val v: String, val label: String, val name: String, val g: Double, val hint: String = "")

    fun measG(k: String, f: Food): Double {
        val m = content.meas[k] ?: return 100.0
        return m.g[f.cat] ?: m.g["default"] ?: 100.0
    }

    fun unitOpts(f: Food?): List<UnitOpt> {
        val o = mutableListOf(UnitOpt("g", "gramm", "g", 1.0))
        if (f == null) return o
        if (f.u.isNotBlank() && f.ug > 0) o += UnitOpt("u", "${f.u} (${fmt(f.ug)} g)", f.u, f.ug, "az étel szokásos adagja")
        (content.catMeas[f.cat] ?: content.catMeas["sajat"].orEmpty()).forEach { k ->
            val m = content.meas[k] ?: return@forEach
            if (f.u.isNotBlank() && norm(f.u).take(4) == norm(m.l).take(4)) return@forEach
            val g = measG(k, f)
            o += UnitOpt("m:$k", "${m.l} (≈${fmt(g)} g)", m.l, g, m.h)
        }
        return o
    }

    // ---------- Szabad szöveg ----------

    data class Chunk(
        val raw: String, val qty: Double?, val size: Double, val unitKey: String?, val ownUnit: String?,
        val grams: Double?, val cands: List<Food>,
    )

    data class Estimate(val g: Double, val txt: String)

    fun splitText(s: String): List<String> =
        s.split(Regex("""[,;\n+]|\s+(?:és|meg|plusz)\s+""", RegexOption.IGNORE_CASE)).map { it.trim() }.filter { it.isNotEmpty() }

    private fun ftoks(name: String) = norm(name).replace(Regex("[^a-z0-9 ]"), " ").split(Regex("\\s+")).filter { it.length >= 3 }

    private fun tokMatch(q: String, ft: String): Double = when {
        q == ft -> 1.1
        q.startsWith(ft) -> if (q.length - ft.length <= 3) 1.0 else .65
        ft.startsWith(q) -> 1.0
        q.length >= 5 && ft.startsWith(q.take(5)) -> .9
        ft.length >= 5 && q.startsWith(ft.take(5)) -> .9
        q.length >= 4 && ft.contains(q.take(minOf(q.length, 6))) -> .6
        else -> 0.0
    }

    fun matchFoods(s: FoodState, q: List<String>): List<Food> {
        if (q.isEmpty()) return emptyList()
        val res = mutableListOf<Pair<Food, Double>>()
        allFoods(s).forEach { f ->
            val ft = ftoks(f.name)
            if (ft.isEmpty()) return@forEach
            var qc = 0.0; var fc = 0.0
            q.forEach { w -> qc += ft.maxOf { tokMatch(w, it) } }
            ft.forEach { t -> fc += q.maxOf { tokMatch(it, t) } }
            var sc = qc / q.size * 2 + fc / ft.size
            q.forEach { w ->
                val k = content.alias.keys.firstOrNull { w.startsWith(it) && w.length - it.length <= 4 }
                if (k != null && content.alias[k] == f.id) sc += 1.2
            }
            if (f.src == "base") sc += .05
            if (qc > 0) res += f to sc
        }
        return res.sortedByDescending { it.second }.take(6).map { it.first }
    }

    private fun ownUnits(s: FoodState): List<String> =
        allFoods(s).map { norm(it.u).split(" ").last() }.filter { it.length >= 3 }.distinct()

    fun parseChunk(s: FoodState, chunk: String): Chunk {
        var t = norm(chunk).replace(Regex("""(\d),(\d)"""), "$1.$2")
        var grams: Double? = null; var qty: Double? = null; var size = 1.0
        var unitKey: String? = null; var own: String? = null
        Regex("""(\d+(?:\.\d+)?)\s*(kg|dkg|dk|gramm|gr|g|dl|ml|l|liter)\b""").find(t)?.let { m ->
            val v = m.groupValues[1].toDouble()
            val mul = mapOf("kg" to 1000.0, "dkg" to 10.0, "dk" to 10.0, "dl" to 100.0, "ml" to 1.0, "l" to 1000.0, "liter" to 1000.0)[m.groupValues[2]] ?: 1.0
            grams = v * mul; t = t.replace(m.value, " ")
        }
        Regex("""(\d+)\s*/\s*(\d+)""").find(t)?.let { m ->
            qty = m.groupValues[1].toDouble() / m.groupValues[2].toDouble().coerceAtLeast(1.0); t = t.replace(m.value, " ")
        }
        val toks = t.replace(Regex("[^a-z0-9. ]"), " ").split(Regex("\\s+")).filter { it.isNotEmpty() }
        val rest = mutableListOf<String>()
        val ou = ownUnits(s)
        val stop = content.stop.toSet()
        for (w in toks) {
            if (Regex("""^\d+(\.\d+)?$""").matches(w)) { qty = (qty ?: 1.0) * w.toDouble(); continue }
            val qw = content.qtyW.keys.firstOrNull { k -> w == k || (k.length >= 3 && w.startsWith(k) && w.length - k.length <= 2 && w != "hatalmas") }
            if (qw != null && !(qw == "hat" && w != "hat")) { qty = (qty ?: 1.0) * content.qtyW.getValue(qw); continue }
            val sw = content.sizeW.keys.firstOrNull { k -> w == k || (w.startsWith(k) && w.length - k.length <= 3 && k.length >= 4) }
            if (sw != null) { size *= content.sizeW.getValue(sw); continue }
            if (unitKey == null && own == null) {
                val uw = content.unitW.firstOrNull { (k) -> w == k || (w.startsWith(k) && (k.length >= 4 || w.length - k.length <= 4) && !(k == "tal" && w.length > 6)) }
                if (uw != null) { unitKey = uw[1]; continue }
                val ow = ou.firstOrNull { o -> w == o || (o.length >= 4 && w.startsWith(o.take(maxOf(4, o.length - 1))) && w.length - o.length <= 4) }
                if (ow != null) { own = ow; continue }
            }
            if (w in stop || w.length < 3) continue
            rest += w
        }
        return Chunk(chunk.trim(), qty, size, unitKey, own, grams, matchFoods(s, rest))
    }

    fun estimate(s: FoodState, info: Chunk, f: Food?): Estimate {
        if (f == null) return Estimate(0.0, "")
        val qty = info.qty ?: 1.0
        info.grams?.let { return Estimate(it, "") }
        val baseG: Double; val name: String
        when {
            info.ownUnit != null -> {
                if (f.u.isNotBlank() && norm(f.u).contains(info.ownUnit.take(4))) { baseG = f.ug; name = f.u }
                else {
                    val any = allFoods(s).firstOrNull { it.u.isNotBlank() && norm(it.u).split(" ").last() == info.ownUnit }
                    baseG = any?.ug ?: 100.0; name = info.ownUnit
                }
            }
            info.unitKey != null -> {
                val m = content.meas[info.unitKey]
                if (m != null && f.u.isNotBlank() && norm(f.u).take(4) == norm(m.l).take(4)) { baseG = f.ug; name = f.u }
                else { baseG = measG(info.unitKey, f); name = m?.l ?: info.unitKey }
            }
            f.u.isNotBlank() && f.ug > 0 && norm(f.u).startsWith("fel ") -> { baseG = f.ug * 2; name = f.u.drop(4) }
            f.u.isNotBlank() && f.ug > 0 -> { baseG = f.ug; name = f.u }
            else -> { baseG = 100.0; name = "adag" }
        }
        val g = qty * baseG * info.size
        val sz = if (info.size < .95) "kicsi " else if (info.size > 1.05) "nagy " else ""
        return Estimate(g, "${fmt(qty, if (qty % 1 != 0.0) 1 else 0)} $sz$name")
    }

    // ---------- Súly ----------

    fun trend(list: List<WeightRec>): List<Double> = list.map { x ->
        val t = LocalDate.parse(x.d)
        val win = list.filter { y -> val ty = LocalDate.parse(y.d); !ty.isAfter(t) && ty.isAfter(t.minusDays(7)) }
        win.sumOf { it.kg } / win.size
    }

    fun slopePerDay(list: List<WeightRec>): Double? {
        val cut = LocalDate.now().minusDays(28)
        val l = list.filter { !LocalDate.parse(it.d).isBefore(cut) }
        if (l.size < 3) return null
        val xs = l.map { ChronoUnit.DAYS.between(cut, LocalDate.parse(it.d)).toDouble() }
        val ys = l.map { it.kg }
        val mx = xs.average(); val my = ys.average()
        var num = 0.0; var den = 0.0
        xs.forEachIndexed { i, x -> num += (x - mx) * (ys[i] - my); den += (x - mx) * (x - mx) }
        return if (den != 0.0) num / den else null
    }

    // ---------- Tanácsok ----------

    data class Insight(val kind: String, val text: String)

    fun insights(s: FoodState, items: List<FoodEntry>): List<Insight> {
        val r = targets(s.profile)
        val t = totals(items)
        val out = mutableListOf<Insight>()
        if (items.isEmpty()) return listOf(Insight("", "Adj hozzá ételt, és itt látod, mennyi maradt a napi keretedből."))
        val kl = r.target - t.kcal
        out += if (kl >= 0) Insight("", "Még ${fmt(kl)} kcal fér bele a napba.") else Insight("warn", "${fmt(-kl)} kcal-lal a napi cél fölött vagy.")
        val pl = r.p - t.p
        if (pl > 5) {
            val ideas = listOf("skyr", "turosov", "tofu", "gorog", "tempeh", "cottage", "edamame", "lencse", "szejtan", "tojas")
                .filter { id -> items.none { it.fid == id } }.take(3)
                .mapNotNull { findFood(s, it)?.name?.split(",")?.first()?.lowercase() }
            out += Insight("", "Fehérjéből még ${fmt(pl)} g hiányzik. Ötlet: ${ideas.joinToString(", ")}.")
        } else out += Insight("ok", "A fehérjecél megvan.")
        if (t.gl >= 120) out += Insight("warn", "Magas a napi glikémiás terhelés (${fmt(t.gl)}). Cserélj gyors szénhidrátot hüvelyesre, zöldségre.")
        if (t.fib < 20 && t.kcal > r.target * .5) out += Insight("", "Kevés a rost (${fmt(t.fib)} g). Zöldség, hüvelyes, chia vagy lenmag segít.")
        val bad = items.filter { it.r == "b" }
        if (bad.isNotEmpty()) out += Insight("warn", "Kerülendő tétel ma: ${bad.take(3).joinToString(", ") { it.name }}${if (bad.size > 3) "…" else ""}. A Kalkulátor fülön cserelehetőséget is mutatok.")
        val outside = items.filter { !inWindow(s.profile, toMin(it.t)) && it.kcal > 5 }
        if (outside.isNotEmpty()) out += Insight("warn", "${outside.size} tétel az étkezési ablakon kívül került rögzítésre.")
        val first = items.minByOrNull { it.t }
        if (first != null && first.r == "b") out += Insight("warn", "A böjtöt kerülendő étellel törted meg. Holnap próbáld fehérjével és zöldséggel kezdeni.")
        return out
    }

    fun swapsFor(s: FoodState, f: Food): List<Food> {
        var sw = content.swaps[f.id].orEmpty().mapNotNull { findFood(s, it) }
        if (f.r != "g" && sw.isEmpty()) sw = base.filter { it.cat == f.cat && it.r == "g" }.sortedByDescending { it.p / maxOf(1.0, it.kcal) }.take(3)
        if (sw.isEmpty()) sw = base.filter { it.cat == f.cat && it.r == "g" && it.id != f.id }.take(3)
        return sw
    }

    companion object {
        fun norm(s: String): String =
            Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

        fun rid(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(8)

        fun fmt(v: Double, decimals: Int = 0): String {
            val s = String.format(java.util.Locale("hu", "HU"), "%,.${decimals}f", v)
            return s.replace(' ', ' ')
        }

        fun glTxt(gl: Double) = if (gl <= 10) "alacsony" else if (gl < 20) "közepes" else "magas"
        fun giTxt(gi: Double) = if (gi == 0.0) "nincs számottevő" else if (gi <= 55) "alacsony" else if (gi < 70) "közepes" else "magas"
    }
}
