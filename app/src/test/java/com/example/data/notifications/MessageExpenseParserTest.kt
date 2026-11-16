package com.example.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageExpenseParserTest {

    @Test
    fun parse_otpAndVerificationAlerts_areRejectedInMemory() {
        val otpSamples = listOf(
            "HDFC Bank" to "Your OTP is 482910 for Rs 500 transaction at Amazon. Do not share with anyone.",
            "ICICI Bank" to "Verification code 123456 to complete payment of INR 1,299. Valid for 5 min.",
            "SBI Card" to "One Time Password 908712 for your debit card txn of Rs. 2,000.",
            "Axis Bank" to "Do not share this MPIN 4421 reset security code 887766.",
            "GPay" to "Use code 654321 as your login code.",
            "SBI" to "123456 is your UPI PIN reset verification code. Never share with anyone."
        )

        otpSamples.forEach { (sender, body) ->
            assertNull(
                "Expected OTP/security alert to be rejected: $body",
                MessageExpenseParser.parse(sender, body, 1_700_000_000_000L)
            )
        }
    }

    @Test
    fun parse_creditRefundFailedMandateAndBillDueAlerts_areRejected() {
        val nonSpendSamples = listOf(
            "HDFC Bank" to "Rs 2,500.00 credited to A/c XX1234 via UPI from Rahul.",
            "ICICI Bank" to "Available Balance is Rs 15,000.00 in A/c XX9876.",
            "SBI" to "Transaction failed for Rs. 450 at Swiggy due to timeout.",
            "Axis" to "Refunded INR 399.00 to your card XX4411 from Flipkart.",
            "PhonePe" to "Arun is requesting Rs 600 via collect request.",
            "SBI" to "Your A/c XX1234 is credited with Rs 65,000.00 towards salary on 01-11-26.",
            "SBI Card" to "Payment of Rs.12,450.00 received towards your SBI Credit Card XX9267. Thank you.",
            "HDFC Bank" to "Thanks for your payment of Rs. 8,500 towards HDFC Credit Card XX9012.",
            "ICICI Bank" to "Your Rs 499 debited from A/c XX4001 has been reversed on 15-11-26.",
            "UPI AutoPay" to "UPI AutoPay mandate created for Rs. 649 towards Netflix India.",
            "HDFC Bank" to "Mandate paused for Rs 199 towards Spotify India.",
            "SBI Card" to "Statement generated for SBI Card XX9267. Total Amount Due: Rs.15,200.00. Min Due: Rs.760.00 due on 05-12-26.",
            "Kotak" to "Upcoming AutoPay: Rs.799 will be debited on 18-11-26 towards JioFiber.",
            "Axis Bank" to "Pre-approved loan & EMI offer! Convert your Rs.5,680 txn to EMI today.",
            "HDFC" to "Rs. 1,200.00 cashback credited to your HDFC Bank A/c XX1234.",
            "ICICI" to "Your Card XX4001 txn of Rs 3,000 was declined due to insufficient funds.",
            "SBI Card" to "Your SBI Credit Card XX9267 has been blocked for suspicious txn of Rs. 9,000."
        )

        nonSpendSamples.forEach { (sender, body) ->
            assertNull(
                "Expected non-spend alert to be rejected: $body",
                MessageExpenseParser.parse(sender, body, 1_700_000_000_000L)
            )
        }
    }

    @Test
    fun parse_userExactSbiCreditCardUpiSms_extractsAmountMerchantAndUpiMode() {
        val exactSms =
            "Rs.5,680.00 spent on your SBI Credit Card ending with 9267 at sbipmopad0222119002854 on 15-11-26 via UPI (Ref No. 631909616383). Trxn. not done by you? Report at"
        val parsed = MessageExpenseParser.parse(
            senderTitle = "AX-SBICRD",
            body = exactSms,
            timestampMs = 1_700_000_040_000L
        )

        assertNotNull(parsed)
        assertEquals(5680.0, parsed!!.amount, 0.01)
        assertEquals("UPI", parsed.paymentMode)
        assertEquals("sbipmopad0222119002854", parsed.title)
    }

    @Test
    fun parse_sbiCreditCardWithReportUrlAndAvlLimit_extractsSpendAmountAndIgnoresLimit() {
        val smsWithLinkAndLimit =
            "Rs.5,680.00 spent on your SBI Credit Card ending with 9267 at sbipmopad0222119002854 on 15-11-26 via UPI (Ref No. 631909616383). Avl Lmt Rs.1,44,320.00. Trxn. not done by you? Report at https://sbicard.com/Dispute or SMS BLOCK 9267"
        val parsed = MessageExpenseParser.parse("JD-SBICRD", smsWithLinkAndLimit, 1_700_000_040_000L)

        assertNotNull(parsed)
        assertEquals(5680.0, parsed!!.amount, 0.01)
        assertEquals("UPI", parsed.paymentMode)
        assertEquals("sbipmopad0222119002854", parsed.title)
    }

    @Test
    fun parse_sbiUpiDebitWithoutRsPrefix_extractsAmountAndMerchant() {
        val sbiUpi =
            "Dear UPI user A/C X9267 debited by 1450.5 on date 15Nov26 trf to SWIGGY INDIA Refno 631909616383. If not u? call 1800111109. -SBI"
        val parsed = MessageExpenseParser.parse("AD-SBIUPI", sbiUpi, 1_700_000_040_000L)

        assertNotNull(parsed)
        assertEquals(1450.5, parsed!!.amount, 0.01)
        assertEquals("UPI", parsed.paymentMode)
        assertEquals("SWIGGY INDIA", parsed.title)
        assertEquals("food", parsed.suggestedCategoryId)
    }

    @Test
    fun parse_indianBankDebitAlert_extractsMerchantAmountModeAndIgnoresAvlBal() {
        val parsed = MessageExpenseParser.parse(
            senderTitle = "HDFC Bank Alert",
            body = "Rs. 450.00 debited from A/c XX1234 to SWIGGY via UPI. Avl Bal Rs 18,200.50",
            timestampMs = 1_700_000_040_000L
        )

        assertNotNull(parsed)
        assertEquals(450.0, parsed!!.amount, 0.01)
        assertEquals("UPI", parsed.paymentMode)
        assertTrue(parsed.title.contains("SWIGGY", ignoreCase = true))
        assertEquals("food", parsed.suggestedCategoryId)
    }

    @Test
    fun parse_majorIndianBankFormats_hdfcIciciAxisKotakPnbBobAtmAndDualCurrency() {
        // HDFC UPI with SMS BLOCK UPI footer
        val hdfcUpi = MessageExpenseParser.parse(
            "VM-HDFCBK",
            "Sent Rs.720.00 From HDFC Bank A/C *1234 To ZOMATO On 15/11/26 Ref 631909616383 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808"
        )
        assertNotNull(hdfcUpi)
        assertEquals(720.0, hdfcUpi!!.amount, 0.01)
        assertEquals("ZOMATO", hdfcUpi.title)
        assertEquals("food", hdfcUpi.suggestedCategoryId)

        // ICICI Bank Card with "on <merchant>" and "Avl Limit"
        val iciciCard = MessageExpenseParser.parse(
            "AX-ICICIB",
            "INR 1,890.00 spent using ICICI Bank Card XX4001 on 15-Nov-26 on INDIANOIL FUEL. Avl Limit: INR 85,550.00. If not you, call 18002662."
        )
        assertNotNull(iciciCard)
        assertEquals(1890.0, iciciCard!!.amount, 0.01)
        assertEquals("Card", iciciCard.paymentMode)
        assertEquals("veh", iciciCard.suggestedCategoryId)
        assertTrue(iciciCard.title.contains("INDIANOIL", ignoreCase = true))

        // Axis Bank UPI Info- UPI/P2M/ref/merchant & Avl Bal-
        val axis = MessageExpenseParser.parse(
            "JD-AXISBK",
            "INR 640.00 debited from A/c no. XX1234 on 15-11-26 14:22:10 IST. Info- UPI/P2M/631909616383/APOLLO PHARMACY. Avl Bal- INR 12,345.67"
        )
        assertNotNull(axis)
        assertEquals(640.0, axis!!.amount, 0.01)
        assertEquals("UPI", axis.paymentMode)
        assertEquals("hlth", axis.suggestedCategoryId)
        assertTrue(axis.title.contains("APOLLO", ignoreCase = true))

        // Kotak Bank with VPA & comma Not you footer
        val kotak = MessageExpenseParser.parse(
            "AD-KOTAKB",
            "Sent Rs.850.00 from Kotak Bank AC X1234 to rahul@okicici on 15-11-26.UPI Ref 631909616383. Not you, https://kotak.com/fraud"
        )
        assertNotNull(kotak)
        assertEquals(850.0, kotak!!.amount, 0.01)
        assertEquals("UPI", kotak.paymentMode)
        assertEquals("rahul@okicici", kotak.title)

        // BOB outgoing debit mentioning recipient Cr. to
        val bob = MessageExpenseParser.parse(
            "VM-BOBTXN",
            "Rs.500.00 Dr. from A/C X1234 and Cr. to 9876543210@ybl. Ref:631909616383. AvlBal:Rs10500.00"
        )
        assertNotNull(bob)
        assertEquals(500.0, bob!!.amount, 0.01)
        assertEquals("UPI", bob.paymentMode)
        assertEquals("9876543210@ybl", bob.title)

        // ATM withdrawal
        val atm = MessageExpenseParser.parse(
            "AD-SBIINB",
            "Rs.3,000.00 withdrawn at SBI ATM KARAIKAL from A/c XX9267 on 15-11-26. Avl Bal Rs 22,100.00"
        )
        assertNotNull(atm)
        assertEquals(3000.0, atm!!.amount, 0.01)
        assertEquals("ATM Cash Withdrawal", atm.title)

        // Dual currency international card spend prefers INR
        val intl = MessageExpenseParser.parse(
            "HDFC Card",
            "Your HDFC Credit Card XX9012 was used for USD 20.00 (INR 1,680.00) at NETFLIX.COM on 15-Nov-26."
        )
        assertNotNull(intl)
        assertEquals(1680.0, intl!!.amount, 0.01)
        assertEquals("Card", intl.paymentMode)
        assertEquals("subs", intl.suggestedCategoryId)
    }

    @Test
    fun parse_upiFuelAlert_suggestsVehicleCategory() {
        val parsed = MessageExpenseParser.parse(
            senderTitle = "Google Pay",
            body = "Paid ₹1,200 to HPCL Petrol Pump via GPay",
            timestampMs = 1_700_000_050_000L
        )

        assertNotNull(parsed)
        assertEquals(1200.0, parsed!!.amount, 0.01)
        assertEquals("UPI", parsed.paymentMode)
        assertEquals("veh", parsed.suggestedCategoryId)
        assertTrue(parsed.title.contains("HPCL", ignoreCase = true))
    }

    @Test
    fun parse_creditCardSpend_detectsCardMode() {
        val parsed = MessageExpenseParser.parse(
            senderTitle = "HDFC Card",
            body = "INR 2,999 spent on HDFC Credit Card XX9012 at Amazon India on 12-Nov.",
            timestampMs = 1_700_000_050_000L
        )

        assertNotNull(parsed)
        assertEquals(2999.0, parsed!!.amount, 0.01)
        assertEquals("Card", parsed.paymentMode)
        assertEquals("groc", parsed.suggestedCategoryId)
    }

    @Test
    fun parse_sameTransactionWithinTwoMinutes_producesSameFingerprint() {
        val t1 = 1_700_000_040_000L
        val t2 = t1 + 30_000L
        val first = MessageExpenseParser.parse("GPay", "Paid ₹320 to Zomato via UPI", t1)
        val second = MessageExpenseParser.parse("GPay", "Paid ₹320 to Zomato via UPI", t2)

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(first!!.fingerprint, second!!.fingerprint)
    }
}
