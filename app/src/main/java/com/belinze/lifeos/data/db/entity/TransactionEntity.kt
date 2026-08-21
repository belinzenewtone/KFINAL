package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `transactions` table.
 * Schema is authoritative — verified against src/database/schema.ts.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),
        Index("category"),
        Index("merchant"),
        Index(value = ["mpesa_code"],       unique = true, name = "idx_tx_mpesa_code"),
        Index("source_hash"),
        Index("semantic_hash"),
        Index("deleted_at"),
        Index(value = ["institution_id", "external_ref"], unique = true, name = "idx_tx_inst_ext_ref"),
        Index("institution_id", "date"),
        Index("institution_id", "category"),
        Index("category", "date"),
        Index("transaction_type", "date"),
        Index("status", "date"),
        Index("amount", "merchant", "date"),
    ]
)
data class TransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "amount") val amount: Double = 0.0,
    @ColumnInfo(name = "merchant") val merchant: String? = null,
    @ColumnInfo(name = "category") val category: String? = null,
    @ColumnInfo(name = "date") val date: String? = null,
    @ColumnInfo(name = "source") val source: String? = null,
    @ColumnInfo(name = "transaction_type") val transactionType: String? = null,
    @ColumnInfo(name = "mpesa_code") val mpesaCode: String? = null,
    @ColumnInfo(name = "source_hash") val sourceHash: String? = null,
    @ColumnInfo(name = "raw_sms") val rawSms: String? = null,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "balance_after") val balanceAfter: Double? = null,
    @ColumnInfo(name = "fee") val fee: Double? = null,
    @ColumnInfo(name = "status",        defaultValue = "completed")
                                                val status: String = "completed",
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "sync_state",    defaultValue = "pending")
                                                val syncState: String = "pending",
    @ColumnInfo(name = "record_source", defaultValue = "manual")
                                                val recordSource: String = "manual",
    @ColumnInfo(name = "deleted_at") val deletedAt: String? = null,
    @ColumnInfo(name = "revision",      defaultValue = "1")
                                                val revision: Int = 1,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "inferred_category", defaultValue = "0")
                                                val inferredCategory: Int = 0,
    @ColumnInfo(name = "inference_source") val inferenceSource: String? = null,
    @ColumnInfo(name = "semantic_hash") val semanticHash: String? = null,
    @ColumnInfo(name = "institution_id", defaultValue = "mpesa")
                                                val institutionId: String = "mpesa",
    @ColumnInfo(name = "external_ref") val externalRef: String? = null,
    @ColumnInfo(name = "currency",      defaultValue = "KES")
                                                val currency: String = "KES",
    @ColumnInfo(name = "raw_sender",    defaultValue = "")
                                                val rawSender: String = "",
)
