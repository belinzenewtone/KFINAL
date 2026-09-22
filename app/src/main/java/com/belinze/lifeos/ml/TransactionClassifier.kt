package com.belinze.lifeos.ml

import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.db.dao.SmsDao
import com.belinze.lifeos.data.db.entity.MlTrainingSampleEntity
import com.belinze.lifeos.data.db.entity.TransactionEntity
import com.belinze.lifeos.util.nowIso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ClassificationResult(val label: String, val confidence: Double)

@Singleton
class TransactionClassifier @Inject constructor(
    private val smsDao:      SmsDao,
    private val preferences: AppPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val MIN_SAMPLES          = 50
    private val CONFIDENCE_THRESHOLD = 0.65

    @Volatile
    private var tree: TreeNode? = null

    init {
        scope.launch { loadOrRetrain() }
    }

    /** Predict category for a transaction. Returns null when model is not trained
     *  or confidence is below threshold. */
    fun classify(tx: TransactionEntity): ClassificationResult? {
        val t = tree ?: return null
        val features = FeatureExtractor.extract(tx)
        val (label, confidence) = DecisionTree.predictTree(t, features)
        if (confidence < CONFIDENCE_THRESHOLD) return null
        return ClassificationResult(label = label, confidence = confidence)
    }

    /** Record a user correction and trigger retrain once enough samples accumulate. */
    suspend fun recordCorrection(tx: TransactionEntity, correctLabel: String) {
        withContext(Dispatchers.IO) {
            val features = FeatureExtractor.toJson(FeatureExtractor.extract(tx))
            smsDao.insertSample(
                MlTrainingSampleEntity(
                    features   = features,
                    label      = correctLabel,
                    recordedAt = nowIso(),
                )
            )
            if (smsDao.countSamples() >= MIN_SAMPLES) retrain()
        }
    }

    private suspend fun loadOrRetrain() {
        val json = preferences.getMLTree()
        if (!json.isNullOrBlank()) {
            tree = DecisionTree.fromJson(json)
        }
        if (tree == null && smsDao.countSamples() >= MIN_SAMPLES) {
            retrain()
        }
    }

    private suspend fun retrain() {
        val samples = smsDao.getSamples(2_000)
        if (samples.size < MIN_SAMPLES) return
        val pairs = samples.mapNotNull { s ->
            val f = FeatureExtractor.fromJson(s.features)
            if (f.isEmpty()) null else f to s.label
        }
        val newTree = withContext(Dispatchers.Default) {
            DecisionTree.trainTree(pairs)
        }
        tree = newTree
        preferences.setMLTree(DecisionTree.toJson(newTree))
    }
}
