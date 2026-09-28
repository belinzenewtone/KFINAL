package com.belinze.lifeos.data.db.dao

import com.belinze.lifeos.data.db.entity.EventEntity
import kotlinx.coroutines.flow.Flow
interface EventDao {
        suspend fun getFrom(fromDate: String): List<EventEntity>

        suspend fun getAll(): List<EventEntity>

        suspend fun getById(id: String): EventEntity?

        suspend fun search(q: String, limit: Int): List<EventEntity>

        suspend fun getInRange(startDate: String, endDate: String): List<EventEntity>

    /** Next upcoming event from today */
        suspend fun getNextUpcoming(today: String): EventEntity?

        fun observeAll(): Flow<List<EventEntity>>

        suspend fun insert(event: EventEntity)

        suspend fun update(event: EventEntity)

        suspend fun softDelete(id: String, timestamp: String)
}
