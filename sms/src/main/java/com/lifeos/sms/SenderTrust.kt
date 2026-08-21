package com.lifeos.sms

/**
 * Trust level for the sender of an incoming SMS, derived from the sender ID string.
 *
 * Levels are assigned by matching the sender ID against the institution whitelist
 * in [InstitutionDetector]. If no institution matches, the sender is UNKNOWN — parsed
 * results from UNKNOWN senders are still accepted but carry lower inherent trust and
 * should not bypass review for high-value transactions.
 */
enum class SenderTrust {
    OFFICIAL_MPESA,
    AIRTEL_MONEY,
    BANK,
    UNKNOWN;

    companion object {
        fun classify(sender: String?): SenderTrust {
            if (sender.isNullOrBlank()) return UNKNOWN
            val detection = InstitutionDetector.detect(sender, "")
            return when (detection?.institutionId) {
                "mpesa"  -> OFFICIAL_MPESA
                "airtel" -> AIRTEL_MONEY
                null     -> UNKNOWN
                else     -> BANK
            }
        }
    }
}
