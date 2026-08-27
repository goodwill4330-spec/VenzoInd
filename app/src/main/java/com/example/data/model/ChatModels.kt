package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    UPI_PAYMENT,
    FILE,
    POLL,
    SYSTEM_SECURITY,
    LOCATION,
    CATALOGUE
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    SEEN,
    READ
}

data class CatalogueProduct(
    val id: String,
    val title: String,
    val price: Double,
    val originalPrice: Double? = null,
    val discountPercent: Int = 0,
    val category: String,
    val description: String,
    val imageUrl: String? = null,
    val badge: String? = null
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val avatarInitial: String,
    val avatarColorHex: String,
    val isGroup: Boolean = false,
    val isSecret: Boolean = false,
    val isVerifiedBusiness: Boolean = false,
    val isAiAssistant: Boolean = false,
    val unreadCount: Int = 0,
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isOnline: Boolean = false,
    val disappearingSeconds: Int = 0, // 0 = off, 5, 10, 30, 60
    val customWallpaperId: String = "default",
    val lastMessageStatus: String = "SEEN", // SENDING, SENT, DELIVERED, SEEN, READ
    val lastMessageIsFromMe: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String,
    val isFromMe: Boolean,
    val status: String = "SEEN", // SENDING, SENT, DELIVERED, SEEN, READ
    val isSeen: Boolean = false,
    val seenTimestamp: Long? = null,
    val seenTimeFormatted: String? = null,
    val messageType: String = "TEXT", // TEXT, IMAGE, VOICE, UPI_PAYMENT, FILE, POLL, SYSTEM_SECURITY
    val attachmentUrl: String? = null,
    val fileSizeStr: String? = null,
    val upiAmount: Double? = null,
    val upiTransactionId: String? = null,
    val upiStatus: String? = null, // SUCCESS, PENDING, FAILED
    val voiceDurationSec: Int? = null,
    val audioWaveform: String? = null, // comma separated ints
    val translatedText: String? = null,
    val targetLang: String? = null,
    val reactionEmoji: String? = null,
    val pollQuestion: String? = null,
    val pollOptionsJson: String? = null, // JSON string of PollOption
    val pollVotesJson: String? = null,
    val isSecretExpiring: Boolean = false,
    val expireTimeMillis: Long = 0L,
    val isStarred: Boolean = false,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val catalogueTitle: String? = null,
    val cataloguePrice: Double? = null,
    val catalogueImageUrl: String? = null,
    val catalogueDescription: String? = null,
    val catalogueCategory: String? = null,
    val catalogueDiscountPercent: Int? = null
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val authorName: String,
    val authorAvatar: String,
    val isAiGenerated: Boolean = false,
    val aiEffectName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String,
    val caption: String,
    val isViewed: Boolean = false,
    val mediaGradientStart: String = "#FF671F",
    val mediaGradientEnd: String = "#06038D"
)

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val contactAvatar: String,
    val isVideo: Boolean,
    val isIncoming: Boolean,
    val isMissed: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String,
    val durationStr: String,
    val isEncrypted: Boolean = true,
    val qualityStr: String = "HD Voice"
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val upiId: String,
    val amount: Double,
    val isDebit: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String,
    val status: String = "SUCCESS",
    val referenceId: String
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val followersCountStr: String,
    val verified: Boolean = true,
    val avatarInitial: String,
    val isJoined: Boolean = false,
    val latestPost: String = "",
    val latestPostTime: String = ""
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val upiVpa: String = "",
    val avatarInitial: String,
    val avatarColorHex: String = "#FF671F",
    val statusMsg: String = "Available on VenzoInd 🇮🇳",
    val isBharatChatUser: Boolean = true,
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val publicKeyFingerprint: String = "KYBER-1024-DEF78A",
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val profilePicUri: String? = null,
    val isOnline: Boolean = false
) {
    val statusBio: String get() = statusMsg
    val status: String get() = statusMsg
    val upiId: String get() = upiVpa
    val isVenzoUser: Boolean get() = isBharatChatUser
}

data class PollOption(
    val id: Int,
    val text: String,
    val votes: Int = 0
)

data class NearbyUser(
    val id: String,
    val name: String,
    val distanceMeters: Int,
    val avatarInitial: String,
    val statusMsg: String,
    val isConnected: Boolean = false
)

data class UserProfile(
    val name: String = "Vikram Aditya",
    val bharatId: String = "@vikram_venzo",
    val phone: String = "+91 98765 43210",
    val email: String = "vikram.aditya@venzoind.com",
    val upiVpa: String = "vikram@venzo",
    val walletBalance: Double = 14850.50,
    val isCloudBackupEnabled: Boolean = true,
    val totalStorageUsedGb: Double = 1.4,
    val isQuantumEncrypted: Boolean = true,
    val multiDevicesCount: Int = 3,
    val statusBio: String = "Living in the moment | Building sovereign tech 🚀",
    val avatarInitial: String = "VA",
    val avatarColorHex: String = "#FF671F",
    val customAvatarIndex: Int = 0,
    val profilePicUri: String = ""
) {
    val upiId: String get() = upiVpa
    val status: String get() = statusBio
    val venzoId: String get() = bharatId
}
