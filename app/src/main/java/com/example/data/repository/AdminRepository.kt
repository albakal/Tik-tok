package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.Moderator
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    )
    private val auth = Firebase.auth

    companion object {
        private const val TAG = "AdminRepository"
    }

    fun observeAllUsers(): Flow<List<UserProfile>> {
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java)
            }
    }

    suspend fun setUserBannedStatus(userId: String, isBanned: Boolean): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .update(
                    "isBanned", isBanned,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user ban status", e)
            Result.failure(e)
        }
    }

    fun observeModerators(): Flow<List<Moderator>> {
        return db.collection("moderators")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Moderator::class.java)
            }
    }

    suspend fun addModerator(email: String, permissions: List<String>): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول كمدير رئيسي"))

        return try {
            val modId = "mod_" + UUID.randomUUID().toString().replace("-", "").take(10)
            val modPayload = hashMapOf<String, Any>(
                "uid" to modId,
                "email" to email,
                "permissions" to permissions,
                "assignedBy" to currentUser.uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("moderators").document(modId).set(modPayload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding moderator", e)
            Result.failure(e)
        }
    }

    suspend fun removeModerator(modId: String): Result<Unit> {
        return try {
            db.collection("moderators").document(modId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing moderator", e)
            Result.failure(e)
        }
    }
}
