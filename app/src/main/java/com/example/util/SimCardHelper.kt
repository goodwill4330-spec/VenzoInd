package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

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
     * Reads all active SIM cards in the device
     */
    fun getActiveSimCards(context: Context): List<DetectedSim> {
        val simList = mutableListOf<DetectedSim>()

        try {
            val hasPhoneStatePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            val hasPhoneNumbersPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_NUMBERS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                hasPhoneStatePermission
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && (hasPhoneStatePermission || hasPhoneNumbersPermission)) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val subList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList

                if (!subList.isNullOrEmpty()) {
                    subList.forEachIndexed { index, info ->
                        var rawNumber: String? = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasPhoneNumbersPermission) {
                            try {
                                rawNumber = subscriptionManager.getPhoneNumber(info.subscriptionId)
                            } catch (e: Exception) {
                                rawNumber = info.number
                            }
                        } else {
                            rawNumber = info.number
                        }

                        // Clean number if present
                        val cleanPhone = if (!rawNumber.isNullOrBlank()) {
                            val digits = rawNumber.filter { it.isDigit() }
                            if (digits.length >= 10) digits.takeLast(10) else null
                        } else null

                        val carrier = info.carrierName?.toString()?.takeIf { it.isNotBlank() }
                            ?: info.displayName?.toString()?.takeIf { it.isNotBlank() }
                            ?: "SIM ${info.simSlotIndex + 1}"

                        simList.add(
                            DetectedSim(
                                slotIndex = info.simSlotIndex,
                                carrierName = carrier,
                                phoneNumber = cleanPhone,
                                countryIso = info.countryIso?.uppercase() ?: "IN",
                                displayName = "SIM ${info.simSlotIndex + 1}: $carrier",
                                isDefault = index == 0
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // SubscriptionManager fallback
        }

        // If no SIM found through SubscriptionManager (e.g. permission pending or single SIM without SubscriptionInfo)
        if (simList.isEmpty()) {
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val simState = telephonyManager?.simState
                val carrier = telephonyManager?.networkOperatorName?.takeIf { it.isNotBlank() }
                    ?: telephonyManager?.simOperatorName?.takeIf { it.isNotBlank() }
                    ?: "Primary SIM"
                val country = telephonyManager?.simCountryIso?.uppercase()?.takeIf { it.isNotBlank() } ?: "IN"

                var phone: String? = null
                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    try {
                        @Suppress("DEPRECATION")
                        val line1 = telephonyManager?.line1Number
                        if (!line1.isNullOrBlank()) {
                            val digits = line1.filter { it.isDigit() }
                            if (digits.length >= 10) phone = digits.takeLast(10)
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                if (simState == TelephonyManager.SIM_STATE_READY || carrier != "Primary SIM") {
                    simList.add(
                        DetectedSim(
                            slotIndex = 0,
                            carrierName = carrier,
                            phoneNumber = phone,
                            countryIso = country,
                            displayName = "SIM 1: $carrier",
                            isDefault = true
                        )
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        return simList
    }
}
