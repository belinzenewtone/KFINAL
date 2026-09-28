package com.belinze.lifeos.data.db.entity

/**
 * ML training samples for the on-device category classifier.
 * Classifier activates once ≥ 50 samples exist; training reads up to 2000 rows.
 */
data class MlTrainingSampleEntity(
    val id: Long = 0,
    /** JSON feature vector */
    val features: String,
    val label: String,
    val recordedAt: String?,
)
