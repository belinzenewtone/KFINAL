package com.belinze.lifeos.data.db.dao

import com.belinze.lifeos.data.db.entity.*

/**
 * DAO for tables adjacent to the SMS pipeline.
 *
 * `import_audit` and `sms_ingest_queue` are intentionally absent: they are
 * owned by the untouched parser (DbWriter) and accessed via SmsService.
 */
interface SmsDao {
    // ─── Merchant categories ──────────────────────────────────────────────────

        suspend fun getMerchantCategory(merchant: String): MerchantCategoryEntity?

        suspend fun upsertMerchantCategory(entity: MerchantCategoryEntity)

    // ─── Paybill registry ─────────────────────────────────────────────────────

        suspend fun getPaybillName(paybillNumber: String): String?

        suspend fun getTopPaybills(limit: Int): List<PaybillRegistryEntity>

        suspend fun upsertPaybillUsage(
        paybillNumber: String,
        displayName: String,
        lastSeenAt: String,
        lastAmountKes: Double?,
    )

    // ─── ML training samples ──────────────────────────────────────────────────

        suspend fun insertSample(sample: MlTrainingSampleEntity)

        suspend fun countSamples(): Int

        suspend fun getSamples(limit: Int): List<MlTrainingSampleEntity>
}
