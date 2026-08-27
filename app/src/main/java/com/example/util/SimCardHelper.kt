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
    val subscriptionId: Int = -1,
    val carrierName: String,
    val phoneNumber: String?,
    val countryIso: String,
    val displayName: String,
    val isDefault: Boolean = false,
    val isVerifiedInDevice: Boolean = true
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
            return "कृपया अपना 10 अंकों का सक्रिय मोबाइल नंबर दर्ज करें।"
        }

        if (cleanNational.length != 10) {
            return "मोबाइल नंबर ठीक 10 अंकों का होना चाहिए (वर्तमान में ${cleanNational.length} अंक हैं)।"
        }

        val firstChar = cleanNational.first()
        if (firstChar !in listOf('6', '7', '8', '9')) {
            return "अमान्य नंबर। भारतीय मोबाइल नंबर 6, 7, 8 या 9 से शुरू होने चाहिए।"
        }

        // Check for dummy repeating digits
        if (cleanNational.all { it == firstChar }) {
            return "अमान्य नंबर। एक जैसे अंकों वाला नंबर ($cleanNational) मान्य नहीं है।"
        }

        // Check for sequential patterns (e.g. 9876543210, 1234567890)
        if (cleanNational == "9876543210" || cleanNational == "0123456789" || cleanNational == "1234567890") {
            return "यह एक टेस्ट/डमी नंबर है। कृपया अपने फ़ोन में लगे वास्तविक सिम का नंबर दर्ज करें।"
        }

        // Known dummy / placeholder test series
        val invalidPatterns = listOf(
            "1234567890", "0123456789", "0000000000", "1111111111",
            "2222222222", "3333333333", "4444444444", "5555555555",
            "6666666666", "7777777777", "8888888888", "9999999999",
            "9999988888", "9898989898", "9191919191", "9000000000"
        )
        if (cleanNational in invalidPatterns) {
            return "कृपया अपने मोबाइल में लगे असली सिम कार्ड का नंबर दर्ज करें।"
        }

        return null
    }

    /**
     * Reads active SIM cards from device hardware using SubscriptionManager and TelephonyManager.
     */
    fun getActiveSimCards(context: Context): List<DetectedSim> {
        val simList = mutableListOf<DetectedSim>()

        try {
            val hasPhoneState = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            val hasPhoneNumbers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_NUMBERS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                hasPhoneState
            }

            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

            if (subscriptionManager != null && (hasPhoneState || hasPhoneNumbers)) {
                try {
                    val subList: List<SubscriptionInfo>? = subscriptionManager.activeSubscriptionInfoList
                    if (!subList.isNullOrEmpty()) {
                        for ((index, info) in subList.withIndex()) {
                            var number: String? = null
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasPhoneNumbers) {
                                    number = subscriptionManager.getPhoneNumber(info.subscriptionId)
                                }
                                if (number.isNullOrBlank()) {
                                    number = info.number
                                }
                            } catch (e: Exception) {
                                number = info.number
                            }

                            val cleanPhone = number?.filter { it.isDigit() }?.let { digits ->
                                if (digits.length > 10) digits.takeLast(10) else digits
                            }

                            val carrier = info.carrierName?.toString()?.takeIf { it.isNotBlank() }
                                ?: info.displayName?.toString()?.takeIf { it.isNotBlank() }
                                ?: "SIM ${info.simSlotIndex + 1}"

                            val country = info.countryIso?.uppercase()?.takeIf { it.isNotBlank() } ?: "IN"

                            simList.add(
                                DetectedSim(
                                    slotIndex = info.simSlotIndex,
                                    subscriptionId = info.subscriptionId,
                                    carrierName = carrier,
                                    phoneNumber = cleanPhone?.takeIf { it.length == 10 },
                                    countryIso = country,
                                    displayName = "SIM ${info.simSlotIndex + 1}: $carrier",
                                    isDefault = index == 0,
                                    isVerifiedInDevice = true
                                )
                            )
                        }
                    }
                } catch (e: SecurityException) {
                    // fall back to telephonyManager
                }
            }

            // If SubscriptionManager didn't return any, use TelephonyManager
            if (simList.isEmpty() && telephonyManager != null) {
                val simState = telephonyManager.simState
                val hasSim = simState == TelephonyManager.SIM_STATE_READY || simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED

                val carrier = telephonyManager.networkOperatorName?.takeIf { it.isNotBlank() }
                    ?: telephonyManager.simOperatorName?.takeIf { it.isNotBlank() }
                    ?: if (hasSim) "Active Mobile SIM" else "No SIM Detected"

                val country = telephonyManager.simCountryIso?.uppercase()?.takeIf { it.isNotBlank() } ?: "IN"

                var line1Number: String? = null
                if (hasPhoneNumbers || hasPhoneState) {
                    try {
                        line1Number = telephonyManager.line1Number?.filter { it.isDigit() }?.let { digits ->
                            if (digits.length > 10) digits.takeLast(10) else digits
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                simList.add(
                    DetectedSim(
                        slotIndex = 0,
                        subscriptionId = 1,
                        carrierName = carrier,
                        phoneNumber = line1Number?.takeIf { it.length == 10 },
                        countryIso = country,
                        displayName = "SIM 1: $carrier",
                        isDefault = true,
                        isVerifiedInDevice = hasSim
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
            simList.add(
                DetectedSim(
                    slotIndex = 0,
                    subscriptionId = 1,
                    carrierName = "Device SIM Card",
                    phoneNumber = null,
                    countryIso = "IN",
                    displayName = "SIM 1: Active Network",
                    isDefault = true,
                    isVerifiedInDevice = true
                )
            )
        }

        return simList
    }

    /**
     * Verifies if the entered mobile number is from a SIM card in the current device.
     * If the SIM hardware exposes the number, strictly matches the number.
     * If carrier doesn't write MSISDN to SIM, checks if SIM is active in device and passes Indian carrier checks.
     */
    fun verifySimInDevice(context: Context, rawNumber: String, selectedSim: DetectedSim?): Pair<Boolean, String?> {
        val digits = rawNumber.filter { it.isDigit() }
        val cleanNational = if (digits.length > 10) digits.takeLast(10) else digits

        val indianValidationError = validateIndianMobileNumber(cleanNational)
        if (indianValidationError != null) {
            return Pair(false, indianValidationError)
        }

        val activeSims = getActiveSimCards(context)
        if (activeSims.isEmpty() || activeSims.all { !it.isVerifiedInDevice }) {
            return Pair(false, "इस डिवाइस में कोई सक्रिय सिम कार्ड नहीं मिला। कृपया पहले सिम कार्ड डालें।")
        }

        // Check if any SIM explicitly has a readable phone number
        val matchingSimWithNumber = activeSims.firstOrNull { it.phoneNumber != null }
        if (matchingSimWithNumber != null) {
            val isExactMatch = activeSims.any { it.phoneNumber == cleanNational }
            if (!isExactMatch) {
                return Pair(
                    false,
                    "दर्ज किया गया नंबर (${cleanNational}) इस फ़ोन के सिम कार्ड (${matchingSimWithNumber.phoneNumber}) से मेल नहीं खाता है। केवल इसी मोबाइल के सिम से एक्टिवेशन संभव है।"
                )
            }
        }

        // If specific SIM was selected, ensure it is active
        if (selectedSim != null && !selectedSim.isVerifiedInDevice) {
            return Pair(false, "चयनित सिम कार्ड (${selectedSim.displayName}) इस डिवाइस में सक्रिय नहीं है।")
        }

        return Pair(true, null)
    }
}

