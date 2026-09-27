package com.kaan.watchlist.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cihaz üzerinde yerel hesap yönetimi.
 * Şifreler düz metin olarak değil, rastgele tuz (salt) + PBKDF2 ile hash'lenerek saklanır.
 */
class AuthRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("watchlist_auth", Context.MODE_PRIVATE)

    sealed class Result {
        object Success : Result()
        data class Error(val message: String) : Result()
    }

    fun register(email: String, username: String, password: String): Result {
        val cleanEmail = email.trim()
        val cleanUser = username.trim()

        if (cleanEmail.isEmpty() || cleanUser.isEmpty() || password.isEmpty()) {
            return Result.Error("Lütfen tüm alanları doldurun.")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.Error("Geçerli bir e-posta adresi girin.")
        }
        if (cleanUser.length < 3) {
            return Result.Error("Kullanıcı adı en az 3 karakter olmalı.")
        }
        if (password.length < 6) {
            return Result.Error("Şifre en az 6 karakter olmalı.")
        }

        val key = userKey(cleanUser)
        if (prefs.contains(key)) {
            return Result.Error("Bu kullanıcı adı zaten alınmış.")
        }

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(password, salt)

        val json = JSONObject().apply {
            put("username", cleanUser)
            put("email", cleanEmail)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
        }
        prefs.edit().putString(key, json.toString()).apply()
        return Result.Success
    }

    fun login(username: String, password: String): Result {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty() || password.isEmpty()) {
            return Result.Error("Kullanıcı adı ve şifre boş bırakılamaz.")
        }

        val stored = prefs.getString(userKey(cleanUser), null)
            ?: return Result.Error("Kullanıcı adı veya şifre hatalı.")

        return try {
            val json = JSONObject(stored)
            val salt = Base64.decode(json.getString("salt"), Base64.NO_WRAP)
            val expected = Base64.decode(json.getString("hash"), Base64.NO_WRAP)
            val actual = hashPassword(password, salt)
            if (MessageDigest.isEqual(expected, actual)) {
                prefs.edit().putString(KEY_CURRENT_USER, json.getString("username")).apply()
                Result.Success
            } else {
                Result.Error("Kullanıcı adı veya şifre hatalı.")
            }
        } catch (e: Exception) {
            Result.Error("Hesap bilgisi okunamadı.")
        }
    }

    fun logout() {
        prefs.edit().remove(KEY_CURRENT_USER).apply()
    }

    fun currentUser(): String? = prefs.getString(KEY_CURRENT_USER, null)

    private fun userKey(username: String) = "user_" + username.lowercase()

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        // PBKDF2WithHmacSHA1, minSdk 24 ile uyumlu olduğu için seçildi.
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        private const val KEY_CURRENT_USER = "current_user"
        private const val ITERATIONS = 20_000
        private const val KEY_LENGTH_BITS = 256
    }
}
