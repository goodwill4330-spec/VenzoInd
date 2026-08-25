package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, timestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats")
    suspend fun getAllChatsList(): List<ChatEntity>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE title LIKE '%' || :query || '%' OR lastMessage LIKE '%' || :query || '%' OR subtitle LIKE '%' || :query || '%' ORDER BY isPinned DESC, timestamp DESC")
    fun searchChats(query: String): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("UPDATE chats SET lastMessage = :lastMsg, lastMessageTime = :time, timestamp = :timeMillis, lastMessageStatus = :status, lastMessageIsFromMe = :isFromMe WHERE id = :chatId")
    suspend fun updateLastMessageWithStatus(chatId: String, lastMsg: String, time: String, timeMillis: Long, status: String, isFromMe: Boolean)

    @Query("UPDATE chats SET lastMessage = :lastMsg, lastMessageTime = :time, timestamp = :timeMillis WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMsg: String, time: String, timeMillis: Long)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markChatAsRead(chatId: String)

    @Query("UPDATE chats SET title = :title WHERE id = :chatId")
    suspend fun updateChatTitle(chatId: String, title: String)

    @Query("UPDATE chats SET isPinned = CASE WHEN isPinned = 1 THEN 0 ELSE 1 END WHERE id = :chatId")
    suspend fun toggleChatPin(chatId: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("DELETE FROM chats")
    suspend fun clearAllChats()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesList(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :msgId")
    suspend fun getMessageById(msgId: String): MessageEntity?

    @Query("UPDATE messages SET pollVotesJson = :votesJson WHERE id = :msgId")
    suspend fun updatePollVotes(msgId: String, votesJson: String)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessage(chatId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET translatedText = :translated, targetLang = :lang WHERE id = :msgId")
    suspend fun updateTranslation(msgId: String, translated: String, lang: String)

    @Query("UPDATE messages SET reactionEmoji = :reaction WHERE id = :msgId")
    suspend fun updateReaction(msgId: String, reaction: String)

    @Query("UPDATE messages SET status = :status WHERE id = :msgId")
    suspend fun updateMessageStatus(msgId: String, status: String)

    @Query("UPDATE messages SET status = :status, isSeen = :isSeen, seenTimestamp = :seenTimestamp, seenTimeFormatted = :seenTimeFormatted WHERE id = :msgId")
    suspend fun updateMessageSeen(msgId: String, status: String, isSeen: Boolean, seenTimestamp: Long?, seenTimeFormatted: String?)

    @Query("UPDATE messages SET status = 'SEEN', isSeen = 1, seenTimestamp = :seenTimestamp, seenTimeFormatted = :seenTimeFormatted WHERE chatId = :chatId AND isFromMe = 0 AND isSeen = 0")
    suspend fun markAllIncomingAsSeen(chatId: String, seenTimestamp: Long, seenTimeFormatted: String)

    @Query("UPDATE messages SET status = :status, isSeen = 1, seenTimestamp = :seenTimestamp, seenTimeFormatted = :seenTimeFormatted WHERE chatId = :chatId AND isFromMe = 1")
    suspend fun markAllOutgoingAsSeen(chatId: String, status: String = "SEEN", seenTimestamp: Long = System.currentTimeMillis(), seenTimeFormatted: String = "")

    @Query("UPDATE messages SET status = :status WHERE chatId = :chatId AND isFromMe = 1")
    suspend fun updateOutgoingMessagesStatus(chatId: String, status: String)

    @Query("UPDATE messages SET isStarred = CASE WHEN isStarred = 1 THEN 0 ELSE 1 END WHERE id = :msgId")
    suspend fun toggleStarMessage(msgId: String)

    @Query("DELETE FROM messages WHERE id = :msgId")
    suspend fun deleteMessage(msgId: String)

    @Query("DELETE FROM messages WHERE id IN (:msgIds)")
    suspend fun deleteMultipleMessages(msgIds: List<String>)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearChatMessages(chatId: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Query("UPDATE stories SET isViewed = 1 WHERE id = :storyId")
    suspend fun markStoryViewed(storyId: String)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: String)
}

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalls(calls: List<CallEntity>)

    @Query("DELETE FROM calls WHERE id = :callId")
    suspend fun deleteCall(callId: String)

    @Query("DELETE FROM calls")
    suspend fun clearAllCalls()
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY followersCountStr DESC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isJoined = :isJoined WHERE id = :channelId")
    suspend fun toggleJoinChannel(channelId: String, isJoined: Boolean)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY isFavorite DESC, name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsList(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    suspend fun getContactByPhone(phone: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR upiVpa LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET isFavorite = :isFav WHERE id = :contactId")
    suspend fun updateFavoriteStatus(contactId: String, isFav: Boolean)

    @Query("UPDATE contacts SET isBlocked = :isBlocked WHERE id = :contactId")
    suspend fun updateBlockedStatus(contactId: String, isBlocked: Boolean)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: String)

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getContactsCount(): Int

    @Query("DELETE FROM contacts")
    suspend fun clearAllContacts()
}
