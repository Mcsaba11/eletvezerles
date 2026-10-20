package hu.lifeos.app.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.lifeos.app.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class FoodViewModel(private val c: AppContainer) : ViewModel() {
    private val repo = c.food
    val content: FoodContent = repo.content
    val logic: FoodLogic = repo.logic
    val state: StateFlow<FoodState> = repo.state
    val loaded: StateFlow<Boolean> = repo.loaded

    private val _view = MutableStateFlow(LocalDate.now())
    val view: StateFlow<LocalDate> = _view.asStateFlow()
    fun setView(d: LocalDate) { _view.value = if (d.isAfter(LocalDate.now())) LocalDate.now() else d }

    /** Az Ételek fülről a Kalkulátorba küldött étel. */
    private val _calcFood = MutableStateFlow<Food?>(null)
    val calcFood: StateFlow<Food?> = _calcFood.asStateFlow()
    fun setCalcFood(f: Food?) { _calcFood.value = f }

    fun nowHM(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    fun today(): String = LocalDate.now().toString()

    fun markSetup() = repo.update { it.copy(setupDone = true) }

    // ---------- Fókuszcsomagok ----------
    fun setPacks(ids: Collection<String>) = repo.update { it.copy(packs = FoodPacks.ALL.filter { p -> p in ids }, setupDone = true) }
    fun togglePack(id: String) = repo.update { s ->
        val cur = s.packIds()
        s.copy(packs = FoodPacks.ALL.filter { p -> if (p == id) p !in cur else p in cur }, setupDone = true)
    }

    // ---------- Étkezési zavar csomag: számok nélküli rögzítés ----------
    fun logMeal(desc: String, meal: String, t: String, hunger: Int?, full: Int?, feel: String?, day: String = _view.value.toString()) {
        repo.update { s ->
            s.withDay(day) { d ->
                d.copy(items = d.items + FoodEntry(id = FoodLogic.rid(), name = desc.trim(), meal = meal, t = t, hunger = hunger, full = full, feel = feel))
            }
        }
    }

    fun updateEntry(e: FoodEntry) {
        val day = _view.value.toString()
        repo.update { s -> s.withDay(day) { d -> d.copy(items = d.items.map { if (it.id == e.id) e else it }) } }
    }

    // ---------- Cukorbetegség csomag: vércukornapló ----------
    fun addBg(v: Double, ctx: String, t: String) {
        if (v <= 0) return
        val day = _view.value.toString()
        repo.update { s -> s.withDay(day) { d -> d.copy(bg = (d.bg + BgRec(FoodLogic.rid(), t, v, ctx)).sortedBy { it.t }) } }
    }

    fun deleteBg(id: String) {
        val day = _view.value.toString()
        repo.update { s -> s.withDay(day) { d -> d.copy(bg = d.bg.filter { it.id != id }) } }
    }

    fun addEntry(f: Food, g: Double, meal: String, t: String, day: String = _view.value.toString(), est: String? = null) {
        if (g <= 0) return
        repo.update { s -> s.withDay(day) { d -> d.copy(items = d.items + logic.entry(f, g, meal, t, est)) } }
    }

    fun addEntries(list: List<Triple<Food, Double, String?>>, meal: String, t: String) {
        val day = _view.value.toString()
        repo.update { s -> s.withDay(day) { d -> d.copy(items = d.items + list.filter { it.second > 0 }.map { (f, g, est) -> logic.entry(f, g, meal, t, est) }) } }
    }

    fun addManual(name: String, kcal: Double, p: Double, cc: Double, f: Double, meal: String, t: String) {
        val day = _view.value.toString()
        repo.update { s ->
            s.withDay(day) { d ->
                d.copy(items = d.items + FoodEntry(id = FoodLogic.rid(), name = name.trim(), kcal = kcal, p = p, c = cc, f = f, meal = meal, t = t))
            }
        }
    }

    fun setGrams(e: FoodEntry, g: Double) {
        if (g <= 0) return
        val day = _view.value.toString()
        repo.update { s -> s.withDay(day) { d -> d.copy(items = d.items.map { if (it.id == e.id) logic.rescale(it, g) else it }) } }
    }

    fun deleteEntry(e: FoodEntry) {
        val day = _view.value.toString()
        repo.update { s -> s.withDay(day) { d -> d.copy(items = d.items.filter { it.id != e.id }) } }
    }

    fun water(delta: Double) {
        val day = _view.value.toString()
        repo.update { s -> s.withDay(day) { d -> d.copy(water = maxOf(0.0, d.water + delta)) } }
    }

    /** Visszaadja, hány tételt másolt át (0, ha az előző nap üres). */
    fun copyPrev(): Int {
        val day = _view.value
        val prev = state.value.day(day.minusDays(1).toString()).items
        if (prev.isEmpty()) return 0
        repo.update { s -> s.withDay(day.toString()) { d -> d.copy(items = d.items + prev.map { it.copy(id = FoodLogic.rid()) }) } }
        return prev.size
    }

    fun loadSample(sampleSuffix: String) {
        val day = _view.value.toString()
        val s0 = state.value
        val rows = listOf(
            listOf("tojas", 120, 0, "12:05"), listOf("spenot", 80, 0, "12:05"), listOf("rozs", 40, 0, "12:05"), listOf("avokado", 80, 0, "12:05"),
            listOf("turo", 250, 2, "15:30"), listOf("paprika", 150, 2, "15:30"),
            listOf("tofu", 200, 3, "19:00"), listOf("brokkoli", 200, 3, "19:00"), listOf("quinoa", 150, 3, "19:00"), listOf("olivaolaj", 10, 3, "19:00"),
        )
        val items = rows.mapNotNull { r ->
            val f = logic.findFood(s0, r[0] as String) ?: return@mapNotNull null
            logic.entry(f.copy(name = f.name + sampleSuffix), (r[1] as Int).toDouble(), logic.defaultMeal(s0.profile, s0.packFlags(), java.time.LocalTime.parse(r[3] as String)), r[3] as String, null)
        }
        repo.update { s -> s.withDay(day) { d -> d.copy(items = d.items + items) } }
    }

    // ---------- Receptek ----------
    fun saveRecipe(r: Recipe) = repo.update { s ->
        val i = s.recipes.indexOfFirst { it.id == r.id }
        s.copy(recipes = if (i >= 0) s.recipes.toMutableList().also { it[i] = r } else listOf(r) + s.recipes)
    }
    fun deleteRecipe(id: String) = repo.update { s -> s.copy(recipes = s.recipes.filter { it.id != id }) }

    // ---------- Ételek ----------
    fun toggleFav(id: String) = repo.update { s -> s.copy(favs = if (id in s.favs) s.favs - id else s.favs + id) }
    fun addCustom(f: Food) = repo.update { s -> s.copy(custom = s.custom + f.copy(id = "c-" + FoodLogic.rid(), cat = "sajat", src = "custom")) }
    fun deleteCustom(id: String) = repo.update { s -> s.copy(custom = s.custom.filter { it.id != id }) }

    // ---------- Súly ----------
    fun addWeight(d: LocalDate, kg: Double, waist: Double?) = repo.update { s ->
        val list = (s.weights.filter { it.d != d.toString() } + WeightRec(d.toString(), kg, waist)).sortedBy { it.d }
        val isLast = list.last().d == d.toString()
        s.copy(weights = list, profile = if (isLast) s.profile.copy(weight = kg) else s.profile)
    }
    fun deleteWeight(d: String) = repo.update { s -> s.copy(weights = s.weights.filter { it.d != d }) }

    // ---------- Profil ----------
    fun setProfile(fn: (Profile) -> Profile) = repo.update { s -> s.copy(profile = fn(s.profile), setupDone = true) }

    fun wipe() = repo.update { FoodState(profile = content.defProfile, setupDone = true, packs = it.packs) }

    fun importLegacy(text: String, onResult: (Int?) -> Unit) = viewModelScope.launch {
        onResult(runCatching { repo.importLegacy(text) }.getOrNull())
    }

    fun csv(): String {
        val s = state.value
        val rows = mutableListOf(listOf("Dátum", "Idő", "Étkezés", "Étel", "Becsült mennyiség", "Gramm", "kcal", "Fehérje", "Szénhidrát", "Zsír", "Rost", "GL", "Besorolás"))
        s.days.keys.sorted().forEach { k ->
            s.days.getValue(k).items.forEach { x ->
                rows += listOf(k, x.t, x.meal, x.name, x.est ?: "", x.g.toString(), Math.round(x.kcal).toString(),
                    "%.1f".format(x.p), "%.1f".format(x.c), "%.1f".format(x.f), "%.1f".format(x.fib), "%.1f".format(x.gl), content.rtxt[x.r] ?: "")
            }
        }
        return "﻿" + rows.joinToString("\n") { r -> r.joinToString(";") { "\"" + it.replace("\"", "\"\"") + "\"" } }
    }
}
