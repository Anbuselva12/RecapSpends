package com.example.data.notifications

import kotlin.math.roundToLong

data class ParsedExpenseAlert(
    val title: String,
    val amount: Double,
    val dateEpochMs: Long,
    val paymentMode: String,
    val suggestedCategoryId: String,
    val suggestedSubcategory: String,
    val fingerprint: String,
    val snippet: String,
    val entryType: String = "expense"
)

object MessageExpenseParser {

    // Reject OTPs, security/verification codes, PINs immediately in memory
    private val otpOrSecurityRegex = Regex(
        """\b(otp|one[\s-]*time[\s-]*password|verification[\s-]*code|security[\s-]*code|auth(?:entication)?[\s-]*code|passcode|login[\s-]*code|cvv|mpin|tpin|upi[\s-]*pin|secret[\s-]*code|do[\s-]*not[\s-]*share|never[\s-]*share|don'?t[\s-]*share)\b""",
        RegexOption.IGNORE_CASE
    )

    private val otpValidityRegex = Regex(
        """\b(is your|use code|enter code|pin is|valid for \d+\s*(min|mins|minute|minutes|sec|secs|second|seconds))\b""",
        RegexOption.IGNORE_CASE
    )

    // Strip standard bank fraud-report / dispute / block footers before spend & merchant checks
    private val fraudFooterRegex = Regex(
        """(?:trxn\.?|txn\.?|transaction)?\s*(?:if\s+)?not\s+(?:done|initiated|authorized|authorised|made)?\s*by\s+(?:you|u)\b.*$|""" +
            """\bif\s+not\s+(?:you|u)\b.*$|""" +
            """\bnot\s+(?:you|u)\s*[?,].*$|""" +
            """\bto\s+(?:report|dispute|block)\b.*$|""" +
            """\bsms\s+block\b.*$|""" +
            """\breport\s+at\b.*$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Strip available balance & credit limit tails so they are never misparsed as transaction amount
    private val balanceClauseRegex = Regex(
        """(?:(?:avl|avail(?:able)?|curr(?:ent)?|total|clr|clear|closing|ledg(?:er)?|updated|net)\s*\.?\s*(?:cr(?:edit)?\.?\s*)?(?:bal(?:ance)?|lmt|limit)|(?:\b(?:bal|lmt)\s*[:-]))\s*(?:on\s+[^0-9\n]{1,40})?(?:is|:|-)?\s*(?:rs\.?|inr|₹)?\s*[0-9,]+(?:\.[0-9]{1,2})?(?:\s*/-)?""",
        RegexOption.IGNORE_CASE
    )

    // Always reject regardless of whether "debited" or "credited" appears anywhere in the text
    private val hardNonTransactionRegex = Regex(
        """\b(declined|failed|unsuccessful|rejected|bounced|dishonoured|dishonored|blocked|insufficient\s+(?:bal|balance|fund|funds)|cancelled|canceled|timed\s*out|reversed|reversal|""" +
            """requesting|has\s+requested|requested\s+(?:rs\.?|inr|₹)|collect\s+request|approve\s+to\s+pay|wants\s+to\s+collect|""" +
            """mandate\s+(?:created|registered|approved|modified|paused|revoked|cancelled|canceled|declined|failed|expired)|""" +
            """autopay\s+(?:created|registered|set\s*up|modified|paused|revoked|cancelled|canceled|declined|failed)|""" +
            """will\s+be\s+debited|to\s+be\s+debited|upcoming\s+autopay|scheduled\s+for|""" +
            """statement\s+generated|e-?statement|total\s+amount\s+due|total\s+due|minimum\s+amount\s+due|min\.?\s*due|payment\s+due|bill\s+is\s+due|due\s+on|due\s+date|payable\s+by|pay\s+before|""" +
            """available\s+balance\s+is|avl\.?\s*bal\.?\s*is|your\s+(?:a/c|account)\s+balance|mini\s+statement|""" +
            """pre-?approved|convert\s+to\s+emi|emi\s+offer|credit\s+limit\s+increased|limit\s+enhancement|apply\s+now|""" +
            """payment\s+(?:.{0,45}?\s+)?received|payment\s+received\s+towards|thank(?:s|\s+you)\s+for\s+(?:your\s+)?payment)\b""",
        RegexOption.IGNORE_CASE
    )

    // Credits/deposits (income) vs outgoing bank debits mentioning recipient credit
    private val creditWordRegex = Regex(
        """\b(credited|deposited|refund(?:ed)?|cashback|salary\s+credited|dividend|interest\s+credited|neft\s+cr|imps\s+cr|rtgs\s+cr|upi\s+cr|cr\.?\s+to\s+(?:your|ur|a/c|acct|account))\b""",
        RegexOption.IGNORE_CASE
    )
    private val creditToOwnAccountRegex = Regex(
        """\b(?:is|has\s+been)?\s*(?:credited|deposited|refunded)\s+(?:to|in|into)\s+(?:your|ur|a/c|acct|account|card|wallet)\b|""" +
            """\b(?:a/c|acct|account|card)[^.]{0,30}\b(?:is|has\s+been)?\s*(?:credited|deposited)\b|""" +
            """\b(cashback|salary\s+credited|dividend|interest\s+credited|neft\s+cr|imps\s+cr|rtgs\s+cr|upi\s+cr)\b""",
        RegexOption.IGNORE_CASE
    )

    private val debitTriggerRegex = Regex(
        """\b(debited|debit\s+(?:of|by)|dr\.?\s+(?:from|by|of|a/c)|spent|paid|sent|transferred|trf\s+to|transfer\s+to|withdrawn|withdrawal|charged|purchase|purchased|used\s+(?:for|at)|swiped|billed|txn\s+(?:of|done)|transaction\s+(?:of|done)|payment\s+(?:of|done)|autopay|auto-?debit|e-?mandate|standing\s+instruction|si\s+executed|deducted|ecom|pos\s+txn)\b""",
        RegexOption.IGNORE_CASE
    )

    private val currencyPrefixRegex = Regex(
        """(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)(?:\s*/-)?""",
        RegexOption.IGNORE_CASE
    )

    private val currencySuffixRegex = Regex(
        """([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:rs\.?|inr)\b""",
        RegexOption.IGNORE_CASE
    )

    // Fallback when Indian bank SMS omits "Rs"/"INR" (e.g. SBI UPI: "A/C X9267 debited by 5680.0 on date...")
    private val verbAmountRegex = Regex(
        """\b(?:debited\s+(?:by|for|with|of)|credited\s+(?:by|with|for|of)|debit\s+(?:of|by)|credit\s+(?:of|by)|spent|withdrawn|deducted|paid|charged|deposited|refunded|purchase\s+of|txn\s+of|transaction\s+of)\s+(?:by|for|of|with)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)(?!-\d{2})\b""",
        RegexOption.IGNORE_CASE
    )

    private val cardOrAccountHolderNoiseRegex = Regex(
        """^(?:your|ur|the|a/c|acct|account|card|sbi|hdfc|icici|axis|kotak|pnb|bob|indusind|idfc|yes|rupay|visa|mastercard|ending|xx+|rs\.?|inr|upi|pos|atm|ecom|unknown|date)$""",
        RegexOption.IGNORE_CASE
    )

    // Specific Indian bank merchant extraction patterns (ordered from most specific to general)
    private val commonMerchantPatterns = listOf(
        // SBI UPI: "trf to AMAZON SELLER Refno ..." or "transfer to Rahul"
        Regex("""\b(?:trf|transfer|transferred)\s+to\s+([A-Za-z0-9@_.+][A-Za-z0-9@_.+ &'\-/]{1,80})""", RegexOption.IGNORE_CASE),
        // Credit/deposit sender: "by UPI-SENDER-vpa"
        Regex("""\bby\s+UPI[-/]([A-Za-z0-9 &'.]{2,60})(?:[-/]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bInfo\s*[:\-]\s*(?:UPI|IMPS|NEFT|RTGS)[/-](?:(?:P2[AM]|DR|CR)[/-])?(?:[0-9A-Za-z]{4,}[/-])+([A-Za-z][A-Za-z0-9@_.+ &'\-]{1,60})""", RegexOption.IGNORE_CASE),
        Regex("""\bUPI/(?:(?:P2[AM]|DR|CR)/)?(?:[0-9]{4,}/)?([A-Za-z][A-Za-z0-9_.+ &'\-]{1,55})(?:/|$)""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:IMPS|NEFT|RTGS)/[A-Z0-9]+/(?:[0-9]+/)?([A-Za-z][A-Za-z0-9_.+ &'\-]{1,55})(?:/|$)""", RegexOption.IGNORE_CASE)
    )

    private val expenseMerchantPatterns = commonMerchantPatterns + listOf(
        // Paid/sent to VPA or merchant
        Regex("""\b(?:paid\s+to|sent\s+to|to\s+vpa|vpa|merchant)\s+([A-Za-z0-9@_.+][A-Za-z0-9@_.+ &'\-/]{1,80})""", RegexOption.IGNORE_CASE),
        // Card / POS / UPI spend: "at sbipmopad0222119002854 on 15-11-26 via UPI" or "at AMAZON PAY*..."
        Regex("""\bat\s+([A-Za-z0-9@_.+][A-Za-z0-9@_.+ &'\-*/]{1,80})""", RegexOption.IGNORE_CASE),
        // "towards Netflix India" / "to SWIGGY"
        Regex("""\b(?:towards|to)\s+([A-Za-z0-9@_.+][A-Za-z0-9@_.+ &'\-/]{1,80})""", RegexOption.IGNORE_CASE),
        // ICICI Card fallback: "on 15-Nov-26 on SWIGGY." (not a date or bank/card phrase)
        Regex("""\bon\s+(?!\d{1,2}[-/\s](?:\d{1,2}|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec))([A-Za-z][A-Za-z0-9@_.+ &'\-*/]{1,60})""", RegexOption.IGNORE_CASE)
    )

    private val incomeMerchantPatterns = commonMerchantPatterns + listOf(
        // "from ACME CORP" / "received from ..." / "from Rahul"
        Regex("""\b(?:received\s+from|credited\s+from|from)\s+([A-Za-z0-9@_.+][A-Za-z0-9@_.+ &'\-/]{1,80})""", RegexOption.IGNORE_CASE),
        // "towards salary"
        Regex("""\btowards\s+([A-Za-z0-9@_.+][A-Za-z0-9@_.+ &'\-/]{1,80})""", RegexOption.IGNORE_CASE)
    )

    private val merchantTrailStopRegex = Regex(
        """\s+(?:\b(?:on|dt|date|via|using|from|for|ref(?:\s*no)?|refno|utr|rrn|upi|info|avl|avail|bal|lmt|limit|if|not|trxn|txn|call|block|your|ur)\b|\(|\b\d{1,2}[-/]\d{1,2}[-/]\d{2,4}\b).*$""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        senderTitle: String,
        body: String,
        timestampMs: Long = System.currentTimeMillis()
    ): ParsedExpenseAlert? {
        val cleanSenderInput = normalizeText(senderTitle)
        val cleanBodyInput = normalizeText(body)
        val combinedRaw = listOf(cleanSenderInput, cleanBodyInput)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (combinedRaw.isBlank()) return null

        // Immediate in-memory OTP & verification code rejection: never stored or logged.
        if (otpOrSecurityRegex.containsMatchIn(combinedRaw) || otpValidityRegex.containsMatchIn(combinedRaw)) {
            return null
        }

        // Strip trailing fraud disclaimers and balance/limit tails before spend checks and extraction
        val bodyWithoutFooter = fraudFooterRegex.replace(cleanBodyInput, " ").trim()
        val sanitizedForAmount = balanceClauseRegex.replace(bodyWithoutFooter, " ").trim()
        val fullSanitized = "$cleanSenderInput $sanitizedForAmount".trim()

        // Hard reject failures, collect requests, mandate setups, bill due reminders, balance enquiries, promos, card payment acks
        if (hardNonTransactionRegex.containsMatchIn(fullSanitized)) {
            return null
        }

        val hasDebit = sanitizedForAmount.contains("debited", ignoreCase = true) ||
            sanitizedForAmount.contains("deducted", ignoreCase = true) ||
            debitTriggerRegex.containsMatchIn(sanitizedForAmount)
        val hasCreditWord = creditWordRegex.containsMatchIn(fullSanitized)
        val isCreditToOwnAccount = creditToOwnAccountRegex.containsMatchIn(sanitizedForAmount)

        val isIncome: Boolean = when {
            isCreditToOwnAccount -> true
            hasCreditWord && !hasDebit -> true
            hasDebit -> false
            else -> return null
        }

        val amount = extractAmount(sanitizedForAmount) ?: extractAmount(cleanSenderInput) ?: return null
        if (amount <= 0.0 || amount > 10_000_000.0) return null

        val entryType = if (isIncome) "income" else "expense"
        val paymentMode = detectPaymentMode(fullSanitized)
        val merchant = extractMerchant(cleanSenderInput, sanitizedForAmount, isIncome)
        val (categoryId, subcategory) = suggestCategory(fullSanitized, merchant, isIncome)

        val bucket = (timestampMs / 120_000L)
        val fingerprint = "$entryType|${(amount * 100.0).roundToLong()}|${merchant.lowercase()}|$bucket"
        val cleanSnippet = combinedRaw.replace(Regex("\\s+"), " ").trim().take(220)

        return ParsedExpenseAlert(
            title = merchant,
            amount = amount,
            dateEpochMs = timestampMs,
            paymentMode = paymentMode,
            suggestedCategoryId = categoryId,
            suggestedSubcategory = subcategory,
            fingerprint = fingerprint,
            snippet = cleanSnippet,
            entryType = entryType
        )
    }

    private fun normalizeText(input: String): String =
        input
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace("\uFEFF", "")
            .replace('\u00A0', ' ')
            .replace('\u2007', ' ')
            .replace('\u202F', ' ')
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex(" {2,}"), " ")
            .trim()

    private fun extractAmount(text: String): Double? {
        val match = currencyPrefixRegex.find(text)
            ?: currencySuffixRegex.find(text)
            ?: verbAmountRegex.find(text)
            ?: return null
        val rawNum = match.groupValues.getOrNull(1)?.replace(",", "") ?: return null
        return rawNum.toDoubleOrNull()
    }

    private fun detectPaymentMode(text: String): String {
        val lower = text.lowercase()
        val hasUpiKeyword = listOf(
            "upi", "gpay", "google pay", "phonepe", "paytm", "bhim",
            "vpa", "@ok", "@ybl", "@ibl", "@axl", "@sbi", "@hdfcbank", "@icici", "@paytm"
        ).any { it in lower } || Regex("""\bcred\b""").containsMatchIn(lower)
        return when {
            hasUpiKeyword -> "UPI"
            listOf(
                "credit card", "debit card", "card xx", "card ending", "ending with",
                "visa", "mastercard", "rupay", "amex", "pos ", "atm ", "ecom"
            ).any { it in lower } -> "Card"
            listOf(
                "neft", "imps", "rtgs", "netbanking", "nach", "ecs", "ach",
                "a/c", "acct", "account", "bank"
            ).any { it in lower } -> "Bank"
            else -> "UPI"
        }
    }

    private fun extractMerchant(senderTitle: String, body: String, isIncome: Boolean): String {
        val lowerBody = body.lowercase()
        if (!isIncome && ("atm" in lowerBody || "cash withdrawal" in lowerBody) &&
            ("withdrawn" in lowerBody || "withdrawal" in lowerBody || "debited" in lowerBody)
        ) {
            return "ATM Cash Withdrawal"
        }

        val patterns = if (isIncome) incomeMerchantPatterns else expenseMerchantPatterns
        for (pattern in patterns) {
            val match = pattern.find(body) ?: continue
            val raw = match.groupValues.getOrNull(1).orEmpty()
            val cleaned = merchantTrailStopRegex.replace(raw, "")
                .replace(Regex("[*]+"), " ")
                .replace(Regex("[^A-Za-z0-9@_.+ &'.\\-]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .trimEnd('.', '-', '/', ',')
                .take(60)
            if (cleaned.length >= 2 &&
                !cardOrAccountHolderNoiseRegex.matches(cleaned) &&
                !cleaned.lowercase().startsWith("a/c") &&
                !cleaned.lowercase().startsWith("rs.") &&
                !cleaned.lowercase().startsWith("xx") &&
                !cleaned.all { it.isDigit() }
            ) {
                return cleaned
            }
        }

        if (isIncome) {
            return when {
                "salary" in lowerBody -> "Salary credit"
                "cashback" in lowerBody -> "Cashback credit"
                "refund" in lowerBody -> "Refund credit"
                "dividend" in lowerBody -> "Dividend credit"
                "interest" in lowerBody -> "Interest credit"
                else -> "Bank credit"
            }
        }

        val cleanSender = senderTitle
            .replace(Regex("^(?:AD|AX|JD|VM|VK|BZ|BP|JK|QP|CP|TM|TX|MD|IM|DM|RM|VA|BW)-", RegexOption.IGNORE_CASE), "")
            .replace(Regex("-(?:S|T|P|G)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?:alert|transaction|notification|debit|spent|paid)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(60)
        return cleanSender.ifBlank { "Transaction alert" }
    }

    private fun suggestCategory(text: String, merchant: String, isIncome: Boolean): Pair<String, String> {
        val lower = "$text $merchant".lowercase()
        if (isIncome) {
            return when {
                "salary" in lower -> "inc" to "Salary"
                "refund" in lower || "cashback" in lower -> "inc" to "Refund & cashback"
                "dividend" in lower || "interest" in lower -> "inc" to "Interest & dividend"
                "rent" in lower -> "inc" to "Rental & other"
                else -> "inc" to "Business / Freelance"
            }
        }
        return when {
            listOf(
                "swiggy", "zomato", "restaurant", "cafe", "coffee", "starbucks",
                "dominos", "pizza", "bakery", "biryani", "kfc", "mcdonald", "burger", "food"
            ).any { it in lower } -> "food" to "Restaurants"
            listOf(
                "bigbasket", "blinkit", "zepto", "instamart", "dmart", "reliance",
                "fresh", "grocery", "groceries", "supermarket", "milk", "vegetable", "nilgiris"
            ).any { it in lower } -> "groc" to "Groceries"
            listOf(
                "hpcl", "bpcl", "iocl", "indianoil", "indian oil", "bharat petroleum",
                "petrol", "diesel", "cng", "shell", "nayara", "fuel", "fastag", "toll",
                "uber", "ola", "rapido", "metro", "garage", "service", "tyre"
            ).any { it in lower } -> "veh" to "Fuel"
            listOf(
                "tneb", "bescom", "msedcl", "tata power", "adani", "electricity",
                "water", "airtel", "jio", "vi ", "bsnl", "broadband", "fibernet",
                "act ", "gas", "indane", "recharge", "eb bill"
            ).any { it in lower } -> "util" to "EB bill"
            listOf(
                "netflix", "prime video", "hotstar", "spotify", "youtube",
                "apple.com", "google play", "icloud", "sonyliv", "zee5", "subscription"
            ).any { it in lower } -> "subs" to "OTT"
            listOf(
                "apollo", "medplus", "1mg", "pharmeasy", "netmeds", "pharmacy",
                "hospital", "clinic", "medical", "diagnostic", "lab"
            ).any { it in lower } -> "hlth" to "Pharmacy"
            listOf(
                "emi", "loan", "lic ", "insurance", "mutual fund", "zerodha", "groww", "sip"
            ).any { it in lower } -> "fin" to "Home loan EMI"
            else -> "groc" to "Groceries"
        }
    }
}
