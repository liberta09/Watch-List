package com.kaan.watchlist.screens.tabs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kaan.watchlist.BuildConfig
import com.kaan.watchlist.data.repository.UpdateStatus
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.DarkNavy
import com.kaan.watchlist.ui.theme.DarkSurface
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.util.NotificationHelper
import com.kaan.watchlist.viewmodel.MediaViewModel

@Composable
fun SettingsTab(
    viewModel: MediaViewModel,
    onLogout: () -> Unit,
    onOpenTelegramWeb: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(true)
            NotificationHelper.sendTestNotification(context)
        } else {
            viewModel.setNotificationsEnabled(false)
            Toast.makeText(context, "Bildirim izni reddedildi.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ayarlar",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = LightText
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Telegram Section Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Telegram Kanalımız",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Geliştirmeler, duyurular ve güncellemeler için kanalımıza katıl.",
                    fontSize = 14.sp,
                    color = LightText.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val onTelegramClick = {
                    if (onOpenTelegramWeb != null) {
                        onOpenTelegramWeb()
                    } else {
                        val telegramUrl = "https://t.me/+o-RFlV4U3UY5NGU8"
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Telegram bağlantısı açılamadı.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Button(
                    onClick = { onTelegramClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = { onTelegramClick() })
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Telegram",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Telegram Kanalımıza Katıl", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Update Section Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Uygulama Güncellemesi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mevcut Sürüm: Sürüm ${BuildConfig.VERSION_NAME}",
                    fontSize = 14.sp,
                    color = LightText.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                when (val status = updateStatus) {
                    is UpdateStatus.Idle -> {
                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Text("Güncellemeleri Kontrol Et", color = LightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    is UpdateStatus.Checking -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(DarkNavy, RoundedCornerShape(12.dp)),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BlueAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Kontrol ediliyor...", color = LightText, fontSize = 15.sp)
                        }
                    }
                    is UpdateStatus.UpToDate -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uygulamanız güncel.", color = LightText, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Text("Yeniden Kontrol Et", color = LightText, fontSize = 14.sp)
                        }
                    }
                    is UpdateStatus.UpdateAvailable -> {
                        Text(
                            text = "Yeni sürüm mevcut: ${status.version}",
                            color = BlueAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri(status.downloadUrl)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "İndirme adresi açılamadı.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Text("Güncellemeyi İndir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    is UpdateStatus.Error -> {
                        Text(
                            text = status.message,
                            color = Color(0xFFFF6B6B),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Text("Tekrar Dene", color = LightText, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notifications Toggle Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .tvFocusable(shape = RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Bildirimleri Aç", color = LightText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setNotificationsEnabled(isChecked)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BlueAccent,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = DarkNavy
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (notificationsEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        NotificationHelper.sendTestNotification(context)
                    }
                } else {
                    Toast.makeText(context, "Bildirimler kapalı. Lütfen önce bildirimleri açın.", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .tvFocusable(shape = RoundedCornerShape(12.dp))
        ) {
            Text(text = "Test Bildirimi Gönder", color = LightText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .tvFocusable(shape = RoundedCornerShape(12.dp))
        ) {
            Text(text = "Çıkış Yap", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
