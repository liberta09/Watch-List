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
            Toast.makeText(context, context.getString(R.string.toast_notification_permission_denied), Toast.LENGTH_SHORT).show()
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
                    text = stringResource(R.string.settings_app_update),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_current_version, BuildConfig.VERSION_NAME),
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
                            Text(stringResource(R.string.settings_check_update), color = LightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                            Text(stringResource(R.string.settings_checking), color = LightText, fontSize = 15.sp)
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
                            Text(stringResource(R.string.settings_up_to_date), color = LightText, fontSize = 15.sp)
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
                            Text(stringResource(R.string.settings_recheck), color = LightText, fontSize = 14.sp)
                        }
                    }
                    is UpdateStatus.UpdateAvailable -> {
                        Text(
                            text = stringResource(R.string.settings_new_version_available, status.version),
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
                            Text(stringResource(R.string.settings_download_update), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                            Text(stringResource(R.string.general_retry), color = LightText, fontSize = 14.sp)
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
                Text(stringResource(R.string.settings_notifications_enable), color = LightText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
                        Toast.makeText(context, context.getString(R.string.toast_notifications_disabled), Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .tvFocusable(shape = RoundedCornerShape(12.dp))
            ) {
                Text(text = stringResource(R.string.settings_test_notification), color = LightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
    
            Spacer(modifier = Modifier.height(12.dp))
    
            Button(
                onClick = {
                    com.kaan.watchlist.util.UpcomingScheduler.checkNow(context)
                    Toast.makeText(context, context.getString(R.string.toast_check_started), Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .tvFocusable(shape = RoundedCornerShape(12.dp))
            ) {
                Text(text = stringResource(R.string.settings_check_upcoming_now), color = LightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                    text = stringResource(R.string.settings_share_list),
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
                    Text(stringResource(R.string.settings_share_my_list), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showOpenDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.settings_open_with_code), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showMySharesDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.settings_share_my_shares), color = Color.White, fontWeight = FontWeight.Bold)
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
                title = { Text(stringResource(R.string.settings_share_my_list), color = LightText, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(stringResource(R.string.settings_share_desc), color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(stringResource(R.string.settings_share_list_name), color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LightText,
                                unfocusedTextColor = LightText,
                                focusedBorderColor = BlueAccent
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.settings_share_content), color = LightText)
                        val filters = listOf(
                            stringResource(R.string.settings_share_filter_all) to com.kaan.watchlist.domain.model.ShareFilter.ALL,
                            stringResource(R.string.settings_share_filter_watchlist) to com.kaan.watchlist.domain.model.ShareFilter.WATCHLIST,
                            stringResource(R.string.settings_share_filter_watched) to com.kaan.watchlist.domain.model.ShareFilter.WATCHED,
                            stringResource(R.string.settings_share_filter_movies) to com.kaan.watchlist.domain.model.ShareFilter.MOVIES,
                            stringResource(R.string.settings_share_filter_shows) to com.kaan.watchlist.domain.model.ShareFilter.SHOWS
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
                                    putExtra(Intent.EXTRA_TEXT, context.resources.getString(R.string.settings_share_list_msg, code))
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            } else {
                                Toast.makeText(context, context.getString(R.string.toast_share_list_failed), Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Text(stringResource(R.string.settings_share_btn), color = BlueAccent)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showShareDialog = false }) {
                        Text(stringResource(R.string.general_cancel), color = LightText)
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
                title = { Text(stringResource(R.string.settings_open_with_code), color = LightText, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        androidx.compose.material3.OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.uppercase().replace(" ", "") },
                            label = { Text(stringResource(R.string.settings_open_code_label), color = Color.Gray) },
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
                                    Toast.makeText(context, context.getString(R.string.toast_invalid_share_code), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Text(stringResource(R.string.settings_open_btn), color = BlueAccent)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showOpenDialog = false }) {
                        Text(stringResource(R.string.general_cancel), color = LightText)
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
                title = { Text(stringResource(R.string.settings_share_my_shares), color = LightText, fontWeight = FontWeight.Bold) },
                text = {
                    if (myShares == null) {
                        CircularProgressIndicator(color = BlueAccent)
                    } else if (myShares!!.isEmpty()) {
                        Text(stringResource(R.string.settings_no_active_shares), color = LightText)
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState())) {
                            myShares!!.forEach { share ->
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(DarkNavy, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                    Column {
                                        Text(share.title.ifBlank { stringResource(R.string.settings_unnamed_list) }, color = LightText, fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.settings_share_code, share.code), color = Color.Gray, fontSize = 12.sp)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            androidx.compose.material3.TextButton(onClick = {
                                                val sendIntent: Intent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, context.resources.getString(R.string.settings_share_list_msg, share.code))
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, null)
                                                context.startActivity(shareIntent)
                                            }) {
                                                Text(stringResource(R.string.general_copy), color = BlueAccent, fontSize = 12.sp)
                                            }
                                            androidx.compose.material3.TextButton(onClick = {
                                                viewModel.removeSharedList(share.code) {
                                                    viewModel.getMySharedLists { myShares = it }
                                                }
                                            }) {
                                                Text(stringResource(R.string.general_close), color = Color(0xFFFF6B6B), fontSize = 12.sp)
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
                        Text(stringResource(R.string.general_close), color = BlueAccent)
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
                    text = stringResource(R.string.settings_trakt_sync_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!traktConfigured) {
                    Text(
                        text = stringResource(R.string.settings_trakt_not_configured),
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
                        Text(stringResource(R.string.settings_trakt_connect), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.settings_trakt_connected), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.disconnectTrakt() },
                            modifier = Modifier.tvFocusable()
                        ) {
                            Text(stringResource(R.string.settings_trakt_disconnect_btn), color = Color(0xFFFF6B6B), fontSize = 14.sp)
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
                        Text(stringResource(R.string.settings_trakt_import_btn), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Auth Dialog
        if (deviceCode != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.cancelTraktAuth() },
                containerColor = DarkSurface,
                title = { Text(stringResource(R.string.settings_trakt_connection_title), color = LightText, fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.settings_trakt_activation_desc), color = LightText, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                        Text(stringResource(R.string.settings_trakt_waiting), color = LightText.copy(alpha=0.7f), fontSize = 12.sp)
                    }
                },
                confirmButton = {},
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { viewModel.cancelTraktAuth() }, modifier = Modifier.tvFocusable()) {
                        Text(stringResource(R.string.general_cancel), color = LightText)
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
                    title = { Text(stringResource(R.string.settings_trakt_importing_title), color = LightText, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(stringResource(R.string.settings_trakt_importing_desc, state.done, state.total), color = LightText)
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
                            Text(stringResource(R.string.settings_trakt_import_failed), color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                        } else {
                            Text(stringResource(R.string.settings_trakt_import_success), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            if (state.result.errorMessage != null) {
                                Text(state.result.errorMessage, color = LightText)
                            } else {
                                Text(stringResource(R.string.settings_trakt_items_added, state.result.itemsAdded), color = LightText)
                                Text(stringResource(R.string.settings_trakt_episodes_added, state.result.episodesAdded), color = LightText)
                                Text(stringResource(R.string.settings_trakt_items_updated, state.result.itemsUpdated), color = LightText)
                                if (state.result.unmatched > 0) {
                                    Text(stringResource(R.string.settings_trakt_unmatched, state.result.unmatched), color = Color(0xFFFF6B6B))
                                }
                            }
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { viewModel.resetTraktImportState() }, modifier = Modifier.tvFocusable()) {
                            Text(stringResource(R.string.general_ok), color = BlueAccent, fontWeight = FontWeight.Bold)
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
            Text(text = stringResource(R.string.settings_logout), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
