package com.kaan.watchlist.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kaan.watchlist.navigation.Screen
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.ui.components.tvFocusable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.kaan.watchlist.R

@Composable
fun LoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_watchlist_logo),
            contentDescription = "Watch List Logo",
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Watch List",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = BlueAccent
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Kataloğunu oluştur, keşfet ve izle.",
            fontSize = 16.sp,
            color = LightText.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Kullanıcı Adı") },
            modifier = Modifier.fillMaxWidth().tvFocusable(shape = RoundedCornerShape(12.dp)),
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
            onValueChange = { password = it },
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().tvFocusable(shape = RoundedCornerShape(12.dp)),
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
        
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // TODO: İleride ViewModel üzerinden Auth işlemleri yapılacak
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .tvFocusable(shape = RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Giriş Yap", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                navController.navigate(Screen.Register.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .tvFocusable(shape = RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Kayıt Ol", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                // Misafir olarak giriş (Şimdilik direkt Home ekranına gidiyor)
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            },
            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(8.dp))
        ) {
            Text(text = "Misafir Olarak Devam Et", color = LightText.copy(alpha = 0.7f))
        }
    }
}
