package com.example.data.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthHelperTest {

    @Test
    fun mapFirebaseAuthException_userCollision_returnsEmailAlreadyInUseException() {
        val raw = FirebaseAuthUserCollisionException(
            "ERROR_EMAIL_ALREADY_IN_USE",
            "The email address is already in use by another account."
        )

        val mapped = mapFirebaseAuthException(raw)

        assertTrue(mapped is EmailAlreadyInUseException)
        assertTrue(mapped.message!!.contains("already exists"))
    }

    @Test
    fun mapFirebaseAuthException_genericAuthCollisionCode_returnsEmailAlreadyInUseException() {
        val raw = FirebaseAuthException(
            "ERROR_EMAIL_ALREADY_IN_USE",
            "The email address is already in use by another account."
        )

        val mapped = mapFirebaseAuthException(raw)

        assertTrue(mapped is EmailAlreadyInUseException)
    }

    @Test
    fun mapFirebaseAuthException_weakPassword_returnsClearMessage() {
        val raw = FirebaseAuthWeakPasswordException(
            "ERROR_WEAK_PASSWORD",
            "The given password is invalid.",
            "Password should be at least 6 characters"
        )

        val mapped = mapFirebaseAuthException(raw)

        assertTrue(mapped is IllegalArgumentException)
        assertEquals("Password is too weak. Use at least 6 characters.", mapped.message)
    }

    @Test
    fun mapFirebaseAuthException_invalidUser_returnsClearMessage() {
        val raw = FirebaseAuthInvalidUserException(
            "ERROR_USER_NOT_FOUND",
            "There is no user record."
        )

        val mapped = mapFirebaseAuthException(raw)

        assertTrue(mapped is IllegalArgumentException)
        assertTrue(mapped.message!!.contains("No account found"))
    }

    @Test
    fun mapFirebaseAuthException_invalidEmail_returnsEmailPrompt() {
        val raw = FirebaseAuthInvalidCredentialsException(
            "ERROR_INVALID_EMAIL",
            "The email address is badly formatted."
        )

        val mapped = mapFirebaseAuthException(raw)

        assertTrue(mapped is IllegalArgumentException)
        assertEquals("Please enter a valid email address.", mapped.message)
    }

    @Test
    fun mapFirebaseAuthException_wrongPassword_returnsCredentialPrompt() {
        val raw = FirebaseAuthInvalidCredentialsException(
            "ERROR_WRONG_PASSWORD",
            "The password is invalid."
        )

        val mapped = mapFirebaseAuthException(raw)

        assertTrue(mapped is IllegalArgumentException)
        assertEquals("Incorrect email or password. Please check and try again.", mapped.message)
    }

    @Test
    fun mapFirebaseAuthException_networkAndRateLimit_returnActionableStateErrors() {
        val networkMapped = mapFirebaseAuthException(FirebaseNetworkException("No connection"))
        val rateMapped = mapFirebaseAuthException(FirebaseTooManyRequestsException("Blocked"))

        assertTrue(networkMapped is IllegalStateException)
        assertTrue(networkMapped.message!!.contains("Network error"))
        assertTrue(rateMapped is IllegalStateException)
        assertTrue(rateMapped.message!!.contains("Too many attempts"))
    }

    @Test(expected = CancellationException::class)
    fun mapFirebaseAuthException_cancellationException_isRethrown() {
        mapFirebaseAuthException(CancellationException("cancelled"))
    }
}
