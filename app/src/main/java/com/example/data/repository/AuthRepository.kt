package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    )
    private val credentialManager = CredentialManager.create(context)

    companion object {
        const val SUPER_ADMIN_EMAIL = "mahmoudomran324@gmail.com"
        private const val TAG = "AuthRepository"
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        return db.collection("users").document(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java)
                } else {
                    null
                }
            }
    }

    suspend fun syncUserProfile(user: FirebaseUser): UserProfile {
        val uid = user.uid
        val email = user.email.orEmpty()
        val displayName = user.displayName?.ifBlank { "مستخدم فيورا" } ?: "مستخدم فيورا"
        val photoUrl = user.photoUrl?.toString().orEmpty()
        val isSuperAdmin = email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
        val role = if (isSuperAdmin) "super_admin" else "user"

        val userDoc = db.collection("users").document(uid)
        val snapshot = userDoc.get().await()

        return if (!snapshot.exists()) {
            val payload = hashMapOf<String, Any>(
                "uid" to uid,
                "email" to email,
                "displayName" to displayName,
                "photoUrl" to photoUrl,
                "role" to role,
                "isBanned" to false,
                "followersCount" to 0L,
                "followingCount" to 0L,
                "likesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userDoc.set(payload).await()
            UserProfile(
                id = uid,
                uid = uid,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                role = role,
                isBanned = false
            )
        } else {
            val existing = snapshot.toObject(UserProfile::class.java) ?: UserProfile()
            if (isSuperAdmin && existing.role != "super_admin") {
                userDoc.update("role", "super_admin", "updatedAt", FieldValue.serverTimestamp()).await()
                existing.copy(role = "super_admin")
            } else {
                existing
            }
        }
    }

    suspend fun updateProfile(name: String, photoUrl: String) {
        val user = auth.currentUser ?: return
        val doc = db.collection("users").document(user.uid)
        doc.update(
            "displayName", name,
            "photoUrl", photoUrl,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("تعذر العثور على إعدادات Google Client ID: ${e.message}")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        syncUserProfile(user)
                        onSuccess(user)
                    } else {
                        onError("فشل تسجيل الدخول: لا يوجد مستخدم")
                    }
                } else {
                    onError("نوع الاعتماد غير مدعوم")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled: ${e.message}", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "حدث خطأ أثناء تسجيل الدخول بحساب Google")
            }
        }
    }

    fun attemptAutoSignIn(
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        val current = auth.currentUser
        if (current != null) {
            scope.launch {
                try {
                    syncUserProfile(current)
                    onSuccess(current)
                } catch (e: Exception) {
                    onSuccess(current)
                }
            }
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        syncUserProfile(user)
                        onSuccess(user)
                    } else {
                        onUnauthenticated()
                    }
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit = {}) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    onComplete()
                }
            }
        }
    }
}
