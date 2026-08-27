package com.pransetu.app.core.auth

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/**
 * Production Firebase Phone Authentication & OTP Service for PRANSETU.
 * Handles real SMS OTP dispatch, auto-retrieval, credential verification, and session management.
 * NO test/demo/fallback modes — every phone number undergoes real SMS verification.
 */
object FirebasePhoneAuthManager {
    private const val TAG = "FirebasePhoneAuth"
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    /**
     * Dispatches a real SMS OTP to the citizen's phone number via Firebase Authentication.
     */
    fun sendOtp(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerified: (user: FirebaseUser?) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        val formattedPhone = formatPhoneNumber(phoneNumber)
        Log.d(TAG, "Initiating Firebase Phone OTP verification for: $formattedPhone")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "Instant auto-verification completed by Google Play Services")
                signInWithCredential(credential, onSuccess = { onAutoVerified(it) }, onError = { onError(it) })
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Firebase phone verification FAILED: ${e.message}", e)
                onError(e.message ?: "Phone verification failed. Please check your number and try again.")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "Firebase SMS OTP dispatched successfully [VerificationID: $verificationId]")
                resendToken = token
                onCodeSent(verificationId)
            }
        }

        try {
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting phone verification", e)
            onError(e.message ?: "Could not initiate phone verification. Please try again.")
        }
    }

    /**
     * Resends an SMS OTP using the preserved ForceResendingToken.
     */
    fun resendOtp(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerified: (user: FirebaseUser?) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        val formattedPhone = formatPhoneNumber(phoneNumber)
        val token = resendToken

        if (token == null) {
            sendOtp(activity, phoneNumber, onCodeSent, onAutoVerified, onError)
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential, onSuccess = { onAutoVerified(it) }, onError = { onError(it) })
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Firebase resend FAILED: ${e.message}", e)
                onError(e.message ?: "Failed to resend OTP. Please try again.")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                resendToken = token
                onCodeSent(verificationId)
            }
        }

        try {
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .setForceResendingToken(token)

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            onError(e.message ?: "Could not resend OTP. Please try again.")
        }
    }

    /**
     * Verifies the 6-digit OTP code entered by the citizen against Firebase Auth.
     */
    fun verifyOtp(
        verificationId: String,
        code: String,
        onSuccess: (user: FirebaseUser?) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        val cleanCode = code.trim()
        if (cleanCode.length < 6) {
            onError("Please enter the complete 6-digit verification code.")
            return
        }

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, cleanCode)
            signInWithCredential(
                credential = credential,
                onSuccess = onSuccess,
                onError = { firebaseErr -> onError(firebaseErr) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error constructing credential", e)
            onError(e.localizedMessage ?: "Invalid verification code.")
        }
    }

    private fun signInWithCredential(
        credential: PhoneAuthCredential,
        onSuccess: (user: FirebaseUser?) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Firebase phone authentication success for UID: ${user?.uid}")
                    onSuccess(user)
                } else {
                    val ex = task.exception
                    Log.e(TAG, "Firebase sign in with credential failed", ex)
                    val msg = if (ex is FirebaseAuthInvalidCredentialsException) {
                        "Incorrect OTP code. Please check the SMS and retry."
                    } else {
                        ex?.localizedMessage ?: "OTP verification failed. Please try again."
                    }
                    onError(msg)
                }
            }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out of Firebase Auth", e)
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        val digits = phone.trim().replace(Regex("[^0-9+]"), "")
        return if (digits.startsWith("+")) {
            digits
        } else if (digits.length == 10) {
            "+91$digits"
        } else {
            "+$digits"
        }
    }
}
