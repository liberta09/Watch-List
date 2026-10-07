package com.kaan.watchlist.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kaan.watchlist.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kaan.watchlist.data.repository.AuthRepository
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.LightText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dummyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        dummyFocusRequester.requestFocus()
    }

    val onBack = { navController.popBackStack() }

    val onRegister: () -> Unit = onRegister@{
        if (isLoading) return@onRegister
        errorMessage = null
        isLoading = true
        AuthRepository.register(
            email = email,
            username = username,
            password = password,
            context = context,
            onSuccess = {
                isLoading = false
                // Kayıttan sonra oturumu kapatıp kullanıcıyı giriş ekranına döndürüyoruz,
                // böylece şifresiyle bir kez giriş yaparak hesabını doğrulamış olur.
                AuthRepository.logout()
                Toast.makeText(context, context.getString(R.string.toast_account_created), Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            },
            onError = { message ->
                isLoading = false
                errorMessage = message
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.register_title), color = LightText) },
                navigationIcon = {
                    IconButton(
                        onClick = { onBack() },
                        modifier = Modifier.tvFocusable(shape = CircleShape, onClick = { onBack() })
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.general_back), tint = LightText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Dummy focus target to prevent auto-focusing on email text field on launch
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(dummyFocusRequester)
                    .focusable()
            )

            Column(
                modifier = Modifier.widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text(stringResource(R.string.login_email)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable(shape = RoundedCornerShape(12.dp)),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = BlueAccent,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = LightText,
                        unfocusedTextColor = LightText
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    label = { Text(stringResource(R.string.register_username)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable(shape = RoundedCornerShape(12.dp)),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = BlueAccent,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = LightText,
                        unfocusedTextColor = LightText
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text(stringResource(R.string.login_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable(shape = RoundedCornerShape(12.dp)),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = BlueAccent,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = LightText,
                        unfocusedTextColor = LightText
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = msg, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = onRegister),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (isLoading) stringResource(R.string.register_button_loading) else stringResource(R.string.register_button), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
