package com.hhp227.concafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hhp227.concafe.data.source.AndroidScreenCaptureProtectionActivityHolder
import com.hhp227.concafe.presentation.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidScreenCaptureProtectionActivityHolder.update(this)
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidScreenCaptureProtectionActivityHolder.update(this)
    }

    override fun onDestroy() {
        AndroidScreenCaptureProtectionActivityHolder.clear(this)
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
