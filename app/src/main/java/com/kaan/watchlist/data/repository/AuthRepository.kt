package com.kaan.watchlist.data.repository

import android.content.Context
import android.util.Patterns
import com.kaan.watchlist.R
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

    /** E-posta ile giriş yapmış gerçek bir kullanıcı var mı (anonim oturum sayılmaz). */
    val isSignedIn: Boolean get() = auth.currentUser?.isAnonymous == false

    private var backgroundListener: FirebaseAuth.AuthStateListener? = null

    /**
     * Veritabanı kuralları yazma için oturum istediğinden, e-postayla giriş yapılmamışken
     * (misafir modu, giriş ekranı, çıkış sonrası) uygulama Firebase'e anonim olarak bağlanır.
     * Her oturum hazır olduğunda [onReady] çağrılır.
     */
    fun startBackgroundSession(onReady: () -> Unit) {
        if (backgroundListener != null) return
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                firebaseAuth.signInAnonymously()
                    .addOnFailureListener { e ->
                        android.util.Log.e("AuthRepository", "Anonim oturum açılamadı", e)
                    }
            } else {
                onReady()
            }
        }
        backgroundListener = listener
        auth.addAuthStateListener(listener)
    }

    val currentUserName: String?
        get() = auth.currentUser?.takeIf { !it.isAnonymous }?.let { it.displayName ?: it.email }

    fun login(
        email: String,
        password: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            onError(context.getString(R.string.auth_err_empty))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onError(context.getString(R.string.auth_err_invalid_email))
            return
        }

        auth.signInWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(toMessage(e, context)) }
    }

    fun register(
        email: String,
        username: String,
        password: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanUser = username.trim()

        when {
            cleanEmail.isEmpty() || cleanUser.isEmpty() || password.isEmpty() ->
                return onError(context.getString(R.string.auth_err_fill_all))
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() ->
                return onError(context.getString(R.string.auth_err_invalid_email))
            cleanUser.length < 3 ->
                return onError(context.getString(R.string.auth_err_user_short))
            password.length < 6 ->
                return onError(context.getString(R.string.auth_err_pass_short))
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
            .addOnFailureListener { e -> onError(toMessage(e, context)) }
    }

    /** E-posta oturumunu kapatır; arka plan oturumu ardından otomatik olarak anonim açılır. */
    fun logout() {
        if (auth.currentUser?.isAnonymous == true) return
        auth.signOut()
    }

    private fun toMessage(e: Exception, context: Context): String = when (e) {
        is FirebaseAuthWeakPasswordException -> context.getString(R.string.auth_err_weak_pass)
        is FirebaseAuthUserCollisionException -> context.getString(R.string.auth_err_collision)
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> context.getString(R.string.auth_err_invalid_creds)
        is FirebaseTooManyRequestsException -> context.getString(R.string.auth_err_too_many)
        is FirebaseNetworkException -> context.getString(R.string.auth_err_network)
        else -> if (e.message?.contains("CONFIGURATION_NOT_FOUND") == true ||
            e.message?.contains("OPERATION_NOT_ALLOWED") == true
        ) {
            context.getString(R.string.auth_err_not_enabled)
        } else {
            context.getString(R.string.auth_err_unknown, e.localizedMessage ?: "unknown error")
        }
    }
}
