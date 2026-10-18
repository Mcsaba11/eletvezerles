package hu.lifeos.app.data

import androidx.room.withTransaction
import hu.lifeos.app.AppContainer
import hu.lifeos.app.satori.SatoriRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class BackupFile(
    val app: String = "eletvezerles",
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val events: List<EventEntity> = emptyList(),
    val todos: List<TodoEntity> = emptyList(),
    val entries: List<EntryEntity> = emptyList(),
    val moods: List<MoodEntity> = emptyList(),
    val satori: JsonElement? = null,
    val food: JsonElement? = null,
)

enum class ImportKind { FULL, SATORI_LEGACY, FOOD_LEGACY }

/** Teljes mentés egyetlen JSON-fájlba, és visszatöltés. A régi Satori mentését is felismeri. */
class Backup(private val c: AppContainer) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; encodeDefaults = true; prettyPrint = false }

    suspend fun export(): String {
        val db = c.db
        val satori = json.parseToJsonElement(c.satori.exportJson())
        val food = json.parseToJsonElement(c.food.exportJson())
        return json.encodeToString(
            BackupFile.serializer(),
            BackupFile(
                events = db.events().all(), todos = db.todos().all(), entries = db.entries().all(),
                moods = db.moods().all(), satori = satori, food = food,
            )
        )
    }

    /** Felismeri, mi van a fájlban, és betölti. Hibás tartalomnál kivételt dob. */
    suspend fun import(text: String): ImportKind {
        val root = json.parseToJsonElement(text.trim()).jsonObject
        val app = (root["app"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        if (app == "ir-naplo" || (app == null && root["profile"] != null && root["days"] != null)) {
            c.food.importLegacy(text)
            return ImportKind.FOOD_LEGACY
        }
        if (root["app"] == null && root["data"] != null) {
            c.satori.importLegacy(text)
            return ImportKind.SATORI_LEGACY
        }
        val b = json.decodeFromJsonElement(BackupFile.serializer(), root)
        require(b.app == "eletvezerles")
        c.db.withTransaction {
            c.db.events().clear(); c.db.events().insertAll(b.events)
            c.db.todos().clear(); c.db.todos().insertAll(b.todos)
            c.db.entries().clear(); c.db.entries().insertAll(b.entries)
            c.db.moods().clear(); c.db.moods().insertAll(b.moods)
        }
        b.satori?.let { c.satori.replaceFromBackup(SatoriRepository.json.encodeToString(JsonElement.serializer(), it)) }
        b.food?.let { c.food.replaceFromBackup(SatoriRepository.json.encodeToString(JsonElement.serializer(), it)) }
        return ImportKind.FULL
    }
}
