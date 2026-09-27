package com.kaan.watchlist.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object RemoteCommandListener {

    private var isListening = false

    @SuppressLint("HardwareIds")
    fun startListening(context: Context) {
        if (isListening) return
        isListening = true

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_device"

        val database = FirebaseDatabase.getInstance()
        val commandsRef = database.getReference("commands/$deviceId")

        Log.d("FirebaseCommand", "Komut dinleyicisi başlatıldı. Cihaz ID: $deviceId")

        commandsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // If there are new commands
                for (commandSnapshot in snapshot.children) {
                    val commandData = commandSnapshot.value as? Map<*, *> ?: continue
                    val action = commandData["action"] as? String
                    val executed = commandData["executed"] as? Boolean ?: false

                    if (!executed && action != null) {
                        Log.d("FirebaseCommand", "Yeni komut alındı: $action (ID: ${commandSnapshot.key})")
                        handleCommand(context, action, commandData)
                        // Mark as executed
                        commandSnapshot.ref.child("executed").setValue(true)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseCommand", "Komut dinleme hatası: ${error.message}")
            }
        })
    }

    private fun handleCommand(context: Context, action: String, payload: Map<*, *>) {
        Log.d("FirebaseCommand", "Komut işleniyor: $action")
        when (action) {
            "show_toast" -> {
                val message = payload["message"] as? String ?: "Remote command executed"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
            // Add more remote commands here
            else -> {
                Log.w("FirebaseCommand", "Bilinmeyen komut: $action")
            }
        }
    }
}
