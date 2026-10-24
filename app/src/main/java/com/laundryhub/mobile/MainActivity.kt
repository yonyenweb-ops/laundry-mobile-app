package com.laundryhub.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.laundryhub.mobile.data.Session
import com.laundryhub.mobile.ui.theme.LaundryHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Session.init(applicationContext)
        setContent {
            LaundryHubTheme {
                LaundryHubApp()
            }
        }
    }
}
