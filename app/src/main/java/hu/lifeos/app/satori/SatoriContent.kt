package hu.lifeos.app.satori

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/*
 * A Satori minden szövege (szintek, idézetek, témák, testi idővonalak, eszköztár…) egy
 * nyelvenkénti JSON-fájlban van: assets/content/<nyelv>/satori.json. Az angol változathoz
 * elég egy content/en/satori.json fájl; ha nincs, a magyar töltődik be.
 */

@Serializable data class TypeDef(val id: String, val label: String, val icon: String)

@Serializable data class StageDef(
    val days: Int, val title: String, val sub: String, val motto: String,
    val welcome: String, val reward: String,
)

@Serializable data class BodyStage(
    val minDay: Int, val maxDay: Int? = null,
    @SerialName("when") val whenText: String, val icon: String, val text: String,
)

@Serializable data class BodyTimeline(
    val badge: String, val icon: String, val heading: String, val cardTitle: String,
    val intro: String, val safety: String? = null, val stages: List<BodyStage>,
)

@Serializable data class StateDef(val id: String, val label: String, val sub: String, val icon: String)
@Serializable data class Tip(val icon: String, val text: String)
@Serializable data class OutcomeMetaDef(val label: String, val icon: String, val journalState: String)

@Serializable data class ModeDef(
    val id: String, val name: String, val icon: String, val short: String,
    val desc: String, val dayDesc: String, val labels: Map<String, String> = emptyMap(),
)

@Serializable data class GroundingDef(val n: Int, val icon: String, val text: String)
@Serializable data class BreathPhase(val n: String, val d: Int, val k: String)
@Serializable data class TechniqueDef(val id: String, val title: String, val sub: String)
@Serializable data class MzStep(val a: Float, val w: List<Int>, val t: String, val x: String)

@Serializable data class MzTool(
    val id: String, val ic: String, val n: String, val ev: List<String>, val time: String,
    val quick: Boolean, val daily: Boolean, val how: String, val sci: String, val warn: String = "",
)

@Serializable data class MentorTopic(val id: String, val label: String, val icon: String)

@Serializable data class ThemeColors(
    val primary: String, val primaryContainer: String, val onPrimaryContainer: String,
    val secondary: String, val secondaryContainer: String, val onSecondaryContainer: String,
    val tertiary: String, val tertiaryContainer: String, val onTertiaryContainer: String,
    val outline: String, val outlineVariant: String,
)

@Serializable data class ThemeDef(
    val id: String, val label: String, val icon: String, val tagline: String,
    val colors: ThemeColors,
    val stageTitles: List<String> = emptyList(),
    val stageSubs: List<String> = emptyList(),
    val stageMottos: List<String> = emptyList(),
    val stageRewards: List<String> = emptyList(),
    val quotes: List<String> = emptyList(),
    val pledgeText: String = "",
    val quoteCaption: String = "",
    val rankLabel: String = "",
    val mentorName: String = "",
    val journalTitle: String = "",
    val addressTerm: String = "",
    val mentorSubtitle: String = "",
    val mentorGreeting: String = "",
    val mentorHeading: String = "",
)

@Serializable data class SatoriContent(
    val types: List<TypeDef>,
    val stages: List<StageDef>,
    val quotes: List<String>,
    @SerialName("body_timelines") val bodyTimelines: Map<String, BodyTimeline>,
    val messages: Map<String, List<String>>,
    @SerialName("state_defs") val stateDefs: List<StateDef>,
    @SerialName("defusion_lines") val defusionLines: List<String>,
    @SerialName("type_tips") val typeTips: Map<String, List<Tip>>,
    @SerialName("micro_tasks") val microTasks: List<String>,
    @SerialName("outcome_meta") val outcomeMeta: Map<String, OutcomeMetaDef>,
    val modes: Map<String, ModeDef>,
    @SerialName("comfort_messages") val comfortMessages: List<List<String>>,
    val land: Map<String, List<String>>,
    val grounding: List<GroundingDef>,
    val techniques: Map<String, List<BreathPhase>>,
    @SerialName("technique_defs") val techniqueDefs: List<TechniqueDef>,
    @SerialName("craving_triggers") val cravingTriggers: List<String>,
    @SerialName("mz_steps") val mzSteps: List<MzStep>,
    @SerialName("mz_tools") val mzTools: List<MzTool>,
    @SerialName("mz_myths") val mzMyths: List<List<String>>,
    @SerialName("mentor_open") val mentorOpen: Map<String, Map<String, String>>,
    @SerialName("mentor_steps") val mentorSteps: Map<String, List<String>>,
    @SerialName("mentor_tasks") val mentorTasks: List<String>,
    @SerialName("mentor_topics") val mentorTopics: List<MentorTopic>,
    @SerialName("daypart_label") val daypartLabel: Map<String, String>,
    val themes: List<ThemeDef>,
) {
    fun theme(id: String): ThemeDef = themes.firstOrNull { it.id == id } ?: themes.first()
    /** A felhasználó saját szokásai; a tároló frissíti az állapot változásakor. */
    var custom: List<TypeDef> = emptyList()

    /** Az összes választható szokás: a beépítettek és a sajátok. */
    fun allTypes(): List<TypeDef> = types + custom
    fun type(id: String): TypeDef = allTypes().firstOrNull { it.id == id } ?: types.first()
    fun isCustom(id: String): Boolean = custom.any { it.id == id }
    fun mode(id: String): ModeDef = modes[id] ?: modes.getValue("balanced")
    fun body(type: String): BodyTimeline = bodyTimelines[type] ?: bodyTimelines["altalanos"] ?: bodyTimelines.getValue("cukor")
    fun tips(type: String): List<Tip> = typeTips[type] ?: typeTips["altalanos"].orEmpty()

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

        fun load(context: Context): SatoriContent {
            val lang = Locale.getDefault().language
            val path = "content/$lang/satori.json"
            val text = runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }
                .getOrElse { context.assets.open("content/hu/satori.json").bufferedReader().use { it.readText() } }
            return json.decodeFromString(serializer(), text)
        }
    }
}
