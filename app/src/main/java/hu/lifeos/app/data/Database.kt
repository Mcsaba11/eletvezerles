package hu.lifeos.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE startMillis < :to AND endMillis >= :from ORDER BY allDay DESC, startMillis")
    fun observeRange(from: Long, to: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY startMillis")
    suspend fun all(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): EventEntity?

    @Insert
    suspend fun insert(e: EventEntity): Long

    @Update
    suspend fun update(e: EventEntity)

    @Delete
    suspend fun delete(e: EventEntity)

    @Query("DELETE FROM events")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<EventEntity>)
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY done, CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate, important DESC, createdAt")
    fun observeAll(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos")
    suspend fun all(): List<TodoEntity>

    @Insert
    suspend fun insert(t: TodoEntity): Long

    @Update
    suspend fun update(t: TodoEntity)

    @Delete
    suspend fun delete(t: TodoEntity)

    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun clearDone()

    @Query("DELETE FROM todos")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TodoEntity>)
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY ts DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date = :date ORDER BY ts DESC")
    fun observeDay(date: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries")
    suspend fun all(): List<EntryEntity>

    @Insert
    suspend fun insert(e: EntryEntity): Long

    @Delete
    suspend fun delete(e: EntryEntity)

    @Query("DELETE FROM entries")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<EntryEntity>)
}

@Dao
interface MoodDao {
    @Query("SELECT * FROM moods WHERE date = :date")
    fun observe(date: String): Flow<MoodEntity?>

    @Query("SELECT * FROM moods WHERE date = :date")
    suspend fun get(date: String): MoodEntity?

    @Query("SELECT * FROM moods ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MoodEntity>>

    @Query("SELECT * FROM moods")
    suspend fun all(): List<MoodEntity>

    @Upsert
    suspend fun upsert(m: MoodEntity)

    @Query("DELETE FROM moods")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<MoodEntity>)
}

@Dao
interface KvDao {
    @Query("SELECT * FROM kv WHERE `key` = :key")
    suspend fun get(key: String): KvEntity?

    @Query("SELECT * FROM kv WHERE `key` = :key")
    fun observe(key: String): Flow<KvEntity?>

    @Upsert
    suspend fun put(e: KvEntity)

    @Query("SELECT * FROM kv")
    suspend fun all(): List<KvEntity>
}

@Database(
    entities = [EventEntity::class, TodoEntity::class, EntryEntity::class, MoodEntity::class, KvEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun events(): EventDao
    abstract fun todos(): TodoDao
    abstract fun entries(): EntryDao
    abstract fun moods(): MoodDao
    abstract fun kv(): KvDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "eletvezerles.db").build()
    }
}
