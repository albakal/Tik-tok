package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.LiveStream
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LiveRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    )
    private val auth = Firebase.auth

    companion object {
        private const val TAG = "LiveRepository"
    }

    fun observeActiveLiveStreams(): Flow<List<LiveStream>> {
        return db.collection("live_streams")
            .whereEqualTo("isActive", true)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LiveStream::class.java)
            }
    }

    suspend fun startLiveStream(title: String): Result<LiveStream> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول لبدء البث المباشر"))

        return try {
            val streamId = "live_" + UUID.randomUUID().toString().replace("-", "").take(12)
            val streamPayload = hashMapOf<String, Any>(
                "hostId" to user.uid,
                "hostName" to (user.displayName?.ifBlank { "مذيع فيورا" } ?: "مذيع فيورا"),
                "hostAvatar" to (user.photoUrl?.toString().orEmpty()),
                "title" to title,
                "viewerCount" to 1L,
                "isActive" to true,
                "startedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("live_streams").document(streamId).set(streamPayload).await()

            val stream = LiveStream(
                id = streamId,
                hostId = user.uid,
                hostName = user.displayName.orEmpty(),
                hostAvatar = user.photoUrl?.toString().orEmpty(),
                title = title,
                viewerCount = 1L,
                isActive = true
            )
            Result.success(stream)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting live stream", e)
            Result.failure(e)
        }
    }

    suspend fun endLiveStream(streamId: String): Result<Unit> {
        return try {
            db.collection("live_streams").document(streamId)
                .update(
                    "isActive", false,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error ending live stream", e)
            Result.failure(e)
        }
    }

    suspend fun joinStream(streamId: String) {
        try {
            db.collection("live_streams").document(streamId)
                .update("viewerCount", FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing viewer count", e)
        }
    }

    suspend fun leaveStream(streamId: String) {
        try {
            db.collection("live_streams").document(streamId)
                .update("viewerCount", FieldValue.increment(-1))
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error decrementing viewer count", e)
        }
    }
}
