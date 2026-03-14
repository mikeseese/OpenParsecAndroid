package com.aigch.openparsec.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aigch.openparsec.network.ClientInfo
import com.aigch.openparsec.network.NetworkHandler
import com.aigch.openparsec.network.SessionStore

/**
 * Main content view managing navigation between screens.
 * Ported from iOS ContentView.swift
 */
enum class ViewType {
    LOGIN,
    MAIN,
    PARSEC
}

@Composable
fun ContentView() {
    var currentView by remember { mutableStateOf(ViewType.LOGIN) }

    LaunchedEffect(Unit) {
        // Restore session from storage
        val savedSession = SessionStore.load()
        if (savedSession != null) {
            NetworkHandler.clinfo = savedSession
            currentView = ViewType.MAIN
            android.util.Log.d("ContentView", "Session restored, moving to main page")
        }
        android.util.Log.d("ContentView", "Initialized")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Crossfade(targetState = currentView, label = "view_transition") { view ->
            when (view) {
                ViewType.LOGIN -> LoginScreen(
                    onLoginSuccess = { currentView = ViewType.MAIN }
                )
                ViewType.MAIN -> MainScreen(
                    onConnect = { currentView = ViewType.PARSEC },
                    onLogout = { currentView = ViewType.LOGIN }
                )
                ViewType.PARSEC -> ParsecScreen(
                    onDisconnect = { currentView = ViewType.MAIN }
                )
            }
        }
    }
}
