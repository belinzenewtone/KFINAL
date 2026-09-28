package com.belinze.lifeos.data.db.dao

import com.belinze.lifeos.data.db.entity.LearningSessionEntity
import kotlinx.coroutines.flow.Flow
interface LearningSessionDao {
    // ── Observe ──────────────────────────────────────────────────────────────

        fun observeAll(): Flow<List<LearningSessionEntity>>

        fun observeByCategory(category: String?): Flow<List<LearningSessionEntity>>

    // ── LE-10: monthly hours — SUM(duration_minutes)/60.0 for current month ─

        fun observeMonthlyMinutes(): Flow<Int>

    // ── Write ─────────────────────────────────────────────────────────────────

        suspend fun insert(session: LearningSessionEntity)

        suspend fun update(session: LearningSessionEntity)

        suspend fun setCompleted(id: String, done: Int)

        suspend fun softDelete(id: String, now: String)

        suspend fun getById(id: String): LearningSessionEntity?
}
