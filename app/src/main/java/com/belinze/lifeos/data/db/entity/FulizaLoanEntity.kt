package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuliza_loans",
    indices = [Index("status")]
)
data class FulizaLoanEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "draw_code") val drawCode: String? = null,
    @ColumnInfo(name = "draw_amount_kes") val drawAmountKes: Double = 0.0,
    @ColumnInfo(name = "total_repaid_kes", defaultValue = "0")
                                                val totalRepaidKes: Double = 0.0,
    @ColumnInfo(name = "status",    defaultValue = "active")
                                                val status: String = "active",
    @ColumnInfo(name = "draw_date") val drawDate: String? = null,
    @ColumnInfo(name = "last_repayment_date") val lastRepaymentDate: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "user_id") val userId: String? = null,
)
