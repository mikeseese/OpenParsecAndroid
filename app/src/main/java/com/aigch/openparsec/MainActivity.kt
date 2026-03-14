package com.aigch.openparsec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aigch.openparsec.ui.screens.ContentView
import com.aigch.openparsec.ui.theme.OpenParsecTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenParsecTheme {
                ContentView()
            }
        }
    }
}
