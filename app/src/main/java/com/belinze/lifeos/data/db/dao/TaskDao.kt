package com.belinze.lifeos.data.db.dao

import androidx.room.*
import com.belinze.lifeos.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deleted_at IS NULL ORDER BY deadline ASC, created_at DESC")
    suspend fun getAll(): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks
        WHERE deleted_at IS NULL AND status = 'active'
          AND deadline IS NOT NULL AND deadline <= :dueBefore
        ORDER BY deadline ASC
        LIMIT :limit
    """)
    suspend fun getUpcoming(dueBefore: String, limit: Int): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL AND status = 'active'")
    suspend fun countPending(): Int

    @Query("SELECT * FROM tasks WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE deleted_at IS NULL AND (title LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%') ORDER BY deadline ASC LIMIT :limit")
    suspend fun search(q: String, limit: Int): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deleted_at IS NULL ORDER BY deadline ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET deleted_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: String)

    /** Count tasks completed (status='done') on or after [since] (ISO date-time string). */
    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE deleted_at IS NULL
          AND status = 'done'
          AND completed_at >= :since
    """)
    suspend fun countCompletedSince(since: String): Int
}
