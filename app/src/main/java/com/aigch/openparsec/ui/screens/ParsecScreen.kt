package com.aigch.openparsec.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aigch.openparsec.ui.ParsecGLSurfaceView
import com.aigch.openparsec.parsec.CParsec
import com.aigch.openparsec.parsec.DataManager
import com.aigch.openparsec.parsec.ParsecStatus
import com.aigch.openparsec.parsec.ParsecUserDataType
import com.aigch.openparsec.settings.ParsecResolution
import com.aigch.openparsec.settings.SettingsHandler
import com.aigch.openparsec.ui.theme.Accent
import com.aigch.openparsec.ui.theme.BackgroundPrompt
import com.aigch.openparsec.ui.theme.Foreground
import kotlinx.coroutines.delay

/**
 * Streaming/gaming view with overlay controls.
 * Ported from iOS ParsecView.swift
 *
 * This screen manages the streaming session display with an overlay menu
 * for adjusting resolution, bitrate, display switching, and other options.
 */
@Composable
fun ParsecScreen(
    onDisconnect: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var hideOverlay by remember { mutableStateOf(SettingsHandler.noOverlay) }
    var muted by remember { mutableStateOf(false) }
    var constantFps by remember { mutableStateOf(false) }
    var showDCAlert by remember { mutableStateOf(false) }
    var dcAlertText by remember { mutableStateOf("Disconnected (reason unknown)") }
    var metricInfo by remember { mutableStateOf("Loading...") }

    fun disconnect() {
        CParsec.disconnect()
        onDisconnect()
    }

    fun getHostUserData() {
        val data = "".toByteArray()
        CParsec.sendUserData(ParsecUserDataType.GET_VIDEO_CONFIG, data)
        CParsec.sendUserData(ParsecUserDataType.GET_ADAPTER_INFO, data)
    }

    LaunchedEffect(Unit) {
        CParsec.applyConfig()
        CParsec.setMuted(muted)
        getHostUserData()

        // Poll connection status
        while (true) {
            delay(200)
            if (showDCAlert) continue
            val status = CParsec.getStatus()
            if (status != ParsecStatus.OK) {
                dcAlertText = "Disconnected (code $status)"
                showDCAlert = true
            }
        }
    }

    // Disconnect alert
    if (showDCAlert) {
        AlertDialog(
            onDismissRequest = { disconnect() },
            title = { Text(dcAlertText) },
            confirmButton = {
                TextButton(onClick = { disconnect() }) {
                    Text("Close")
                }
            }
        )
    }

    // GLSurfaceView for Parsec rendering
    var glSurfaceView by remember { mutableStateOf<ParsecGLSurfaceView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            glSurfaceView?.onPause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Streaming rendering surface (ported from iOS ParsecGLKViewController)
        AndroidView(
            factory = { context ->
                ParsecGLSurfaceView(context).also {
                    glSurfaceView = it
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Metrics bar
        if (showMenu) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(BackgroundPrompt.copy(alpha = 0.75f))
                    .padding(4.dp)
            ) {
                Text(
                    text = metricInfo,
                    color = Foreground,
                    fontSize = 10.sp,
                    maxLines = 2
                )
            }
        }

        // Overlay controls
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            if (!hideOverlay) {
                // Menu toggle button
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(48.dp)
                        .background(
                            BackgroundPrompt.copy(alpha = if (showMenu) 0.75f else 1f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            showMenu = !showMenu
                            if (showMenu) getHostUserData()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("OP", color = Accent, fontWeight = FontWeight.Bold)
                }

                // Keyboard button
                if (SettingsHandler.showKeyboardButton) {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(48.dp)
                            .background(
                                BackgroundPrompt.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { /* TODO: Toggle soft keyboard */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = "Keyboard",
                            tint = Foreground,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Menu panel
            if (showMenu) {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .widthIn(max = 175.dp)
                        .background(BackgroundPrompt.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MenuButton("Hide Overlay") {
                        hideOverlay = true
                        showMenu = false
                    }
                    MenuButton("Sound: ${if (muted) "OFF" else "ON"}") {
                        muted = !muted
                        CParsec.setMuted(muted)
                    }

                    // Resolution picker
                    MenuDropdown("Resolution", ParsecResolution.entries.map { it.desc }) { selected ->
                        val res = ParsecResolution.entries.find { it.desc == selected } ?: return@MenuDropdown
                        SettingsHandler.resolution = res
                        DataManager.resolutionX = res.width
                        DataManager.resolutionY = res.height
                        CParsec.updateHostVideoConfig()
                    }

                    // Bitrate picker
                    MenuDropdown("Bitrate", ParsecResolution.bitrates.map { "$it Mbps" }) { selected ->
                        val bitrate = selected.replace(" Mbps", "").toIntOrNull() ?: return@MenuDropdown
                        SettingsHandler.bitrate = bitrate
                        DataManager.bitrate = bitrate
                        CParsec.updateHostVideoConfig()
                    }

                    // Display picker
                    if (DataManager.displayConfigs.size > 1) {
                        val displayOptions = listOf("Auto") + DataManager.displayConfigs.map { "${it.name} ${it.adapterName}" }
                        MenuDropdown("Switch Display", displayOptions) { selected ->
                            val displayId = if (selected == "Auto") "none"
                            else DataManager.displayConfigs.find { "${it.name} ${it.adapterName}" == selected }?.id ?: "none"
                            DataManager.output = displayId
                            CParsec.updateHostVideoConfig()
                        }
                    }

                    MenuButton("Constant FPS: ${if (constantFps) "ON" else "OFF"}") {
                        constantFps = !constantFps
                        DataManager.constantFps = constantFps
                        CParsec.updateHostVideoConfig()
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(Foreground.copy(alpha = 0.25f))
                            .size(1.dp)
                    )

                    MenuButton("Disconnect", textColor = Color.Red) {
                        disconnect()
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MenuButton(text: String, textColor: Color = Foreground, onClick: () -> Unit) {
    Text(
        text = text,
        color = textColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun MenuDropdown(label: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Text(
            text = label,
            color = Foreground,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
