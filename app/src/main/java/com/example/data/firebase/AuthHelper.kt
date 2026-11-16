package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.notifications.ReviewDraftStore
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "SelavuAuth"

class EmailAlreadyInUseException(
    message: String = "An account with this email already exists. Sign in with your password, reset your password, or continue with Google."
) : Exception(message)

internal fun mapFirebaseAuthException(error: Throwable): Throwable {
    if (error is CancellationException) throw error
    return when (error) {
        is FirebaseAuthUserCollisionException -> EmailAlreadyInUseException()
        is FirebaseAuthWeakPasswordException -> IllegalArgumentException(
            "Password is too weak. Use at least 6 characters."
        )
        is FirebaseAuthInvalidUserException -> IllegalArgumentException(
            "No account found for this email. Switch to Create account or continue with Google."
        )
        is FirebaseAuthInvalidCredentialsException -> {
            if (error.errorCode == "ERROR_INVALID_EMAIL") {
                IllegalArgumentException("Please enter a valid email address.")
            } else {
                IllegalArgumentException("Incorrect email or password. Please check and try again.")
            }
        }
        is FirebaseTooManyRequestsException -> IllegalStateException(
            "Too many attempts right now. Please wait a moment or reset your password."
        )
        is FirebaseNetworkException -> IllegalStateException(
            "Network error. Check your internet connection and try again."
        )
        is FirebaseAuthException -> {
            if (error.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                EmailAlreadyInUseException()
            } else {
                IllegalStateException(
                    error.localizedMessage ?: "Authentication failed. Please try again."
                )
            }
        }
        else -> error
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun launchInteractiveGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned by Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(mapFirebaseAuthException(e).localizedMessage ?: "Sign in failed. Please try again.")
        }
    }
}

suspend fun signInWithEmailPassword(email: String, password: String): Result<Unit> = try {
    val cleanEmail = email.trim()
    require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "Please enter a valid email address." }
    require(password.isNotEmpty()) { "Please enter your password." }
    Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
    Result.success(Unit)
} catch (e: Throwable) {
    val mapped = mapFirebaseAuthException(e)
    Log.w(AUTH_TAG, "Email sign-in failed: ${mapped.message}")
    Result.failure(mapped)
}

suspend fun createAccountWithEmailPassword(
    username: String,
    email: String,
    password: String
): Result<Unit> = try {
    val cleanName = username.trim()
    val cleanEmail = email.trim()
    require(cleanName.isNotEmpty()) { "Please enter a username or display name." }
    require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "Please enter a valid email address." }
    require(password.length >= 6) { "Password must be at least 6 characters." }
    val result = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
    val user = result.user
    if (user != null) {
        try {
            val profileUpdates = userProfileChangeRequest {
                displayName = cleanName.take(80)
            }
            user.updateProfile(profileUpdates).await()
        } catch (profileError: Exception) {
            if (profileError is CancellationException) throw profileError
            Log.w(AUTH_TAG, "Account created but updating display name failed: ${profileError.message}")
        }
    }
    Result.success(Unit)
} catch (e: Throwable) {
    val mapped = mapFirebaseAuthException(e)
    Log.w(AUTH_TAG, "Account creation failed: ${mapped.message}")
    Result.failure(mapped)
}

suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
    val cleanEmail = email.trim()
    require(cleanEmail.contains("@") && cleanEmail.contains(".")) {
        "Enter your registered email address to receive a reset link."
    }
    Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
    Result.success(Unit)
} catch (e: Throwable) {
    val mapped = mapFirebaseAuthException(e)
    Log.w(AUTH_TAG, "Password reset failed: ${mapped.message}")
    Result.failure(mapped)
}

suspend fun updateAuthDisplayName(newName: String): Result<Unit> = runCatching {
    val user = Firebase.auth.currentUser ?: throw IllegalStateException("Not signed in.")
    val clean = newName.trim().take(80)
    require(clean.isNotEmpty()) { "Display name cannot be empty." }
    user.updateProfile(userProfileChangeRequest { displayName = clean }).await()
}

suspend fun deleteAccountCompletely(
    credentialManager: CredentialManager,
    repository: SelavuRepository,
    draftStore: ReviewDraftStore? = null
): Result<Unit> {
    val user = Firebase.auth.currentUser
        ?: return Result.failure(IllegalStateException("User is not signed in."))
    val uid = user.uid
    return try {
        repository.deleteAllUserData(uid = uid, deleteProfileDoc = true)
        user.delete().await()
        withContext(NonCancellable) {
            try {
                draftStore?.clearUserDrafts(uid)
                draftStore?.clearActiveUser()
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear drafts after account delete: ${e.message}")
            }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state after delete: ${e.message}")
            }
            Firebase.auth.signOut()
        }
        Result.success(Unit)
    } catch (e: FirebaseAuthRecentLoginRequiredException) {
        Log.w(AUTH_TAG, "Account deletion requires recent login", e)
        Result.failure(
            IllegalStateException(
                "Your Firestore data was cleared. For security, please sign out, sign back in, and tap Delete account again to remove your login."
            )
        )
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Failed to delete account", e)
        Result.failure(e)
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    draftStore: ReviewDraftStore? = null,
    onComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                // Only clears active_uid pointer; unapproved Review drafts stay preserved device-locally.
                draftStore?.clearActiveUser()
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear active user in draft store: ${e.message}")
            }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onComplete()
            }
        }
    }
}
