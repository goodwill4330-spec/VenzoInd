package com.example.data.sync

import com.example.data.model.MessageType
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class DeviceRole(
    val deviceName: String,
    val ownerName: String,
    val phone: String,
    val avatarInitial: String,
    val upiVpa: String,
    val nodeColorHex: String
) {
    PHONE_1(
        deviceName = "Phone 1 (Primary Node)",
        ownerName = "Vikram Aditya",
        phone = "+91 98765 43210",
        avatarInitial = "VA",
        upiVpa = "vikram@upi",
        nodeColorHex = "#FF671F"
    ),
    PHONE_2(
        deviceName = "Phone 2 (Secondary Node)",
        ownerName = "Gufran",
        phone = "+91 98123 45678",
        avatarInitial = "G",
        upiVpa = "gufran@upi",
        nodeColorHex = "#046A38"
    )
}

data class SyncPairStatus(
    val isPaired: Boolean = true,
    val pairCode: String = "1947",
    val activeRole: DeviceRole = DeviceRole.PHONE_1,
    val autoReplyEnabled: Boolean = false,
    val simulatedDelayMs: Long = 1200L,
    val connectedDevicesCount: Int = 2
)

data class IncomingCallEvent(
    val callId: String = "",
    val callerName: String,
    val callerAvatar: String,
    val isVideo: Boolean,
    val callerPhone: String = ""
)

data class IncomingUpiEvent(
    val senderName: String,
    val amount: Double,
    val upiVpa: String,
    val note: String
)

class MultiDeviceSyncManager {
    private val _syncStatus = MutableStateFlow(SyncPairStatus())
    val syncStatus: StateFlow<SyncPairStatus> = _syncStatus.asStateFlow()

    private val _incomingCall = MutableStateFlow<IncomingCallEvent?>(null)
    val incomingCall: StateFlow<IncomingCallEvent?> = _incomingCall.asStateFlow()

    private val _incomingUpi = MutableStateFlow<IncomingUpiEvent?>(null)
    val incomingUpi: StateFlow<IncomingUpiEvent?> = _incomingUpi.asStateFlow()

    fun switchRole(newRole: DeviceRole): UserProfile {
        _syncStatus.value = _syncStatus.value.copy(activeRole = newRole)
        return when (newRole) {
            DeviceRole.PHONE_1 -> UserProfile(
                name = DeviceRole.PHONE_1.ownerName,
                bharatId = "@vikram_bharat",
                phone = DeviceRole.PHONE_1.phone,
                email = "vikram.aditya@bharat.in",
                upiVpa = DeviceRole.PHONE_1.upiVpa,
                walletBalance = 14850.50
            )
            DeviceRole.PHONE_2 -> UserProfile(
                name = DeviceRole.PHONE_2.ownerName,
                bharatId = "@gufran_bharat",
                phone = DeviceRole.PHONE_2.phone,
                email = "gufran@bharat.in",
                upiVpa = DeviceRole.PHONE_2.upiVpa,
                walletBalance = 22400.00
            )
        }
    }

    fun updatePairCode(newCode: String) {
        _syncStatus.value = _syncStatus.value.copy(pairCode = newCode, isPaired = true)
    }

    fun toggleAutoReply(enabled: Boolean) {
        _syncStatus.value = _syncStatus.value.copy(autoReplyEnabled = enabled)
    }

    fun triggerIncomingCall(
        callId: String = "call_${UUID.randomUUID()}",
        callerName: String,
        callerAvatar: String,
        isVideo: Boolean,
        callerPhone: String = ""
    ) {
        _incomingCall.value = IncomingCallEvent(
            callId = callId,
            callerName = callerName,
            callerAvatar = callerAvatar,
            isVideo = isVideo,
            callerPhone = callerPhone
        )
    }

    fun triggerSimulatedIncomingCall(isVideo: Boolean = false) {
        val currentRole = _syncStatus.value.activeRole
        val caller = if (currentRole == DeviceRole.PHONE_1) DeviceRole.PHONE_2 else DeviceRole.PHONE_1
        triggerIncomingCall(
            callerName = caller.ownerName,
            callerAvatar = caller.avatarInitial,
            isVideo = isVideo,
            callerPhone = caller.phone
        )
    }

    fun clearIncomingCall() {
        _incomingCall.value = null
    }

    fun triggerSimulatedIncomingUpi(amount: Double = 500.0) {
        val currentRole = _syncStatus.value.activeRole
        val sender = if (currentRole == DeviceRole.PHONE_1) DeviceRole.PHONE_2 else DeviceRole.PHONE_1
        _incomingUpi.value = IncomingUpiEvent(
            senderName = sender.ownerName,
            amount = amount,
            upiVpa = sender.upiVpa,
            note = "Split for Coffee & Snacks ☕"
        )
    }

    fun clearIncomingUpi() {
        _incomingUpi.value = null
    }
}
