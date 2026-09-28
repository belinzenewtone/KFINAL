package com.belinze.lifeos.data.db.dao

import com.belinze.lifeos.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
interface TaskDao {
        suspend fun getAll(): List<TaskEntity>

        suspend fun getUpcoming(dueBefore: String, limit: Int): List<TaskEntity>

        suspend fun countPending(): Int

    /** Count active tasks with deadline within [startOfDay]..[endOfDay] (ISO date-time strings). */
        suspend fun countDueToday(startOfDay: String, endOfDay: String): Int

        suspend fun getById(id: String): TaskEntity?

        suspend fun search(q: String, limit: Int): List<TaskEntity>

        fun observeAll(): Flow<List<TaskEntity>>

        suspend fun insert(task: TaskEntity)

        suspend fun update(task: TaskEntity)

        suspend fun softDelete(id: String, timestamp: String)

    /** Count tasks completed (status='done') on or after [since] (ISO date-time string).
     *  Falls back to updated_at when completed_at was never stamped — mirrors the
     *  WeekReviewScreen.tsx query. */
        suspend fun countCompletedSince(since: String): Int

    /** Count every outstanding (not-done) task. WeekReviewScreen.tsx counts all pending
     *  tasks — it is deliberately NOT scoped to the current week. */
        suspend fun countAllPending(): Int
}
