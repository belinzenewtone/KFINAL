package com.belinze.lifeos.data.db.dao

import androidx.room.*
import com.belinze.lifeos.data.db.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("""
        SELECT * FROM events
        WHERE deleted_at IS NULL
          AND date >= :fromDate
        ORDER BY date ASC
    """)
    suspend fun getFrom(fromDate: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE deleted_at IS NULL ORDER BY date ASC")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE deleted_at IS NULL AND (title LIKE '%' || :q || '%' OR location LIKE '%' || :q || '%') ORDER BY date ASC LIMIT :limit")
    suspend fun search(q: String, limit: Int): List<EventEntity>

    @Query("""
        SELECT * FROM events
        WHERE deleted_at IS NULL
          AND date >= :startDate AND date <= :endDate
        ORDER BY date ASC
    """)
    suspend fun getInRange(startDate: String, endDate: String): List<EventEntity>

    /** Next upcoming event from today */
    @Query("""
        SELECT * FROM events
        WHERE deleted_at IS NULL
          AND date >= :today
          AND status = 'active'
        ORDER BY date ASC
        LIMIT 1
    """)
    suspend fun getNextUpcoming(today: String): EventEntity?

    @Query("SELECT * FROM events WHERE deleted_at IS NULL ORDER BY date ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Update
    suspend fun update(event: EventEntity)

    @Query("UPDATE events SET deleted_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: String)
}
