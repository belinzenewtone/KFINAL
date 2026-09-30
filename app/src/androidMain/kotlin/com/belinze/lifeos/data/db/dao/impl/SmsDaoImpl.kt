package com.belinze.lifeos.data.db.dao.impl

import com.belinze.lifeos.data.db.MerchantCategoryQueries
import com.belinze.lifeos.data.db.Merchant_categories
import com.belinze.lifeos.data.db.MlTrainingSampleQueries
import com.belinze.lifeos.data.db.Ml_training_samples
import com.belinze.lifeos.data.db.PaybillRegistryQueries
import com.belinze.lifeos.data.db.Paybill_registry
import com.belinze.lifeos.data.db.dao.SmsDao
import com.belinze.lifeos.data.db.entity.MerchantCategoryEntity
import com.belinze.lifeos.data.db.entity.MlTrainingSampleEntity
import com.belinze.lifeos.data.db.entity.PaybillRegistryEntity

class SmsDaoImpl(
    private val merchantQ: MerchantCategoryQueries,
    private val paybillQ: PaybillRegistryQueries,
    private val mlQ: MlTrainingSampleQueries,
) : SmsDao {
    override suspend fun getMerchantCategory(merchant: String): MerchantCategoryEntity? =
        merchantQ.getMerchantCategory(merchant).executeAsOneOrNull()?.toEntity()

    override suspend fun upsertMerchantCategory(entity: MerchantCategoryEntity) =
        merchantQ.upsertMerchantCategory(
            id           = entity.id,
            merchant     = entity.merchant,
            category     = entity.category,
            confidence   = entity.confidence,
            userCorrected = entity.userCorrected.toLong(),
            createdAt    = entity.createdAt,
            updatedAt    = entity.updatedAt,
            syncState    = entity.syncState,
            recordSource = entity.recordSource,
            deletedAt    = entity.deletedAt,
            revision     = entity.revision.toLong(),
            userId       = entity.userId,
        )

    override suspend fun getPaybillName(paybillNumber: String): String? =
        paybillQ.getPaybillName(paybillNumber).executeAsOneOrNull()?.display_name

    override suspend fun getTopPaybills(limit: Int): List<PaybillRegistryEntity> =
        paybillQ.getTopPaybills(limit.toLong()).executeAsList().map { it.toEntity() }

    override suspend fun upsertPaybillUsage(
        paybillNumber: String,
        displayName: String,
        lastSeenAt: String,
        lastAmountKes: Double?,
    ) = paybillQ.upsertPaybillUsage(paybillNumber, displayName, lastSeenAt, lastAmountKes)

    override suspend fun insertSample(sample: MlTrainingSampleEntity) =
        mlQ.insertSample(sample.features, sample.label, sample.recordedAt)

    override suspend fun countSamples(): Int =
        mlQ.countSamples().executeAsOne().toInt()

    override suspend fun getSamples(limit: Int): List<MlTrainingSampleEntity> =
        mlQ.getSamples(limit.toLong()).executeAsList().map { it.toEntity() }
}

private fun Merchant_categories.toEntity() = MerchantCategoryEntity(
    id           = id,
    merchant     = merchant,
    category     = category,
    confidence   = confidence,
    userCorrected = user_corrected.toInt(),
    createdAt    = created_at,
    updatedAt    = updated_at,
    syncState    = sync_state,
    recordSource = record_source,
    deletedAt    = deleted_at,
    revision     = revision.toInt(),
    userId       = user_id,
)

private fun Paybill_registry.toEntity() = PaybillRegistryEntity(
    paybillNumber = paybill_number,
    displayName   = display_name,
    lastSeenAt    = last_seen_at,
    usageCount    = usage_count.toInt(),
    lastAmountKes = last_amount_kes,
    userId        = user_id,
)

private fun Ml_training_samples.toEntity() = MlTrainingSampleEntity(
    id         = id,
    features   = features,
    label      = label,
    recordedAt = recorded_at,
)
