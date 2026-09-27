package com.kaan.watchlist.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

object PresenceManager {

    private var isInitialized = false

    @SuppressLint("HardwareIds")
    fun initPresence(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_device"

        val database = FirebaseDatabase.getInstance()
        val myConnectionsRef = database.getReference("devices/$deviceId/online")
        val lastSeenRef = database.getReference("devices/$deviceId/lastSeen")
        val connectedRef = database.getReference(".info/connected")
        val historyRef = database.getReference("history/$deviceId")

        Log.d("FirebasePresence", "Firebase bağlantısı başlatıldı. Cihaz ID: $deviceId")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Log.d("FirebasePresence", "Cihaz Firebase'e bağlandı: $deviceId")
                    
                    // When this device disconnects, remove it
                    myConnectionsRef.onDisconnect().setValue(false)
                    // When I disconnect, update the last time I was seen online
                    lastSeenRef.onDisconnect().setValue(ServerValue.TIMESTAMP)
                    
                    // Add this device to my connections list
                    myConnectionsRef.setValue(true)
                    lastSeenRef.setValue(ServerValue.TIMESTAMP)

                    Log.d("FirebasePresence", "Cihaz kaydedildi: $deviceId (online: true)")

                    // Log login event
                    historyRef.push().setValue(
                        mapOf(
                            "event" to "login",
                            "timestamp" to ServerValue.TIMESTAMP
                        )
                    )

                    // Log logout event on disconnect
                    historyRef.push().onDisconnect().setValue(
                        mapOf(
                            "event" to "logout",
                            "timestamp" to ServerValue.TIMESTAMP
                        )
                    )
                } else {
                    Log.d("FirebasePresence", "Cihaz Firebase'den koptu (veya bağlanıyor).")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebasePresence", "Firebase bağlantı hatası: ${error.message}")
            }
        })
    }

    @SuppressLint("HardwareIds")
    fun setOffline(context: Context) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_device"
        
        android.util.Log.d("FirebasePresence", "Uygulama arkaplana atıldı, setOffline çağrılıyor: $deviceId")

        val database = FirebaseDatabase.getInstance()
        database.getReference("devices/$deviceId/online").setValue(false)
        database.getReference("devices/$deviceId/lastSeen").setValue(ServerValue.TIMESTAMP)
        
        database.getReference("history/$deviceId").push().setValue(
            mapOf(
                "event" to "logout",
                "timestamp" to ServerValue.TIMESTAMP
            )
        )
    }
}
