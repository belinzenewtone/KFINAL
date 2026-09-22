package com.belinze.lifeos.ml

import org.json.JSONObject

internal sealed class TreeNode {
    data class Leaf(
        val label:      String,
        val confidence: Double,
        val count:      Int,
    ) : TreeNode()

    data class Split(
        val feature:   String,
        val threshold: Double,
        val left:      TreeNode,
        val right:     TreeNode,
    ) : TreeNode()
}

internal object DecisionTree {
    private const val MAX_DEPTH         = 6
    private const val MIN_SAMPLES_SPLIT = 4

    // ── Training ──────────────────────────────────────────────────────────────

    /** Build a CART decision tree from (features, label) pairs. */
    fun trainTree(samples: List<Pair<Map<String, Double>, String>>): TreeNode =
        buildNode(samples, depth = 0)

    // ── Inference ─────────────────────────────────────────────────────────────

    /** Traverse the tree and return (predicted label, confidence). */
    fun predictTree(node: TreeNode, features: Map<String, Double>): Pair<String, Double> =
        when (node) {
            is TreeNode.Leaf  -> node.label to node.confidence
            is TreeNode.Split -> {
                val v = features[node.feature] ?: 0.0
                if (v <= node.threshold) { predictTree(node.left, features) }
                else { predictTree(node.right, features) }
            }
        }

    // ── Serialization ─────────────────────────────────────────────────────────

    fun toJson(node: TreeNode): String = when (node) {
        is TreeNode.Leaf  ->
            """{"t":"L","l":"${node.label.replace("\"", "\\\"")}","c":${node.confidence},"n":${node.count}}"""
        is TreeNode.Split ->
            """{"t":"S","f":"${node.feature}","th":${node.threshold},"L":${toJson(node.left)},"R":${toJson(node.right)}}"""
    }

    fun fromJson(json: String): TreeNode? =
        runCatching { parseNode(JSONObject(json)) }.getOrNull()

    private fun parseNode(obj: JSONObject): TreeNode = when (obj.getString("t")) {
        "L" -> TreeNode.Leaf(
            label      = obj.getString("l"),
            confidence = obj.getDouble("c"),
            count      = obj.getInt("n"),
        )
        "S" -> TreeNode.Split(
            feature   = obj.getString("f"),
            threshold = obj.getDouble("th"),
            left      = parseNode(obj.getJSONObject("L")),
            right     = parseNode(obj.getJSONObject("R")),
        )
        else -> error("Unknown node type: ${obj.getString("t")}")
    }

    // ── Internal build ────────────────────────────────────────────────────────

    private fun buildNode(
        samples: List<Pair<Map<String, Double>, String>>,
        depth:   Int,
    ): TreeNode {
        val labelCounts = samples.groupingBy { it.second }.eachCount()
        val (majorityLabel, majorityCount) = labelCounts.maxByOrNull { it.value }!!
        val n = samples.size

        if (depth >= MAX_DEPTH || n < MIN_SAMPLES_SPLIT || labelCounts.size == 1) {
            return TreeNode.Leaf(
                label      = majorityLabel,
                confidence = majorityCount.toDouble() / n,
                count      = n,
            )
        }

        val best = findBestSplit(samples) ?: return TreeNode.Leaf(
            label      = majorityLabel,
            confidence = majorityCount.toDouble() / n,
            count      = n,
        )

        return TreeNode.Split(
            feature   = best.feature,
            threshold = best.threshold,
            left      = buildNode(best.left,  depth + 1),
            right     = buildNode(best.right, depth + 1),
        )
    }

    private data class SplitCandidate(
        val feature:   String,
        val threshold: Double,
        val gini:      Double,
        val left:      List<Pair<Map<String, Double>, String>>,
        val right:     List<Pair<Map<String, Double>, String>>,
    )

    private fun findBestSplit(
        samples: List<Pair<Map<String, Double>, String>>,
    ): SplitCandidate? {
        val features = samples.first().first.keys
        var best: SplitCandidate? = null

        for (feature in features) {
            val values = samples.map { it.first[feature] ?: 0.0 }.sorted().distinct()
            for (i in 0 until values.size - 1) {
                val threshold = (values[i] + values[i + 1]) / 2.0
                val left  = samples.filter { (it.first[feature] ?: 0.0) <= threshold }
                val right = samples.filter { (it.first[feature] ?: 0.0) > threshold }
                if (left.isEmpty() || right.isEmpty()) continue
                val gini = weightedGini(left, right)
                if (best == null || gini < best.gini) {
                    best = SplitCandidate(feature, threshold, gini, left, right)
                }
            }
        }
        return best
    }

    private fun gini(samples: List<Pair<Map<String, Double>, String>>): Double {
        if (samples.isEmpty()) return 0.0
        val n = samples.size.toDouble()
        return 1.0 - samples.groupingBy { it.second }
            .eachCount().values
            .sumOf { c -> (c / n) * (c / n) }
    }

    private fun weightedGini(
        left:  List<Pair<Map<String, Double>, String>>,
        right: List<Pair<Map<String, Double>, String>>,
    ): Double {
        val n = (left.size + right.size).toDouble()
        return (left.size / n) * gini(left) + (right.size / n) * gini(right)
    }
}
