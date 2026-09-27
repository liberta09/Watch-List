package com.kaan.watchlist.data.repository

import android.util.Patterns
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Firebase Authentication (e-posta / şifre) ile giriş ve kayıt.
 * Şifreler uygulamada saklanmaz; doğrulamayı Firebase yapar.
 */
object AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val isSignedIn: Boolean get() = auth.currentUser != null

    val currentUserName: String?
        get() = auth.currentUser?.let { it.displayName ?: it.email }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            onError("E-posta ve şifre boş bırakılamaz.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onError("Geçerli bir e-posta adresi girin.")
            return
        }

        auth.signInWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(toMessage(e)) }
    }

    fun register(
        email: String,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanUser = username.trim()

        when {
            cleanEmail.isEmpty() || cleanUser.isEmpty() || password.isEmpty() ->
                return onError("Lütfen tüm alanları doldurun.")
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() ->
                return onError("Geçerli bir e-posta adresi girin.")
            cleanUser.length < 3 ->
                return onError("Kullanıcı adı en az 3 karakter olmalı.")
            password.length < 6 ->
                return onError("Şifre en az 6 karakter olmalı.")
        }

        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener { result ->
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanUser)
                    .build()
                // Kullanıcı adı kaydedilemese bile hesap açılmıştır; akışı durdurmuyoruz.
                result.user?.updateProfile(profile)
                    ?.addOnCompleteListener { onSuccess() }
                    ?: onSuccess()
            }
            .addOnFailureListener { e -> onError(toMessage(e)) }
    }

    fun logout() {
        auth.signOut()
    }

    private fun toMessage(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Şifre çok zayıf, en az 6 karakter kullanın."
        is FirebaseAuthUserCollisionException -> "Bu e-posta ile zaten bir hesap var."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "E-posta veya şifre hatalı."
        is FirebaseTooManyRequestsException -> "Çok fazla deneme yapıldı, biraz sonra tekrar deneyin."
        is FirebaseNetworkException -> "İnternet bağlantısı yok, bağlantınızı kontrol edin."
        else -> if (e.message?.contains("CONFIGURATION_NOT_FOUND") == true ||
            e.message?.contains("OPERATION_NOT_ALLOWED") == true
        ) {
            "Firebase'de e-posta/şifre girişi etkin değil."
        } else {
            "Bir hata oluştu: ${e.localizedMessage ?: "bilinmeyen hata"}"
        }
    }
}
