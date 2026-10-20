package hu.lifeos.app.food

import androidx.compose.runtime.staticCompositionLocalOf

/*
 * Az Étkezés modul alapja egy általános étkezési napló. Erre épülnek rá a bekapcsolható
 * fókuszcsomagok, amelyek saját mérőszámokat, figyelmeztetéseket, tippeket és kártyákat hoznak.
 */
object FoodPacks {
    /** Inzulinrezisztencia: glikémiás terhelés, étel-besorolás, cserejavaslatok. */
    const val IR = "ir"
    /** Időszakos böjt: étkezési ablak, böjt-gyűrű, ablakon kívüli tételek jelzése. */
    const val FAST = "fast"
    /** Étkezési zavar: számok nélkül, rendszeresség, éhség és teltség, érzések. */
    const val ED = "ed"
    /** Cukorbetegség: szénhidrát-követés, vércukornapló, gyors felszívódású ételek jelzése. */
    const val DIAB = "diab"

    val ALL = listOf(IR, FAST, ED, DIAB)
}

/** A bekapcsolt csomagokból levezetett megjelenítési szabályok. */
data class Packs(val ir: Boolean = false, val fast: Boolean = false, val ed: Boolean = false, val diab: Boolean = false) {
    /** Kalória, makrók, súly, célok. Étkezési zavar csomagnál rejtve. */
    val numbers: Boolean get() = !ed
    /** Jó / mértékkel / kerülendő besorolás. Étkezési zavarnál sosem jelenik meg. */
    val rating: Boolean get() = ir && !ed
    /** Szénhidrát grammban: cukorbetegségnél mindig, egyébként ha vannak számok. */
    val carbs: Boolean get() = diab || numbers

    companion object {
        fun of(ids: Set<String>) = Packs(
            ir = FoodPacks.IR in ids, fast = FoodPacks.FAST in ids,
            ed = FoodPacks.ED in ids, diab = FoodPacks.DIAB in ids,
        )
    }
}

/** A bekapcsolt csomagok. Régi (csomagok előtti) mentésnél az IR és a böjt marad bekapcsolva. */
fun FoodState.packIds(): Set<String> =
    packs?.toSet() ?: if (setupDone || days.isNotEmpty()) setOf(FoodPacks.IR, FoodPacks.FAST) else emptySet()

fun FoodState.packFlags(): Packs = Packs.of(packIds())

val LocalPacks = staticCompositionLocalOf { Packs() }
