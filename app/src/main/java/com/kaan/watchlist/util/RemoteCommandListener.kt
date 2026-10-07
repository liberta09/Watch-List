package com.kaan.watchlist.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.kaan.watchlist.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Kontrol panelinden gelen komutları dinler.
 * Panel her cihaz için tek bir komut yazar: commands/{deviceId} = { action, message?, executed, timestamp }
 * Uygulama komutu çalıştırdıktan sonra yalnızca "executed" alanını true yapar.
 */
object RemoteCommandListener {

    private var isListening = false

    @SuppressLint("HardwareIds")
    fun startListening(context: Context) {
        if (isListening) return
        isListening = true

        val appContext = context.applicationContext
        val deviceId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_device"

        val commandRef = FirebaseDatabase.getInstance().getReference("commands/$deviceId")

        Log.d("FirebaseCommand", "Komut dinleyicisi başlatıldı. Cihaz ID: $deviceId")

        commandRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val action = snapshot.child("action").getValue(String::class.java) ?: return
                val executed = snapshot.child("executed").getValue(Boolean::class.java) ?: false
                if (executed) return

                Log.d("FirebaseCommand", "Yeni komut alındı: $action")
                // Komutu, "executed" işareti sunucuya yazıldıktan sonra çalıştırıyoruz;
                // aksi halde yeniden başlatma komutu açılışta tekrar tekrar çalışabilir.
                snapshot.ref.child("executed").setValue(true)
                    .addOnSuccessListener { handleCommand(appContext, action, snapshot) }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseCommand", "Komut işaretlenemedi, çalıştırılmadı: ${e.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                // Oturum değişiminde (ör. çıkış yapınca) dinleyici iptal olabilir;
                // yeni oturum hazır olduğunda startListening tekrar çağrılır.
                Log.e("FirebaseCommand", "Komut dinleme hatası: ${error.message}")
                isListening = false
            }
        })
    }

    private fun handleCommand(context: Context, action: String, command: DataSnapshot) {
        Log.d("FirebaseCommand", "Komut işleniyor: $action")
        when (action) {
            "show_message", "show_toast" -> {
                val message = command.child("message").getValue(String::class.java)
                    ?: context.getString(R.string.remote_command_executed)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
            "restart_app" -> {
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }
            }
            else -> {
                Log.w("FirebaseCommand", "Bu sürümde desteklenmeyen komut: $action")
            }
        }
    }
}
