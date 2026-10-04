package com.kaan.watchlist

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.kaan.watchlist.data.repository.AuthRepository
import com.kaan.watchlist.navigation.SetupNavGraph
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.DarkSurface
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.ui.theme.WatchListTheme
import com.kaan.watchlist.util.PresenceManager
import com.kaan.watchlist.util.RemoteCommandListener
import com.kaan.watchlist.util.UpdateHelper
import kotlinx.coroutines.delay

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.kaan.watchlist.util.UpcomingScheduler
import android.content.Context

class MainActivity : ComponentActivity() {
    private val openMediaId = mutableStateOf<Int?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getIntExtra("open_media_id", -1) ?: -1
        if (id != -1) {
            openMediaId.value = id
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        
        val prefs = getSharedPreferences("watchlist_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("notifications_enabled", true)) {
            UpcomingScheduler.schedule(this)
        }
        try {
            val appContext = applicationContext
            // Veritabanı kuralları oturum istediği için servisleri oturum hazır olunca başlatıyoruz.
            AuthRepository.startBackgroundSession {
                try {
                    PresenceManager.initPresence(appContext)
                    RemoteCommandListener.startListening(appContext)
                } catch (e: Exception) {
                    Log.e("Firebase", "Failed to initialize Firebase services", e)
                }
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Failed to start Firebase session", e)
        }
        
        enableEdgeToEdge()
        setContent {
            WatchListTheme {
                val navController = rememberNavController()
                val downloadState by UpdateHelper.downloadState.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    SetupNavGraph(
                        navController = navController,
                        initialMediaId = openMediaId.value,
                        onInitialMediaIdHandled = { openMediaId.value = null }
                    )

                    when (val state = downloadState) {
                        is UpdateHelper.DownloadState.Downloading -> {
                            // Optionally show a small toast or overlay
                            LaunchedEffect(Unit) {
                                Toast.makeText(this@MainActivity, "Güncelleme indiriliyor...", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is UpdateHelper.DownloadState.Completed -> {
                            Log.d("UpdateDebug", "Dialog gösterilmeye çalışılıyor")
                            AlertDialog(
                                onDismissRequest = { },
                                title = {
                                    Text(
                                        text = "✅ İndirme Tamamlandı",
                                        color = LightText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Güncelleme dosyası hazır. Kuruluma geçiliyor...",
                                        color = LightText.copy(alpha = 0.85f),
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            UpdateHelper.resetDownloadState()
                                            UpdateHelper.promptInstall(this@MainActivity, state.apkFile)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.tvFocusable(shape = RoundedCornerShape(8.dp), onClick = {
                                            UpdateHelper.resetDownloadState()
                                            UpdateHelper.promptInstall(this@MainActivity, state.apkFile)
                                        })
                                    ) {
                                        Text("Kur (Install)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                },
                                containerColor = DarkSurface,
                                shape = RoundedCornerShape(16.dp)
                            )

                            // Automatically trigger the prompt after showing the dialog
                            LaunchedEffect(state) {
                                delay(1000)
                                UpdateHelper.resetDownloadState()
                                UpdateHelper.promptInstall(this@MainActivity, state.apkFile)
                            }
                        }
                        is UpdateHelper.DownloadState.Error -> {
                            LaunchedEffect(state) {
                                Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                                UpdateHelper.resetDownloadState()
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        UpdateHelper.isAppInForeground = true
        UpdateHelper.checkAndPromptPendingInstall(this)
        
        try {
            PresenceManager.initPresence(this)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onPause() {
        super.onPause()
        UpdateHelper.isAppInForeground = false
        
        try {
            PresenceManager.setOffline(this)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_MOUSE != 0) {
            if (event.action == MotionEvent.ACTION_BUTTON_PRESS && 
                event.buttonState == MotionEvent.BUTTON_PRIMARY) {
                
                val downTime = event.eventTime
                val eventTime = event.eventTime
                val x = event.x
                val y = event.y

                val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
                dispatchTouchEvent(downEvent)
                downEvent.recycle()

                val upEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0)
                dispatchTouchEvent(upEvent)
                upEvent.recycle()

                return true
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }
}
