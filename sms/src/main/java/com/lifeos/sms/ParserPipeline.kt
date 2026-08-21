package com.lifeos.sms

object ParserPipeline {
    private val specificParsers: Map<String, FinancialSmsParser> = listOf(
        MpesaParser,
        AirtelMoneyParser,
    ).associateBy { it.institutionId }

    private val specificParserList: Collection<FinancialSmsParser> = specificParsers.values

    fun process(body: String, sender: String, receivedAtMs: Long): SmsParser.SmsParseResult {
        val detection = InstitutionDetector.detect(sender, body)
        val raw: SmsParser.SmsParseResult

        if (detection != null) {
            // 1. Try exact-match specific parser (M-Pesa, Airtel Money)
            val specific = specificParsers[detection.institutionId]
            if (specific != null && specific.canParse(body, sender)) {
                raw = specific.parse(body, sender, receivedAtMs)
                return postProcess(raw, body)
            }

            // 2. Semantic fallback for all other banks (KCB, Equity, Co-op, NCBA…)
            //    Skip non-transactional SMS (OTPs, maintenance, promos) before parsing.
            if (GenericBankParser.canParse(detection)) {
                if (SmsParserConfig.isServiceNotice(body)) {
                    return SmsParser.SmsParseResult.Error(
                        SmsParser.SmsParseError("service_notice", body)
                    )
                }
                raw = GenericBankParser.parseWithDetection(body, sender, receivedAtMs, detection)
                return postProcess(raw, body)
            }
        }

        // 3. Body-keyword last resort — catches edge cases where the sender ID
        // wasn't recognised but the body clearly belongs to a known parser
        for (parser in specificParserList) {
            if (parser.canParse(body, sender)) {
                raw = parser.parse(body, sender, receivedAtMs)
                return postProcess(raw, body)
            }
        }

        return SmsParser.SmsParseResult.Error(
            SmsParser.SmsParseError("not_financial", body)
        )
    }

    /**
     * Post-parse verification:
     *  1. [CrossParserVoter] — for M-Pesa results, cross-check with [SimpleMpesaParser]
     *     and demote DIRECT → REVIEW on category or amount disagreement.
     *  2. [TransactionDecisionTree] — demote HIGH-confidence results to REVIEW when
     *     raw-body structural features don't support it.
     */
    private fun postProcess(result: SmsParser.SmsParseResult, body: String): SmsParser.SmsParseResult {
        if (result !is SmsParser.SmsParseResult.Success) return result
        var tx = result.transaction
        if (tx.institutionId == "mpesa") {
            tx = CrossParserVoter.applyVote(tx, body)
        }
        if (TransactionDecisionTree.shouldDemote(body, tx)) {
            tx = tx.copy(parseRoute = SmsParser.ParseRoute.REVIEW)
        }
        return if (tx === result.transaction) result else SmsParser.SmsParseResult.Success(tx)
    }
}
