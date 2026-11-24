package com.shrey.prankspot.data

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

@Entity(tableName = "favorite_ssid")
data class FavoriteSsid(@PrimaryKey val ssid: String, val categoryId: String, val createdAt: Long)

@Entity(tableName = "favorite_prank")
data class FavoritePrank(@PrimaryKey val prankId: String, val createdAt: Long)

/** No passwords, no visitor addresses: just what ran and for how long. */
@Entity(tableName = "session_history")
data class SessionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val ssid: String,
    val categoryId: String,
    val prankId: String,
    val startedAt: Long,
    val endedAt: Long,
    val visitorCount: Int,
)

@Dao
interface PrankDao {
    @Query("SELECT * FROM favorite_ssid ORDER BY createdAt DESC")
    fun favoriteSsids(): Flow<List<FavoriteSsid>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteSsid(f: FavoriteSsid)

    @Query("DELETE FROM favorite_ssid WHERE ssid = :ssid")
    suspend fun removeFavoriteSsid(ssid: String)

    @Query("SELECT * FROM favorite_prank")
    fun favoritePranks(): Flow<List<FavoritePrank>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoritePrank(f: FavoritePrank)

    @Query("DELETE FROM favorite_prank WHERE prankId = :id")
    suspend fun removeFavoritePrank(id: String)

    @Query("SELECT * FROM session_history ORDER BY startedAt DESC LIMIT 50")
    fun sessions(): Flow<List<SessionRecord>>

    @Insert
    suspend fun addSession(s: SessionRecord)

    @Query("DELETE FROM session_history")
    suspend fun clearSessions()
}

@Database(
    entities = [FavoriteSsid::class, FavoritePrank::class, SessionRecord::class],
    version = 1,
    exportSchema = true,
)
abstract class PrankDatabase : RoomDatabase() {
    abstract fun dao(): PrankDao

    companion object {
        fun create(context: Context): PrankDatabase =
            Room.databaseBuilder(context, PrankDatabase::class.java, "prankspot.db").build()
    }
}
