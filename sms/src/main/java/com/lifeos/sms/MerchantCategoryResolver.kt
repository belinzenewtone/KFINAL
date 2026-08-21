package com.lifeos.sms

/**
 * Resolves a merchant or counterparty name to a granular spending category using
 * Safaricom/Kenya-specific keyword patterns.
 *
 * Resolution priority (most-specific first):
 *   GOVERNMENT > HEALTH > INSURANCE > EDUCATION > UTILITIES > HOUSING > LOANS >
 *   BANKING > FOOD > GROCERIES > FUEL > TRANSPORT > TELECOMS > ENTERTAINMENT >
 *   SHOPPING > OTHER
 *
 * Used by [DbWriter] as an in-memory fallback before [SmsParserConfig.refineAppCategory],
 * providing 14 granular categories that map to the app's spending analytics.
 */
internal object MerchantCategoryResolver {
    enum class Category(val label: String) {
        GOVERNMENT("Government & Tax"),
        HEALTH("Health & Pharmacy"),
        INSURANCE("Insurance"),
        EDUCATION("Education"),
        UTILITIES("Utilities"),
        HOUSING("Housing & Rent"),
        LOANS("Loans & Credit"),
        BANKING("Banking & Financial"),
        FOOD("Food & Restaurants"),
        GROCERIES("Groceries & Supermarket"),
        FUEL("Fuel & Gas"),
        TRANSPORT("Transport & Ride-hailing"),
        TELECOMS("Telecoms & Internet"),
        ENTERTAINMENT("Entertainment"),
        SHOPPING("Shopping"),
        OTHER("Other"),
    }

    private data class Rule(val pattern: Regex, val category: Category)

    private val RULES = listOf(
        Rule(
            Regex("""(?i)\b(?:kra|kenya\s+revenue|ntsa|helb|nssf|nhif|county|ecitizen|huduma|judiciary|ministry|lands|nairobi\s+city|tax|duty|permit|government)\b"""),
            Category.GOVERNMENT,
        ),
        Rule(
            Regex("""(?i)\b(?:pharmacy|chemist|hospital|clinic|medical|health|dispensary|lab(?:oratory)?|nhif|dentist|doctor|optician|pathology|radiology)\b"""),
            Category.HEALTH,
        ),
        Rule(
            Regex("""(?i)\b(?:insurance|assurance|jubilee|aar|britam|cic\s|madison|old\s+mutual|pioneer|resolution|amaco|kenindia|premium|policy|cover)\b"""),
            Category.INSURANCE,
        ),
        Rule(
            Regex("""(?i)\b(?:school|college|university|institute|tuition|exam|kcpe|kcse|uon\b|kenyatta\s+university|strathmore|usiu|daystar|education|fees|bursary|tvet)\b"""),
            Category.EDUCATION,
        ),
        Rule(
            Regex("""(?i)\b(?:kplc|kenya\s+power|nairobi\s+water|nwsc|water\b|electricity|power\s+token|wastewater|sewerage|garbage|zuku\s+fibre|nea\b)\b"""),
            Category.UTILITIES,
        ),
        Rule(
            Regex("""(?i)\b(?:rent\b|house\b|apartment|bedsitter|studio|landlord|caretaker|property|estate\b|plot\b|lease|tenancy|accommodation|hostel)\b"""),
            Category.HOUSING,
        ),
        Rule(
            Regex("""(?i)\b(?:loan\b|fuliza|mshwari|m-shwari|kcb\s+mpesa|kcb\s+m-pesa|tala\b|branch\b|okolea|zenka|timiza|vooma|repay|instalment|installment|credit|interest|lipa\s+mdogo)\b"""),
            Category.LOANS,
        ),
        Rule(
            Regex("""(?i)\b(?:kcb|equity\s+bank|co-?op|ncba|loop\b|absa|stanbic|stanchart|dtb\b|family\s+bank|i&m\s+bank|hf\s+group|sbm\b|prime\s+bank|bank\b|sacco|microfinance)\b"""),
            Category.BANKING,
        ),
        Rule(
            Regex("""(?i)\b(?:restaurant|cafe|kfc|java\s+house|chicken\s+inn|pizza|subway|artcaffe|big\s+square|steers|debonairs|galito|burger|grill|bistro|eatery|deli|canteen|fast\s+food)\b"""),
            Category.FOOD,
        ),
        Rule(
            Regex("""(?i)\b(?:naivas|quickmart|carrefour|cleanshelf|tuskys|chandarana|eastmatt|supermarket|hypermarket|wholesale|mini[.\-\s]?mart|groceries|fresh\s+market|fruits?\s+market)\b"""),
            Category.GROCERIES,
        ),
        Rule(
            Regex("""(?i)\b(?:shell|total\b|totalenergies|rubis|oilibya|kenol|kobil|ola\s+energy|vivo\s+energy|gulf\s+energy|petrol\b|diesel\b|fuel\b|pump\b|service\s+station)\b"""),
            Category.FUEL,
        ),
        Rule(
            Regex("""(?i)\b(?:uber\b|bolt\b|little\s+cab|swvl|faras|bus\b|shuttle|matatu|kenya\s+railways|sgr\b|taxify|indriver|boda\s+boda|ride.hailing|parking)\b"""),
            Category.TRANSPORT,
        ),
        Rule(
            Regex("""(?i)\b(?:safaricom|airtel|telkom|faiba|zuku\b|liquid\s+telecom|jamii\s+telecom|wananchi|internet|broadband|fibre|data\s+bundle|airtime)\b"""),
            Category.TELECOMS,
        ),
        Rule(
            Regex("""(?i)\b(?:netflix|spotify|youtube|showmax|dstv|gotv|startimes|binge|canal\b|cinema|imax|anga\b|cinemax|movie|concert|ticket|gaming|steam|playstation|prime\s+video)\b"""),
            Category.ENTERTAINMENT,
        ),
        Rule(
            Regex("""(?i)\b(?:jumia|kilimall|masoko|amazon|alibaba|shein|fashion\b|clothing|shoes\b|electronics|phone\s+shop|laptop\s+shop|mall\b|boutique|online\s+shop)\b"""),
            Category.SHOPPING,
        ),
    )

    /**
     * Resolve [counterparty] to the most-specific matching [Category].
     * Returns [Category.OTHER] when no rule matches, or null when [counterparty] is blank.
     */
    fun resolve(counterparty: String?): Category? {
        if (counterparty.isNullOrBlank()) return null
        val normalized = counterparty.trim()
        return RULES.firstOrNull { it.pattern.containsMatchIn(normalized) }?.category
            ?: Category.OTHER
    }

    /** Convenience: resolve to the category's string label (for DB insertion). */
    fun resolveLabel(counterparty: String?): String? = resolve(counterparty)?.label
}
