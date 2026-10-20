package hu.lifeos.app.satori

import hu.lifeos.app.data.AppDatabase
import hu.lifeos.app.data.KvEntity
import hu.lifeos.app.data.MoodEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class SatoriRepository(
    private val db: AppDatabase,
    val content: SatoriContent,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SatoriState().normalized(content.types.map { it.id }))
    val state: StateFlow<SatoriState> = _state.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private val mutex = Mutex()

    private fun set(s: SatoriState) { content.custom = s.custom; _state.value = s }

    suspend fun load() {
        val raw = db.kv().get(KEY)?.json
        if (raw != null) {
            runCatching { json.decodeFromString(SatoriState.serializer(), raw) }
                .onSuccess { set(it.normalized(content.types.map { t -> t.id })) }
        }
        _loaded.value = true
    }

    fun update(fn: (SatoriState) -> SatoriState) {
        scope.launch {
            mutex.withLock {
                val next = fn(_state.value)
                set(next)
                db.kv().put(KvEntity(KEY, json.encodeToString(SatoriState.serializer(), next)))
            }
        }
    }

    fun exportJson(): String = json.encodeToString(SatoriState.serializer(), _state.value)

    /**
     * Régi Satori-mentés (satori-v1) vagy az új app saját Satori-része. A hangulatnaplót
     * átemeli a közös Hangulat táblába. Hibás fájlnál kivételt dob.
     */
    suspend fun importLegacy(text: String) {
        val obj: JsonObject = json.parseToJsonElement(text.trim()).jsonObject
        require(obj.containsKey("data")) { "not a satori backup" }
        val parsed = json.decodeFromJsonElement(SatoriState.serializer(), obj)
            .normalized(content.types.map { it.id })
            .copy(onboardingDone = true)
        mutex.withLock {
            set(parsed)
            db.kv().put(KvEntity(KEY, json.encodeToString(SatoriState.serializer(), parsed)))
        }
        parsed.moodLog.forEach { m ->
            val existing = db.moods().get(m.date)
            db.moods().upsert(
                MoodEntity(
                    date = m.date,
                    mood = existing?.mood ?: m.mood,
                    energy = existing?.energy ?: m.energy,
                )
            )
        }
    }

    suspend fun replaceFromBackup(raw: String) {
        val parsed = json.decodeFromString(SatoriState.serializer(), raw).normalized(content.types.map { it.id })
        mutex.withLock {
            set(parsed)
            db.kv().put(KvEntity(KEY, json.encodeToString(SatoriState.serializer(), parsed)))
        }
    }

    companion object {
        const val KEY = "satori"
        val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; encodeDefaults = true; explicitNulls = false }
    }
}
