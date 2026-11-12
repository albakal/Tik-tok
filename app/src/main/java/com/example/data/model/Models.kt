package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val role: String = "user", // "super_admin", "moderator", "user"
    val isBanned: Boolean = false,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Video(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val videoUrl: String = "",
    val description: String = "",
    val category: String = "community", // "community", "foryou", "friends"
    val viewsCount: Long = 0L,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val isBlocked: Boolean = false,
    val adminWatermark: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val videoId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LiveStream(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val hostAvatar: String = "",
    val title: String = "",
    val viewerCount: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val startedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Moderator(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val permissions: List<String> = emptyList(),
    val assignedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
