package com.belinze.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ML training samples for the on-device category classifier.
 * Classifier activates once ≥ 50 samples exist; training reads up to 2000 rows.
 */
@Entity(tableName = "ml_training_samples")
data class MlTrainingSampleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    /** JSON feature vector */
    @ColumnInfo(name = "features") val features: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "recorded_at") val recordedAt: String?,
)
