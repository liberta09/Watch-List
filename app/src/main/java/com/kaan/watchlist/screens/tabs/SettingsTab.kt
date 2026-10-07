package com.kaan.watchlist.screens.tabs

import androidx.compose.runtime.LaunchedEffect

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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
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
import com.kaan.watchlist.util.UpdateHelper
import com.kaan.watchlist.viewmodel.MediaViewModel
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.res.stringResource
import com.kaan.watchlist.R

@Composable
fun SettingsTab(
    viewModel: MediaViewModel,
    onLogout: () -> Unit,
    onOpenTelegramWeb: (() -> Unit)? = null,
    onNavigateToSharedList: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

            val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(true)
            com.kaan.watchlist.util.UpcomingScheduler.schedule(context)
            NotificationHelper.sendTestNotification(context)
        } else {
            viewModel.setNotificationsEnabled(false)
            com.kaan.watchlist.util.UpcomingScheduler.cancel(context)
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
            text = stringResource(R.string.home_tab_settings),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = LightText
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- Language Section ---
        var showLanguageDialog by remember { mutableStateOf(false) }
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentLangCode = if (currentLocales.isEmpty) "system" else currentLocales.get(0)?.language ?: "system"
        val currentLangLabel = when (currentLangCode) {
            "tr" -> stringResource(R.string.settings_lang_tr)
            "en" -> stringResource(R.string.settings_lang_en)
            else -> stringResource(R.string.settings_lang_system)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .clickable { showLanguageDialog = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Text(
                    text = currentLangLabel,
                    fontSize = 16.sp,
                    color = BlueAccent
                )
            }
        }

        if (showLanguageDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                containerColor = DarkSurface,
                title = { Text(stringResource(R.string.settings_language), color = LightText, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        val options = listOf(
                            "system" to stringResource(R.string.settings_lang_system),
                            "tr" to stringResource(R.string.settings_lang_tr),
                            "en" to stringResource(R.string.settings_lang_en)
                        )
                        options.forEach { (code, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showLanguageDialog = false
                                        val locales = if (code == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(code)
                                        AppCompatDelegate.setApplicationLocales(locales)
                                        // Cache temizliği ve refresh viewmodel üzerinden yapılmalı, bunu birazdan viewmodela ekleyeceğiz
                                        viewModel.onLanguageChanged()
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = currentLangCode == code,
                                    onClick = null,
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = BlueAccent)
                                )
                                Text(label, color = LightText, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {}
            )
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
                    text = "Mevcut Sürüm: Sürüm ${BuildConfig.VERSION_NAME} ✨",
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
                        val onDownload = {
                            UpdateHelper.downloadAndInstallApk(context, status.downloadUrl)
                        }
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = onDownload)
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
        val isTvDevice = com.kaan.watchlist.util.DeviceUtils.isTv(context)

        if (!isTvDevice) {
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
                            if (isChecked) com.kaan.watchlist.util.UpcomingScheduler.schedule(context)
                            else com.kaan.watchlist.util.UpcomingScheduler.cancel(context)
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
                Text(text = "Test Bildirimi Gönder", color = LightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
    
            Spacer(modifier = Modifier.height(12.dp))
    
            Button(
                onClick = {
                    com.kaan.watchlist.util.UpcomingScheduler.checkNow(context)
                    Toast.makeText(context, "Kontrol başlatıldı", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .tvFocusable(shape = RoundedCornerShape(12.dp))
            ) {
                Text(text = "Yaklaşanları şimdi kontrol et", color = LightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Paylaşım Kartı ---
        var showShareDialog by remember { mutableStateOf(false) }
        var showOpenDialog by remember { mutableStateOf(false) }
        var showMySharesDialog by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Liste Paylaşımı",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showShareDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Listemi Paylaş", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showOpenDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Kod ile Liste Aç", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showMySharesDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Paylaşımlarım", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showShareDialog) {
            var title by rememberSaveable { mutableStateOf("") }
            var filter by remember { mutableStateOf(com.kaan.watchlist.domain.model.ShareFilter.ALL) }
            var loading by remember { mutableStateOf(false) }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showShareDialog = false },
                containerColor = DarkSurface,
                title = { Text("Listemi Paylaş", color = LightText, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Bu kodu bilen herkes listeni görebilir.", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Liste Adı", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LightText,
                                unfocusedTextColor = LightText,
                                focusedBorderColor = BlueAccent
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("İçerik:", color = LightText)
                        val filters = listOf(
                            "Hepsi" to com.kaan.watchlist.domain.model.ShareFilter.ALL,
                            "Sadece İzlenecekler" to com.kaan.watchlist.domain.model.ShareFilter.WATCHLIST,
                            "Sadece İzlenenler" to com.kaan.watchlist.domain.model.ShareFilter.WATCHED,
                            "Sadece Filmler" to com.kaan.watchlist.domain.model.ShareFilter.MOVIES,
                            "Sadece Diziler" to com.kaan.watchlist.domain.model.ShareFilter.SHOWS
                        )
                        filters.forEach { (label, enumValue) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { filter = enumValue }
                                    .padding(vertical = 4.dp)
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = filter == enumValue,
                                    onClick = { filter = enumValue },
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = BlueAccent)
                                )
                                Text(label, color = LightText, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = BlueAccent)
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        loading = true
                        viewModel.shareList(title, filter) { code ->
                            loading = false
                            showShareDialog = false
                            if (code != null) {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Watch List listemi sana gönderdim! Uygulamada Ayarlar > Kod ile Liste Aç bölümüne şu kodu gir: $code")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            } else {
                                Toast.makeText(context, "Liste paylaşılamadı (Sadece giriş yapan kullanıcılar paylaşabilir)", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Text("Paylaş", color = BlueAccent)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showShareDialog = false }) {
                        Text("İptal", color = LightText)
                    }
                }
            )
        }

        if (showOpenDialog) {
            var code by rememberSaveable { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }
            
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showOpenDialog = false },
                containerColor = DarkSurface,
                title = { Text("Kod ile Liste Aç", color = LightText, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        androidx.compose.material3.OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.uppercase().replace(" ", "") },
                            label = { Text("8 Haneli Kod", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LightText,
                                unfocusedTextColor = LightText,
                                focusedBorderColor = BlueAccent
                            )
                        )
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp), color = BlueAccent)
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        if (code.length == 8) {
                            loading = true
                            viewModel.fetchSharedList(code) { list ->
                                loading = false
                                if (list != null) {
                                    showOpenDialog = false
                                    onNavigateToSharedList(code)
                                } else {
                                    Toast.makeText(context, "Bu kod geçersiz veya paylaşım kapatılmış", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Text("Aç", color = BlueAccent)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showOpenDialog = false }) {
                        Text("İptal", color = LightText)
                    }
                }
            )
        }

        if (showMySharesDialog) {
            var myShares by remember { mutableStateOf<List<com.kaan.watchlist.domain.model.SharedListInfo>?>(null) }
            
            LaunchedEffect(Unit) {
                viewModel.getMySharedLists { myShares = it }
            }
            
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showMySharesDialog = false },
                containerColor = DarkSurface,
                title = { Text("Paylaşımlarım", color = LightText, fontWeight = FontWeight.Bold) },
                text = {
                    if (myShares == null) {
                        CircularProgressIndicator(color = BlueAccent)
                    } else if (myShares!!.isEmpty()) {
                        Text("Aktif paylaşımınız yok.", color = LightText)
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState())) {
                            myShares!!.forEach { share ->
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(DarkNavy, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                    Column {
                                        Text(share.title.ifBlank { "İsimsiz Liste" }, color = LightText, fontWeight = FontWeight.Bold)
                                        Text("Kod: ${share.code}", color = Color.Gray, fontSize = 12.sp)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            androidx.compose.material3.TextButton(onClick = {
                                                val sendIntent: Intent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "Watch List listemi sana gönderdim! Uygulamada Ayarlar > Kod ile Liste Aç bölümüne şu kodu gir: ${share.code}")
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, null)
                                                context.startActivity(shareIntent)
                                            }) {
                                                Text("Kopyala", color = BlueAccent, fontSize = 12.sp)
                                            }
                                            androidx.compose.material3.TextButton(onClick = {
                                                viewModel.removeSharedList(share.code) {
                                                    viewModel.getMySharedLists { myShares = it }
                                                }
                                            }) {
                                                Text("Kapat", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showMySharesDialog = false }) {
                        Text("Kapat", color = BlueAccent)
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- Trakt.tv Card ---
        val traktConfigured = viewModel.traktConfigured
        val traktConnected by viewModel.traktConnected.collectAsState()
        val deviceCode by viewModel.traktDeviceCode.collectAsState()
        val importState by viewModel.traktImportState.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.checkTraktConnection()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Trakt.tv Senkronizasyonu",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!traktConfigured) {
                    Text(
                        text = "Trakt yapılandırılmamış.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                } else if (!traktConnected) {
                    Button(
                        onClick = { viewModel.startTraktAuth() },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tvFocusable(shape = RoundedCornerShape(12.dp))
                    ) {
                        Text("Trakt Hesabını Bağla", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✓ Trakt bağlı", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.disconnectTrakt() },
                            modifier = Modifier.tvFocusable()
                        ) {
                            Text("Bağlantıyı Kes", color = Color(0xFFFF6B6B), fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.importFromTrakt() },
                        enabled = importState !is com.kaan.watchlist.viewmodel.ImportState.InProgress,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tvFocusable(shape = RoundedCornerShape(12.dp))
                    ) {
                        Text("Trakt'tan İçe Aktar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Auth Dialog
        if (deviceCode != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.cancelTraktAuth() },
                containerColor = DarkSurface,
                title = { Text("Trakt Bağlantısı", color = LightText, fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("trakt.tv/activate adresine git ve bu kodu gir:", color = LightText, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = deviceCode!!.user_code.uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueAccent,
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.CircularProgressIndicator(color = BlueAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Bekleniyor...", color = LightText.copy(alpha=0.7f), fontSize = 12.sp)
                    }
                },
                confirmButton = {},
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { viewModel.cancelTraktAuth() }, modifier = Modifier.tvFocusable()) {
                        Text("İptal", color = LightText)
                    }
                }
            )
        }

        // Import Dialog
        when (val state = importState) {
            is com.kaan.watchlist.viewmodel.ImportState.InProgress -> {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { /* No dismiss */ },
                    containerColor = DarkSurface,
                    title = { Text("Trakt'tan İçe Aktarılıyor", color = LightText, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("İçe aktarılıyor... ${state.done} / ${state.total}", color = LightText)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.total > 0) {
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { state.done.toFloat() / state.total },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = BlueAccent,
                                    trackColor = DarkNavy
                                )
                            } else {
                                androidx.compose.material3.LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = BlueAccent,
                                    trackColor = DarkNavy
                                )
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
            is com.kaan.watchlist.viewmodel.ImportState.Completed -> {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { viewModel.resetTraktImportState() },
                    containerColor = DarkSurface,
                    title = { 
                        if (state.result.errorMessage != null) {
                            Text("❌ İçe aktarma başarısız", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                        } else {
                            Text("✓ İçe aktarma tamamlandı", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            if (state.result.errorMessage != null) {
                                Text(state.result.errorMessage, color = LightText)
                            } else {
                                Text("📦 ${state.result.itemsAdded} öğe eklendi", color = LightText)
                                Text("▶️ ${state.result.episodesAdded} bölüm izlendi işareti", color = LightText)
                                Text("🔄 ${state.result.itemsUpdated} öğe güncellendi", color = LightText)
                                if (state.result.unmatched > 0) {
                                    Text("⚠️ ${state.result.unmatched} öğe eşleştirilemedi", color = Color(0xFFFF6B6B))
                                }
                            }
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { viewModel.resetTraktImportState() }, modifier = Modifier.tvFocusable()) {
                            Text("Tamam", color = BlueAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
            else -> {}
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
