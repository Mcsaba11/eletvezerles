package hu.lifeos.app.mods

import hu.lifeos.app.data.AppDatabase
import hu.lifeos.app.data.KvEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Egy modul teljes állapota egyetlen JSON-dokumentumként a kulcs–érték táblában.
 * Egyszerű, sémaváltozás nélkül bővíthető, és a mentésbe is egy az egyben kerül.
 */
class DocStore<T>(
    private val db: AppDatabase,
    val key: String,
    private val serializer: KSerializer<T>,
    private val default: T,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(default)
    val state: StateFlow<T> = _state.asStateFlow()
    private val mutex = Mutex()

    @Volatile var loaded = false
        private set

    suspend fun load() {
        mutex.withLock {
            db.kv().get(key)?.json?.let { raw -> runCatching { json.decodeFromString(serializer, raw) }.onSuccess { _state.value = it } }
            loaded = true
        }
    }

    suspend fun ensureLoaded() { if (!loaded) load() }

    /** Azonnali (megvárt) módosítás, pl. widgetről. */
    suspend fun updateNow(fn: (T) -> T) {
        mutex.withLock {
            val next = fn(_state.value)
            _state.value = next
            db.kv().put(KvEntity(key, json.encodeToString(serializer, next)))
        }
    }

    fun update(fn: (T) -> T) {
        scope.launch {
            mutex.withLock {
                val next = fn(_state.value)
                _state.value = next
                db.kv().put(KvEntity(key, json.encodeToString(serializer, next)))
            }
        }
    }

    fun export(): JsonElement = json.encodeToJsonElement(serializer, _state.value)

    suspend fun replace(el: JsonElement) {
        val v = json.decodeFromJsonElement(serializer, el)
        mutex.withLock {
            _state.value = v
            db.kv().put(KvEntity(key, json.encodeToString(serializer, v)))
        }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; encodeDefaults = true; explicitNulls = false }
    }
}

/** Az új modulok tárolói. */
class ModStores(db: AppDatabase, scope: CoroutineScope) {
    val money = DocStore(db, "mod_money", MoneyDoc.serializer(), MoneyDoc(), scope)
    val sleep = DocStore(db, "mod_sleep", SleepDoc.serializer(), SleepDoc(), scope)
    val move = DocStore(db, "mod_move", MoveDoc.serializer(), MoveDoc(), scope)
    val people = DocStore(db, "mod_people", PeopleDoc.serializer(), PeopleDoc(), scope)
    val goals = DocStore(db, "mod_goals", GoalsDoc.serializer(), GoalsDoc(), scope)
    val learn = DocStore(db, "mod_learn", LearnDoc.serializer(), LearnDoc(), scope)
    val house = DocStore(db, "mod_house", HouseDoc.serializer(), HouseDoc(), scope)
    val trackers = DocStore(db, "mod_trackers", TrackersDoc.serializer(), TrackersDoc(), scope)

    private val all: List<DocStore<*>> = listOf(money, sleep, move, people, goals, learn, house, trackers)

    suspend fun loadAll() = all.forEach { it.ensureLoaded() }

    fun exportAll(): Map<String, JsonElement> = all.associate { it.key to it.export() }

    suspend fun importAll(map: Map<String, JsonElement>) {
        all.forEach { st -> map[st.key]?.let { runCatching { st.replace(it) } } }
    }
}
