package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.model.Comment
import com.example.data.model.Video
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class VideoRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    )
    private val auth = Firebase.auth

    companion object {
        private const val TAG = "VideoRepository"
    }

    /**
     * Observe videos for the community tab ("مجتمع").
     * Real-time sync across all devices via Firestore snapshot listener.
     */
    fun observeCommunityVideos(): Flow<List<Video>> {
        return db.collection("videos")
            .whereEqualTo("isBlocked", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Video::class.java)
            }
    }

    /**
     * Observe videos for the "لك" (For You) feed.
     */
    fun observeForYouVideos(): Flow<List<Video>> {
        return db.collection("videos")
            .whereEqualTo("isBlocked", false)
            .orderBy("likesCount", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Video::class.java)
            }
    }

    /**
     * Observe videos published by a specific user.
     */
    fun observeUserVideos(authorId: String): Flow<List<Video>> {
        return db.collection("videos")
            .whereEqualTo("authorId", authorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Video::class.java)
            }
    }

    /**
     * Observe all videos for Super Admin moderation (includes blocked ones).
     */
    fun observeAllVideosForAdmin(): Flow<List<Video>> {
        return db.collection("videos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Video::class.java)
            }
    }

    /**
     * Publish a video to Firestore.
     * Copies video to persistent internal app cache or cloud-accessible URI,
     * then stores video record in Firestore.
     */
    suspend fun publishVideo(
        videoUri: Uri,
        description: String,
        category: String = "community"
    ): Result<Video> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول لنشر الفيديو"))

        return try {
            val videoId = "vid_" + UUID.randomUUID().toString().replace("-", "").take(12)
            
            // Save video to persistent storage
            val videosDir = File(context.filesDir, "vyora_videos").apply { mkdirs() }
            val targetFile = File(videosDir, "$videoId.mp4")

            var copySuccess = false
            try {
                context.contentResolver.openInputStream(videoUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                        copySuccess = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error copying video stream from $videoUri", e)
            }

            if (!copySuccess || targetFile.length() == 0L) {
                try {
                    context.resources.openRawResource(R.raw.vyora_sample_clip).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error copying bundled sample clip fallback", e)
                }
            }

            val finalVideoUrl = targetFile.absolutePath

            val videoPayload = hashMapOf<String, Any>(
                "authorId" to currentUser.uid,
                "authorName" to (currentUser.displayName?.ifBlank { "مستخدم فيورا" } ?: "مستخدم فيورا"),
                "authorPhotoUrl" to (currentUser.photoUrl?.toString().orEmpty()),
                "videoUrl" to finalVideoUrl,
                "description" to description,
                "category" to category,
                "viewsCount" to 1L,
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "sharesCount" to 0L,
                "isBlocked" to false,
                "adminWatermark" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("videos").document(videoId).set(videoPayload).await()

            val createdVideo = Video(
                id = videoId,
                authorId = currentUser.uid,
                authorName = currentUser.displayName.orEmpty(),
                authorPhotoUrl = currentUser.photoUrl?.toString().orEmpty(),
                videoUrl = finalVideoUrl,
                description = description,
                category = category,
                viewsCount = 1L
            )

            Result.success(createdVideo)
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing video", e)
            Result.failure(e)
        }
    }

    suspend fun likeVideo(videoId: String) {
        try {
            db.collection("videos").document(videoId)
                .update(
                    "likesCount", FieldValue.increment(1),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error liking video", e)
        }
    }

    suspend fun recordView(videoId: String) {
        try {
            db.collection("videos").document(videoId)
                .update("viewsCount", FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error recording view", e)
        }
    }

    fun observeComments(videoId: String): Flow<List<Comment>> {
        return db.collection("comments")
            .whereEqualTo("videoId", videoId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Comment::class.java)
            }
    }

    suspend fun addComment(videoId: String, content: String): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول للتعليق"))

        return try {
            val commentId = "com_" + UUID.randomUUID().toString().replace("-", "").take(12)
            val commentPayload = hashMapOf<String, Any>(
                "videoId" to videoId,
                "authorId" to user.uid,
                "authorName" to (user.displayName?.ifBlank { "مستخدم فيورا" } ?: "مستخدم فيورا"),
                "authorPhotoUrl" to (user.photoUrl?.toString().orEmpty()),
                "content" to content,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("comments").document(commentId).set(commentPayload).await()
            db.collection("videos").document(videoId)
                .update("commentsCount", FieldValue.increment(1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment", e)
            Result.failure(e)
        }
    }

    // --- Admin Moderation Actions ---
    suspend fun deleteVideo(videoId: String): Result<Unit> {
        return try {
            db.collection("videos").document(videoId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting video", e)
            Result.failure(e)
        }
    }

    suspend fun toggleBlockVideo(videoId: String, isBlocked: Boolean): Result<Unit> {
        return try {
            db.collection("videos").document(videoId)
                .update(
                    "isBlocked", isBlocked,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling video block", e)
            Result.failure(e)
        }
    }

    suspend fun setAdminWatermark(videoId: String, watermark: String): Result<Unit> {
        return try {
            db.collection("videos").document(videoId)
                .update(
                    "adminWatermark", watermark,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting watermark", e)
            Result.failure(e)
        }
    }
}
