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

    @ColumnInfo(name = "amount")                val amount: Double,
    @ColumnInfo(name = "merchant")              val merchant: String?,
    @ColumnInfo(name = "category")              val category: String?,
    @ColumnInfo(name = "date")                  val date: String?,
    @ColumnInfo(name = "source")                val source: String?,
    @ColumnInfo(name = "transaction_type")      val transactionType: String?,
    @ColumnInfo(name = "mpesa_code")            val mpesaCode: String?,
    @ColumnInfo(name = "source_hash")           val sourceHash: String?,
    @ColumnInfo(name = "raw_sms")               val rawSms: String?,
    @ColumnInfo(name = "description")           val description: String?,
    @ColumnInfo(name = "notes")                 val notes: String?,
    @ColumnInfo(name = "balance_after")         val balanceAfter: Double?,
    @ColumnInfo(name = "fee")                   val fee: Double?,
    @ColumnInfo(name = "status",        defaultValue = "completed")
                                                val status: String,
    @ColumnInfo(name = "created_at")            val createdAt: String?,
    @ColumnInfo(name = "updated_at")            val updatedAt: String?,
    @ColumnInfo(name = "sync_state",    defaultValue = "pending")
                                                val syncState: String,
    @ColumnInfo(name = "record_source", defaultValue = "manual")
                                                val recordSource: String,
    @ColumnInfo(name = "deleted_at")            val deletedAt: String?,
    @ColumnInfo(name = "revision",      defaultValue = "1")
                                                val revision: Int,
    @ColumnInfo(name = "user_id")               val userId: String?,
    @ColumnInfo(name = "inferred_category", defaultValue = "0")
                                                val inferredCategory: Int,
    @ColumnInfo(name = "inference_source")      val inferenceSource: String?,
    @ColumnInfo(name = "semantic_hash")         val semanticHash: String?,
    @ColumnInfo(name = "institution_id", defaultValue = "mpesa")
                                                val institutionId: String,
    @ColumnInfo(name = "external_ref")          val externalRef: String?,
    @ColumnInfo(name = "currency",      defaultValue = "KES")
                                                val currency: String,
    @ColumnInfo(name = "raw_sender",    defaultValue = "")
                                                val rawSender: String,
)
