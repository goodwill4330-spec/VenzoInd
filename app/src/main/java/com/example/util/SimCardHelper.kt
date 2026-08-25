package com.example.util

import android.content.Context
import android.telephony.TelephonyManager

data class DetectedSim(
    val slotIndex: Int,
    val carrierName: String,
    val phoneNumber: String?,
    val countryIso: String,
    val displayName: String,
    val isDefault: Boolean = false
)

object SimCardHelper {

    /**
     * Strict Indian Mobile Number Validation
     * Returns null if valid, or a descriptive error message if invalid.
     */
    fun validateIndianMobileNumber(rawNumber: String): String? {
        val digits = rawNumber.filter { it.isDigit() }
        var cleanNational = digits

        // If user entered +91 or 91 at the beginning of a 12-digit number, strip it
        if (cleanNational.startsWith("91") && cleanNational.length == 12) {
            cleanNational = cleanNational.substring(2)
        }
        // If leading zero in 11-digit number, strip it
        if (cleanNational.startsWith("0") && cleanNational.length == 11) {
            cleanNational = cleanNational.substring(1)
        }

        if (cleanNational.isEmpty()) {
            return "Please enter your 10-digit mobile number"
        }

        if (cleanNational.length != 10) {
            return "Mobile number must be exactly 10 digits (currently ${cleanNational.length})"
        }

        val firstChar = cleanNational.first()
        if (firstChar !in listOf('6', '7', '8', '9')) {
            return "Invalid number. Indian mobile numbers must start with 6, 7, 8, or 9."
        }

        // Check for dummy repeated digits
        if (cleanNational.all { it == firstChar }) {
            return "Invalid number. All repeating digits ($cleanNational) are not allowed."
        }

        // Known dummy / placeholder test series
        val invalidPatterns = listOf(
            "1234567890", "0123456789", "0000000000", "1111111111",
            "2222222222", "3333333333", "4444444444", "5555555555"
        )
        if (cleanNational in invalidPatterns) {
            return "Please enter your real active SIM card number."
        }

        return null
    }

    /**
     * Reads active network carrier without requiring sensitive SMS or Phone state permissions
     */
    fun getActiveSimCards(context: Context): List<DetectedSim> {
        val simList = mutableListOf<DetectedSim>()

        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val carrier = telephonyManager?.networkOperatorName?.takeIf { it.isNotBlank() }
                ?: telephonyManager?.simOperatorName?.takeIf { it.isNotBlank() }
                ?: "Active SIM (Network)"
            val country = telephonyManager?.simCountryIso?.uppercase()?.takeIf { it.isNotBlank() } ?: "IN"

            simList.add(
                DetectedSim(
                    slotIndex = 0,
                    carrierName = carrier,
                    phoneNumber = null,
                    countryIso = country,
                    displayName = "SIM 1: $carrier",
                    isDefault = true
                )
            )
        } catch (e: Exception) {
            // ignore
        }

        return simList
    }
}

