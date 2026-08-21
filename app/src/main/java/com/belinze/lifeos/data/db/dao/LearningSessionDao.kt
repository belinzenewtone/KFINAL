package com.belinze.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.belinze.lifeos.data.db.entity.LearningSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningSessionDao {
    // ── Observe ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM learning_sessions WHERE deleted_at IS NULL ORDER BY created_at DESC")
    fun observeAll(): Flow<List<LearningSessionEntity>>

    @Query("""
        SELECT * FROM learning_sessions
        WHERE deleted_at IS NULL
          AND (:category IS NULL OR category = :category)
        ORDER BY created_at DESC
    """)
    fun observeByCategory(category: String?): Flow<List<LearningSessionEntity>>

    // ── LE-10: monthly hours — SUM(duration_minutes)/60.0 for current month ─

    @Query("""
        SELECT IFNULL(SUM(duration_minutes), 0)
        FROM learning_sessions
        WHERE deleted_at IS NULL
          AND is_completed = 1
          AND strftime('%Y-%m', logged_at) = strftime('%Y-%m', 'now')
    """)
    fun observeMonthlyMinutes(): Flow<Int>

    // ── Write ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: LearningSessionEntity)

    @Update
    suspend fun update(session: LearningSessionEntity)

    @Query("UPDATE learning_sessions SET is_completed = :done WHERE id = :id")
    suspend fun setCompleted(id: String, done: Int)

    @Query("UPDATE learning_sessions SET deleted_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("SELECT * FROM learning_sessions WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    suspend fun getById(id: String): LearningSessionEntity?
}
