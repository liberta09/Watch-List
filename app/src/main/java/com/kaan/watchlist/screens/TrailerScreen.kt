package com.kaan.watchlist.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.ui.theme.BlueAccent

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrailerScreen(navController: NavController, trailerKey: String) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }

    // Custom HTML ile embed — loadUrl yerine bu yöntem
    // loadDataWithBaseURL youtube.com domain'ini base alır,
    // böylece YouTube embed video render ediyor (hardware decode düzgün çalışır)
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          * { margin: 0; padding: 0; box-sizing: border-box; }
          html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
          iframe {
            position: absolute; top: 0; left: 0;
            width: 100%; height: 100%;
            border: none;
          }
        </style>
        </head>
        <body>
        <iframe
          src="https://www.youtube.com/embed/$trailerKey?autoplay=1&playsinline=1&rel=0&showinfo=0"
          allow="autoplay; encrypted-media; fullscreen"
          allowfullscreen>
        </iframe>
        </body>
        </html>
    """.trimIndent()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    // Temel ayarlar
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.allowFileAccess = false

                    // Hardware acceleration — video render için kritik
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    // WebChromeClient olmadan video render ETMİYOR
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress == 100) isLoading = false
                        }
                    }

                    // loadUrl değil, loadDataWithBaseURL kullan
                    // base URL = youtube.com → embed player düzgün çalışır
                    loadDataWithBaseURL(
                        "https://www.youtube.com",
                        html,
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading göstergesi
        if (isLoading) {
            CircularProgressIndicator(
                color = BlueAccent,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Geri butonu (sol üst)
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .tvFocusable(shape = CircleShape, onClick = { navController.popBackStack() })
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Geri",
                tint = Color.White
            )
        }

        // "YouTube'da Aç" butonu (sağ üst) — fallback olarak kalsın
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$trailerKey"))
                context.startActivity(intent)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .tvFocusable(
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$trailerKey"))
                        context.startActivity(intent)
                    }
                ),
            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Text("  YouTube'da Aç", color = Color.White, fontSize = 13.sp)
        }
    }
}
