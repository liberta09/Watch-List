package com.kaan.watchlist.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kaan.watchlist.R
import com.kaan.watchlist.data.repository.AuthRepository
import com.kaan.watchlist.navigation.Screen
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.DarkNavy
import com.kaan.watchlist.ui.theme.DarkSurface
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel

@Composable
fun LoginScreen(navController: NavController, viewModel: MediaViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val dummyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        dummyFocusRequester.requestFocus()
    }

    val onLogin: () -> Unit = onLogin@{
        if (isLoading) return@onLogin
        errorMessage = null
        isLoading = true
        AuthRepository.login(
            email = email,
            password = password,
            onSuccess = {
                isLoading = false
                viewModel.setLoggedIn(true)
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            },
            onError = { message ->
                isLoading = false
                errorMessage = message
            }
        )
    }

    val onRegister = {
        navController.navigate(Screen.Register.route)
    }

    val onGuestLogin = {
        AuthRepository.logout()
        viewModel.setLoggedIn(true)
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Login.route) { inclusive = true }
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
        // Invisible top focus target to prevent auto-focusing on text fields on launch
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
            Image(
                painter = painterResource(id = R.drawable.ic_watchlist_logo),
                contentDescription = "Watch List Logo",
                modifier = Modifier.size(90.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Watch List",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = BlueAccent
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Kataloğunu oluştur, keşfet ve izle.",
                fontSize = 15.sp,
                color = LightText
            )
            
            Spacer(modifier = Modifier.height(28.dp))

            // 1. E-posta
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
            
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Şifre
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

            // 3. Hata Mesajı
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = msg, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Giriş Yap
            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = onLogin),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = if (isLoading) stringResource(R.string.login_button_loading) else stringResource(R.string.login_button), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Kayıt Ol
            Button(
                onClick = onRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = onRegister),
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(R.string.login_register_btn), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Misafir Modu
            Button(
                onClick = onGuestLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = onGuestLogin),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(R.string.login_guest_btn), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LightText)
            }
        }
    }
}
