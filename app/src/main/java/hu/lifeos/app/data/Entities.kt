package hu.lifeos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Saját naptáresemény (a telefon naptárában lévők nem itt vannak, azokat élőben olvassuk). */
@Serializable
@Entity(tableName = "events", indices = [Index("startMillis")])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean = false,
    val location: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "todos", indices = [Index("dueDate"), Index("done")])
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    /** yyyy-MM-dd vagy null, ha nincs határidő. */
    val dueDate: String? = null,
    /** HH:mm vagy null. */
    val dueTime: String? = null,
    val important: Boolean = false,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Az idővonal általános bejegyzése. Minden app ide is írhat; a kind mondja meg, mi ez.
 */
@Serializable
@Entity(tableName = "entries", indices = [Index("date"), Index("ts")])
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    /** yyyy-MM-dd */
    val date: String,
    val kind: String,
    val text: String = "",
    val amount: Double? = null,
    val app: String = "core",
)

object EntryKind {
    const val NOTE = "note"
    const val EXPENSE = "expense"
    const val INCOME = "income"
}

/** Napi hangulat (1–5) és energia (1–3), minden app közösen használja. */
@Serializable
@Entity(tableName = "moods")
data class MoodEntity(
    @PrimaryKey val date: String,
    val mood: Int? = null,
    val energy: Int? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Kulcs–érték tár egész dokumentumokhoz (pl. a Satori teljes állapota JSON-ként). */
@Entity(tableName = "kv")
data class KvEntity(
    @PrimaryKey val key: String,
    val json: String,
)
