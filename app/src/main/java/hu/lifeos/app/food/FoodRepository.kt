package hu.lifeos.app.food

import hu.lifeos.app.data.AppDatabase
import hu.lifeos.app.data.KvEntity
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

class FoodRepository(private val db: AppDatabase, val content: FoodContent, private val scope: CoroutineScope) {
    val logic = FoodLogic(content)
    private val _state = MutableStateFlow(FoodState(profile = content.defProfile))
    val state: StateFlow<FoodState> = _state.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private val mutex = Mutex()

    suspend fun load() {
        db.kv().get(KEY)?.json?.let { raw ->
            runCatching { json.decodeFromString(FoodState.serializer(), raw) }.onSuccess { _state.value = it }
        }
        _loaded.value = true
    }

    fun update(fn: (FoodState) -> FoodState) {
        scope.launch {
            mutex.withLock {
                val next = fn(_state.value)
                _state.value = next
                db.kv().put(KvEntity(KEY, json.encodeToString(FoodState.serializer(), next)))
            }
        }
    }

    fun exportJson(): String = json.encodeToString(FoodState.serializer(), _state.value)

    /** A régi IR Napló mentése ({app:"ir-naplo", data:{…}} vagy közvetlenül az állapot). */
    suspend fun importLegacy(text: String): Int {
        val root: JsonObject = json.parseToJsonElement(text.trim()).jsonObject
        val data = (root["data"] as? JsonObject) ?: root
        require(data.containsKey("profile") && data.containsKey("days")) { "not an IR backup" }
        val parsed = json.decodeFromJsonElement(FoodState.serializer(), data)
        val fixed = parsed.copy(
            setupDone = true,
            days = parsed.days.mapValues { (_, d) -> d.copy(items = d.items.map { if (it.id.isBlank()) it.copy(id = FoodLogic.rid()) else it }) },
        )
        replace(fixed)
        return fixed.days.size
    }

    suspend fun replace(s: FoodState) {
        mutex.withLock {
            _state.value = s
            db.kv().put(KvEntity(KEY, json.encodeToString(FoodState.serializer(), s)))
        }
    }

    suspend fun replaceFromBackup(raw: String) = replace(json.decodeFromString(FoodState.serializer(), raw))

    companion object {
        const val KEY = "food"
        val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; encodeDefaults = true; explicitNulls = false }
    }
}
