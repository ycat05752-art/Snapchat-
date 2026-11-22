package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_snaps")
data class SavedSnapEntity(
    @PrimaryKey val id: String,
    val filePath: String,
    val mediaType: String, // "photo" or "video"
    val caption: String,
    val filterName: String,
    val filtered: Boolean,
    val favorite: Boolean,
    val dateKey: String,
    val createdAtMillis: Long
)

@Entity(tableName = "filter_prefs")
data class FilterPrefEntity(
    @PrimaryKey val filterId: String,
    val favorite: Boolean,
    val lastUsedAtMillis: Long
)

@Entity(tableName = "incoming_sim_snaps")
data class IncomingSimSnapEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderName: String,
    val senderUsername: String,
    val themeTitle: String,
    val caption: String,
    val filterName: String,
    val paletteStartArgb: Long,
    val paletteEndArgb: Long,
    val stickerEmoji: String,
    val viewOnce: Boolean,
    val replayAllowed: Boolean,
    val opened: Boolean,
    val replayed: Boolean,
    val timerSeconds: Int,
    val receivedAtMillis: Long
)

@Dao
interface SavedSnapDao {
    @Query("SELECT * FROM saved_snaps ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<SavedSnapEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SavedSnapEntity)

    @Query("UPDATE saved_snaps SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("DELETE FROM saved_snaps WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}

@Dao
interface FilterPrefDao {
    @Query("SELECT * FROM filter_prefs ORDER BY lastUsedAtMillis DESC")
    fun observeAll(): Flow<List<FilterPrefEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: FilterPrefEntity)
}

@Dao
interface IncomingSimSnapDao {
    @Query("SELECT * FROM incoming_sim_snaps ORDER BY receivedAtMillis DESC")
    fun observeAll(): Flow<List<IncomingSimSnapEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snap: IncomingSimSnapEntity)

    @Query("UPDATE incoming_sim_snaps SET opened = 1 WHERE id = :id")
    suspend fun markOpened(id: String)

    @Query("UPDATE incoming_sim_snaps SET replayed = 1 WHERE id = :id")
    suspend fun markReplayed(id: String)
}

@Database(
    entities = [SavedSnapEntity::class, FilterPrefEntity::class, IncomingSimSnapEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppLocalDatabase : RoomDatabase() {
    abstract fun savedSnapDao(): SavedSnapDao
    abstract fun filterPrefDao(): FilterPrefDao
    abstract fun incomingSimSnapDao(): IncomingSimSnapDao

    companion object {
        @Volatile
        private var INSTANCE: AppLocalDatabase? = null

        fun getInstance(context: Context): AppLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppLocalDatabase::class.java,
                    "nilesh_snapchat_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
