package hu.lifeos.app.food

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/*
 * Az Étkezés és IR app adatai. A mezőnevek megegyeznek a régi webes IR Napló mentésével
 * (ir2.state), így a régi mentés közvetlenül betölthető.
 */

@Serializable
data class Food(
    val id: String,
    val name: String,
    val cat: String = "sajat",
    val r: String = "m",
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val c: Double = 0.0,
    val f: Double = 0.0,
    val fib: Double = 0.0,
    val gi: Double = 0.0,
    val u: String = "",
    val ug: Double = 0.0,
    val note: String = "",
    /** "base", "custom" vagy "recipe" – nem mentjük, futás közben kap értéket. */
    val src: String = "base",
)

@Serializable data class Meas(val l: String, val g: Map<String, Double>, val h: String = "")
@Serializable data class RecipeItem(val fid: String, val g: Double)
@Serializable data class Recipe(val id: String, val name: String = "", val servings: Int = 1, val items: List<RecipeItem> = emptyList())
/** Tippcsoport; a [pack] megadja, melyik csomaghoz tartozik ("" = mindenkinek, "num" = csak ha vannak számok). */
@Serializable data class TipSection(val t: String, val items: List<List<String>>, val pack: String = "")

@Serializable
data class Profile(
    val sex: String = "f",
    val age: Double = 30.0,
    val height: Double = 170.0,
    val weight: Double = 70.0,
    val act: String = "1.375",
    val goal: String = "0",
    val winStart: String = "12:00",
    val winEnd: String = "20:00",
    val target: Double = 0.0,
    val water: Double = 2500.0,
    /** Cukorbetegség: napi szénhidrát-keret grammban (az orvos/dietetikus szerint); 0 = nincs megadva. */
    val carbGoal: Double = 0.0,
    /** Cukorbetegség: a vércukor célsávja mmol/l-ben (az orvos által megadott értékek az irányadók). */
    val bgLow: Double = 4.0,
    val bgHigh: Double = 10.0,
)

@Serializable
data class FoodContent(
    val foods: List<Food>,
    val cats: Map<String, String>,
    val rtxt: Map<String, String>,
    val meals: List<String>,
    /** Időszakos böjtnél használt étkezésnevek (Böjtzáró…). */
    val mealsFast: List<String> = emptyList(),
    val swaps: Map<String, List<String>>,
    val samples: List<Recipe>,
    val meas: Map<String, Meas>,
    val catMeas: Map<String, List<String>>,
    val defProfile: Profile,
    val qtyW: Map<String, Double>,
    val sizeW: Map<String, Double>,
    val unitW: List<List<String>>,
    val stop: List<String>,
    val alias: Map<String, String>,
    val acts: List<List<String>>,
    val goals: List<List<String>>,
    val tips: List<TipSection>,
    val disclaimer: String,
    /** Étkezési zavar csomag: érzések a rögzítéshez. */
    val feelings: List<String> = emptyList(),
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
        fun load(context: Context): FoodContent {
            val lang = Locale.getDefault().language
            val text = runCatching { context.assets.open("content/$lang/food.json").bufferedReader().use { it.readText() } }
                .getOrElse { context.assets.open("content/hu/food.json").bufferedReader().use { it.readText() } }
            return json.decodeFromString(serializer(), text)
        }
    }
}

// ---------------- Állapot ----------------

@Serializable
data class FoodEntry(
    val id: String = "",
    val fid: String = "",
    val name: String = "",
    val r: String = "",
    val g: Double = 0.0,
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val c: Double = 0.0,
    val f: Double = 0.0,
    val fib: Double = 0.0,
    val gl: Double = 0.0,
    val meal: String = "Ebéd",
    val t: String = "12:00",
    val est: String? = null,
    /** Étkezési zavar csomag: éhség evés előtt és teltség utána (1–5), érzés. */
    val hunger: Int? = null,
    val full: Int? = null,
    val feel: String? = null,
)

/** Vércukormérés (mmol/l); ctx: ehgyomor, elotte, utana, lefekves. */
@Serializable data class BgRec(val id: String = "", val t: String = "", val v: Double = 0.0, val ctx: String = "")

@Serializable data class FoodDay(val water: Double = 0.0, val items: List<FoodEntry> = emptyList(), val bg: List<BgRec> = emptyList())
@Serializable data class WeightRec(val d: String, val kg: Double, val waist: Double? = null)

@Serializable
data class FoodState(
    val profile: Profile = Profile(),
    val recipes: List<Recipe> = emptyList(),
    val custom: List<Food> = emptyList(),
    val favs: List<String> = emptyList(),
    val weights: List<WeightRec> = emptyList(),
    val days: Map<String, FoodDay> = emptyMap(),
    /** Igaz, ha a felhasználó már legalább egyszer megnyitotta vagy betöltötte a régi adatait. */
    val setupDone: Boolean = false,
    /** Bekapcsolt fókuszcsomagok (FoodPacks); null = régi mentés. */
    val packs: List<String>? = null,
) {
    fun day(k: String): FoodDay = days[k] ?: FoodDay()
    fun withDay(k: String, fn: (FoodDay) -> FoodDay) = copy(days = days + (k to fn(day(k))))
}

data class Nutr(val kcal: Double = 0.0, val p: Double = 0.0, val c: Double = 0.0, val f: Double = 0.0, val fib: Double = 0.0, val gl: Double = 0.0) {
    operator fun plus(o: Nutr) = Nutr(kcal + o.kcal, p + o.p, c + o.c, f + o.f, fib + o.fib, gl + o.gl)
    operator fun times(k: Double) = Nutr(kcal * k, p * k, c * k, f * k, fib * k, gl * k)
}

data class Targets(
    val bmr: Double, val tdee: Double, val target: Double, val p: Int, val c: Int, val f: Int,
    val bmi: Double, val floored: Boolean, val floor: Int, val water: Double,
)
