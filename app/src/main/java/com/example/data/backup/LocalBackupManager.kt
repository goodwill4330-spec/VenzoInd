package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.data.local.AppDatabase
import com.example.data.model.ChatEntity
import com.example.data.model.ContactEntity
import com.example.data.model.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportResult(
    val success: Boolean,
    val filePath: String = "",
    val fileName: String = "",
    val chatsCount: Int = 0,
    val messagesCount: Int = 0,
    val contactsCount: Int = 0,
    val fileSizeFormatted: String = "",
    val backupTimestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

data class RestoreResult(
    val success: Boolean,
    val chatsRestored: Int = 0,
    val messagesRestored: Int = 0,
    val contactsRestored: Int = 0,
    val backupDateStr: String = "",
    val appVersion: String = "",
    val errorMessage: String? = null
)

data class BackupFileInfo(
    val file: File,
    val name: String,
    val sizeFormatted: String,
    val lastModifiedFormatted: String,
    val timestamp: Long
)

class LocalBackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val contactDao = database.contactDao()

    private fun getBackupDirectory(): File {
        val backupDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "VenzoraBackups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir
    }

    suspend fun getAvailableBackups(): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        val dir = getBackupDirectory()
        val files = dir.listFiles { _, name -> name.endsWith(".json") || name.endsWith(".vzb") } ?: emptyArray()
        files.sortedByDescending { it.lastModified() }.map { file ->
            val sizeKb = file.length() / 1024.0
            val sizeStr = if (sizeKb >= 1024) String.format(Locale.getDefault(), "%.1f MB", sizeKb / 1024.0)
                          else String.format(Locale.getDefault(), "%.1f KB", sizeKb)
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))
            BackupFileInfo(
                file = file,
                name = file.name,
                sizeFormatted = sizeStr,
                lastModifiedFormatted = dateStr,
                timestamp = file.lastModified()
            )
        }
    }

    suspend fun exportDataToLocalFile(customFileName: String? = null): ExportResult = withContext(Dispatchers.IO) {
        try {
            val chats = chatDao.getAllChatsList()
            val messages = messageDao.getAllMessagesList()
            val contacts = contactDao.getAllContactsList()

            val rootJson = JSONObject()
            rootJson.put("app", "VenzoInd")
            rootJson.put("version", "2.0")
            rootJson.put("exportTimestamp", System.currentTimeMillis())
            rootJson.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            rootJson.put("schemaVersion", 3)

            // Export Chats
            val chatsArray = JSONArray()
            for (chat in chats) {
                val cObj = JSONObject().apply {
                    put("id", chat.id)
                    put("title", chat.title)
                    put("subtitle", chat.subtitle)
                    put("avatarInitial", chat.avatarInitial)
                    put("avatarColorHex", chat.avatarColorHex)
                    put("isGroup", chat.isGroup)
                    put("isSecret", chat.isSecret)
                    put("isVerifiedBusiness", chat.isVerifiedBusiness)
                    put("isAiAssistant", chat.isAiAssistant)
                    put("unreadCount", chat.unreadCount)
                    put("lastMessage", chat.lastMessage)
                    put("lastMessageTime", chat.lastMessageTime)
                    put("timestamp", chat.timestamp)
                    put("isPinned", chat.isPinned)
                    put("isOnline", chat.isOnline)
                    put("disappearingSeconds", chat.disappearingSeconds)
                    put("customWallpaperId", chat.customWallpaperId)
                    put("lastMessageStatus", chat.lastMessageStatus)
                    put("lastMessageIsFromMe", chat.lastMessageIsFromMe)
                }
                chatsArray.put(cObj)
            }
            rootJson.put("chats", chatsArray)

            // Export Messages
            val messagesArray = JSONArray()
            for (msg in messages) {
                val mObj = JSONObject().apply {
                    put("id", msg.id)
                    put("chatId", msg.chatId)
                    put("senderId", msg.senderId)
                    put("senderName", msg.senderName)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                    put("timeFormatted", msg.timeFormatted)
                    put("isFromMe", msg.isFromMe)
                    put("status", msg.status)
                    put("isSeen", msg.isSeen)
                    put("seenTimestamp", msg.seenTimestamp ?: 0L)
                    put("seenTimeFormatted", msg.seenTimeFormatted ?: "")
                    put("messageType", msg.messageType)
                    put("attachmentUrl", msg.attachmentUrl ?: "")
                    put("fileSizeStr", msg.fileSizeStr ?: "")
                    put("upiAmount", msg.upiAmount ?: 0.0)
                    put("upiTransactionId", msg.upiTransactionId ?: "")
                    put("upiStatus", msg.upiStatus ?: "")
                    put("voiceDurationSec", msg.voiceDurationSec ?: 0)
                    put("audioWaveform", msg.audioWaveform ?: "")
                    put("translatedText", msg.translatedText ?: "")
                    put("targetLang", msg.targetLang ?: "")
                    put("reactionEmoji", msg.reactionEmoji ?: "")
                    put("pollQuestion", msg.pollQuestion ?: "")
                    put("pollOptionsJson", msg.pollOptionsJson ?: "")
                    put("pollVotesJson", msg.pollVotesJson ?: "")
                    put("isSecretExpiring", msg.isSecretExpiring)
                    put("expireTimeMillis", msg.expireTimeMillis)
                }
                messagesArray.put(mObj)
            }
            rootJson.put("messages", messagesArray)

            // Export Contacts
            val contactsArray = JSONArray()
            for (contact in contacts) {
                val ctObj = JSONObject().apply {
                    put("id", contact.id)
                    put("name", contact.name)
                    put("phone", contact.phone)
                    put("upiVpa", contact.upiVpa)
                    put("avatarInitial", contact.avatarInitial)
                    put("avatarColorHex", contact.avatarColorHex)
                    put("statusMsg", contact.statusMsg)
                    put("isBharatChatUser", contact.isBharatChatUser)
                    put("isFavorite", contact.isFavorite)
                    put("isBlocked", contact.isBlocked)
                    put("publicKeyFingerprint", contact.publicKeyFingerprint)
                    put("lastSeenTimestamp", contact.lastSeenTimestamp)
                }
                contactsArray.put(ctObj)
            }
            rootJson.put("contacts", contactsArray)

            // Write to file in app's document directory
            val timeStampForFile = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = customFileName?.ifBlank { null } ?: "Venzora_Backup_$timeStampForFile.json"
            val backupDir = getBackupDirectory()
            val backupFile = File(backupDir, fileName)

            FileOutputStream(backupFile).use { out ->
                out.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val sizeKb = backupFile.length() / 1024.0
            val sizeStr = if (sizeKb >= 1024) String.format(Locale.getDefault(), "%.1f MB", sizeKb / 1024.0)
                          else String.format(Locale.getDefault(), "%.1f KB", sizeKb)

            ExportResult(
                success = true,
                filePath = backupFile.absolutePath,
                fileName = fileName,
                chatsCount = chats.size,
                messagesCount = messages.size,
                contactsCount = contacts.size,
                fileSizeFormatted = sizeStr,
                backupTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Unknown export error"
            )
        }
    }

    suspend fun restoreDataFromFile(file: File): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = file.readText(Charsets.UTF_8)
            restoreFromJsonString(jsonString)
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                errorMessage = "Failed to read backup file: ${e.localizedMessage}"
            )
        }
    }

    suspend fun restoreDataFromUri(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { it.readText() }
            } ?: throw IllegalStateException("Cannot open input stream for selected backup file")

            restoreFromJsonString(jsonString)
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                errorMessage = "Failed to restore backup: ${e.localizedMessage}"
            )
        }
    }

    suspend fun restoreFromJsonString(jsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val rootJson = JSONObject(jsonString)
            val backupDateStr = rootJson.optString("exportDate", "Unknown Date")
            val appVersion = rootJson.optString("app", "VenzoInd")

            // Restore Contacts
            val contactsList = mutableListOf<ContactEntity>()
            val contactsArray = rootJson.optJSONArray("contacts")
            if (contactsArray != null) {
                for (i in 0 until contactsArray.length()) {
                    val obj = contactsArray.getJSONObject(i)
                    contactsList.add(
                        ContactEntity(
                            id = obj.optString("id", "contact_$i"),
                            name = obj.optString("name", "Unknown Contact"),
                            phone = obj.optString("phone", ""),
                            upiVpa = obj.optString("upiVpa", ""),
                            avatarInitial = obj.optString("avatarInitial", "U"),
                            avatarColorHex = obj.optString("avatarColorHex", "#0284C7"),
                            statusMsg = obj.optString("statusMsg", "Available on VenzoInd 🇮🇳"),
                            isBharatChatUser = obj.optBoolean("isBharatChatUser", true),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            isBlocked = obj.optBoolean("isBlocked", false),
                            publicKeyFingerprint = obj.optString("publicKeyFingerprint", "KYBER-1024-DEF"),
                            lastSeenTimestamp = obj.optLong("lastSeenTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Restore Chats
            val chatsList = mutableListOf<ChatEntity>()
            val chatsArray = rootJson.optJSONArray("chats")
            if (chatsArray != null) {
                for (i in 0 until chatsArray.length()) {
                    val obj = chatsArray.getJSONObject(i)
                    chatsList.add(
                        ChatEntity(
                            id = obj.optString("id", "chat_$i"),
                            title = obj.optString("title", "Chat"),
                            subtitle = obj.optString("subtitle", ""),
                            avatarInitial = obj.optString("avatarInitial", obj.optString("title", "C").take(2).uppercase()),
                            avatarColorHex = obj.optString("avatarColorHex", "#FF671F"),
                            isGroup = obj.optBoolean("isGroup", false),
                            isSecret = obj.optBoolean("isSecret", false),
                            isVerifiedBusiness = obj.optBoolean("isVerifiedBusiness", false),
                            isAiAssistant = obj.optBoolean("isAiAssistant", false),
                            unreadCount = obj.optInt("unreadCount", 0),
                            lastMessage = obj.optString("lastMessage", ""),
                            lastMessageTime = obj.optString("lastMessageTime", "Now"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isPinned = obj.optBoolean("isPinned", false),
                            isOnline = obj.optBoolean("isOnline", false),
                            disappearingSeconds = obj.optInt("disappearingSeconds", 0),
                            customWallpaperId = obj.optString("customWallpaperId", "default"),
                            lastMessageStatus = obj.optString("lastMessageStatus", "SEEN"),
                            lastMessageIsFromMe = obj.optBoolean("lastMessageIsFromMe", false)
                        )
                    )
                }
            }

            // Restore Messages
            val messagesList = mutableListOf<MessageEntity>()
            val messagesArray = rootJson.optJSONArray("messages")
            if (messagesArray != null) {
                for (i in 0 until messagesArray.length()) {
                    val obj = messagesArray.getJSONObject(i)
                    val seenTimestampVal = obj.optLong("seenTimestamp", 0L)
                    val seenTimeFormattedVal = obj.optString("seenTimeFormatted", "")
                    val voiceDurationVal = obj.optInt("voiceDurationSec", 0)

                    messagesList.add(
                        MessageEntity(
                            id = obj.optString("id", "msg_$i"),
                            chatId = obj.optString("chatId", ""),
                            senderId = obj.optString("senderId", "me"),
                            senderName = obj.optString("senderName", "You"),
                            text = obj.optString("text", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            timeFormatted = obj.optString("timeFormatted", "Now"),
                            isFromMe = obj.optBoolean("isFromMe", true),
                            status = obj.optString("status", "SEEN"),
                            isSeen = obj.optBoolean("isSeen", false),
                            seenTimestamp = if (seenTimestampVal > 0) seenTimestampVal else null,
                            seenTimeFormatted = if (seenTimeFormattedVal.isNotBlank()) seenTimeFormattedVal else null,
                            messageType = obj.optString("messageType", "TEXT"),
                            attachmentUrl = obj.optString("attachmentUrl").ifBlank { null },
                            fileSizeStr = obj.optString("fileSizeStr").ifBlank { null },
                            upiAmount = if (obj.has("upiAmount") && obj.getDouble("upiAmount") > 0) obj.getDouble("upiAmount") else null,
                            upiTransactionId = obj.optString("upiTransactionId").ifBlank { null },
                            upiStatus = obj.optString("upiStatus").ifBlank { null },
                            voiceDurationSec = if (voiceDurationVal > 0) voiceDurationVal else null,
                            audioWaveform = obj.optString("audioWaveform").ifBlank { null },
                            translatedText = obj.optString("translatedText").ifBlank { null },
                            targetLang = obj.optString("targetLang").ifBlank { null },
                            reactionEmoji = obj.optString("reactionEmoji").ifBlank { null },
                            pollQuestion = obj.optString("pollQuestion").ifBlank { null },
                            pollOptionsJson = obj.optString("pollOptionsJson").ifBlank { null },
                            pollVotesJson = obj.optString("pollVotesJson").ifBlank { null },
                            isSecretExpiring = obj.optBoolean("isSecretExpiring", false),
                            expireTimeMillis = obj.optLong("expireTimeMillis", 0L)
                        )
                    )
                }
            }

            // Save to Room DB with REPLACE strategy
            if (contactsList.isNotEmpty()) {
                contactDao.insertContacts(contactsList)
            }
            if (chatsList.isNotEmpty()) {
                chatDao.insertChats(chatsList)
            }
            if (messagesList.isNotEmpty()) {
                messageDao.insertMessages(messagesList)
            }

            RestoreResult(
                success = true,
                chatsRestored = chatsList.size,
                messagesRestored = messagesList.size,
                contactsRestored = contactsList.size,
                backupDateStr = backupDateStr,
                appVersion = appVersion
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                errorMessage = "Failed to parse backup data: ${e.localizedMessage}"
            )
        }
    }
}
