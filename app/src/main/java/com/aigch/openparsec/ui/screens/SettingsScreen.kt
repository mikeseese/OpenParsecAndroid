package com.aigch.openparsec.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigch.openparsec.settings.CursorMode
import com.aigch.openparsec.settings.DecoderPref
import com.aigch.openparsec.settings.ParsecResolution
import com.aigch.openparsec.settings.RightClickPosition
import com.aigch.openparsec.settings.SettingsHandler
import com.aigch.openparsec.ui.theme.Accent
import com.aigch.openparsec.ui.theme.BackgroundGray
import com.aigch.openparsec.ui.theme.BackgroundTab
import com.aigch.openparsec.ui.theme.Foreground
import com.aigch.openparsec.ui.theme.ForegroundInactive

/**
 * Settings screen for app preferences.
 * Ported from iOS SettingsView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onDismiss: () -> Unit) {
    var cursorMode by remember { mutableStateOf(SettingsHandler.cursorMode) }
    var rightClickPosition by remember { mutableStateOf(SettingsHandler.rightClickPosition) }
    var cursorScale by remember { mutableFloatStateOf(SettingsHandler.cursorScale.toFloat()) }
    var mouseSensitivity by remember { mutableFloatStateOf(SettingsHandler.mouseSensitivity.toFloat()) }
    var resolution by remember { mutableStateOf(SettingsHandler.resolution) }
    var decoder by remember { mutableStateOf(SettingsHandler.decoder) }
    var preferredFps by remember { mutableIntStateOf(SettingsHandler.preferredFramesPerSecond) }
    var decoderCompatibility by remember { mutableStateOf(SettingsHandler.decoderCompatibility) }
    var noOverlay by remember { mutableStateOf(SettingsHandler.noOverlay) }
    var hideStatusBar by remember { mutableStateOf(SettingsHandler.hideStatusBar) }
    var showKeyboardButton by remember { mutableStateOf(SettingsHandler.showKeyboardButton) }

    fun save() {
        SettingsHandler.cursorMode = cursorMode
        SettingsHandler.rightClickPosition = rightClickPosition
        SettingsHandler.cursorScale = cursorScale.toDouble()
        SettingsHandler.mouseSensitivity = mouseSensitivity.toDouble()
        SettingsHandler.resolution = resolution
        SettingsHandler.decoder = decoder
        SettingsHandler.preferredFramesPerSecond = preferredFps
        SettingsHandler.decoderCompatibility = decoderCompatibility
        SettingsHandler.noOverlay = noOverlay
        SettingsHandler.hideStatusBar = hideStatusBar
        SettingsHandler.showKeyboardButton = showKeyboardButton
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Foreground)
                },
                navigationIcon = {
                    IconButton(onClick = { save() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundTab)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Interactivity section
                SectionTitle("Interactivity")
                SettingsCard {
                    SettingsRow("Mouse Movement") {
                        DropdownPicker(
                            selected = cursorMode,
                            options = listOf(CursorMode.TOUCHPAD to "Touchpad", CursorMode.DIRECT to "Direct"),
                            onSelect = { cursorMode = it }
                        )
                    }
                    SettingsRow("Right Click Position") {
                        DropdownPicker(
                            selected = rightClickPosition,
                            options = listOf(
                                RightClickPosition.FIRST_FINGER to "First Finger",
                                RightClickPosition.MIDDLE to "Middle",
                                RightClickPosition.SECOND_FINGER to "Second Finger"
                            ),
                            onSelect = { rightClickPosition = it }
                        )
                    }
                    SettingsRow("Cursor Scale") {
                        Slider(
                            value = cursorScale,
                            onValueChange = { cursorScale = it },
                            valueRange = 0.1f..4f,
                            steps = 38,
                            modifier = Modifier.width(150.dp)
                        )
                        Text("%.1f".format(cursorScale), color = Foreground)
                    }
                    SettingsRow("Mouse Sensitivity") {
                        Slider(
                            value = mouseSensitivity,
                            onValueChange = { mouseSensitivity = it },
                            valueRange = 0.1f..4f,
                            steps = 38,
                            modifier = Modifier.width(150.dp)
                        )
                        Text("%.1f".format(mouseSensitivity), color = Foreground)
                    }
                }

                // Graphics section
                SectionTitle("Graphics")
                SettingsCard {
                    SettingsRow("Default Resolution") {
                        DropdownPicker(
                            selected = resolution,
                            options = ParsecResolution.entries.map { it to it.desc },
                            onSelect = { resolution = it }
                        )
                    }
                    SettingsRow("Decoder") {
                        DropdownPicker(
                            selected = decoder,
                            options = listOf(DecoderPref.H264 to "H.264", DecoderPref.H265 to "Prefer H.265"),
                            onSelect = { decoder = it }
                        )
                    }
                    SettingsRow("Frame Rate") {
                        DropdownPicker(
                            selected = preferredFps,
                            options = listOf(0 to "Auto (Device Max)", 120 to "120 FPS", 60 to "60 FPS", 30 to "30 FPS"),
                            onSelect = { preferredFps = it }
                        )
                    }
                    SettingsRow("Decoder Compatibility") {
                        Switch(
                            checked = decoderCompatibility,
                            onCheckedChange = { decoderCompatibility = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Accent)
                        )
                    }
                }

                // Misc section
                SectionTitle("Misc")
                SettingsCard {
                    SettingsRow("Never Show Overlay") {
                        Switch(
                            checked = noOverlay,
                            onCheckedChange = { noOverlay = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Accent)
                        )
                    }
                    SettingsRow("Hide Status Bar") {
                        Switch(
                            checked = hideStatusBar,
                            onCheckedChange = { hideStatusBar = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Accent)
                        )
                    }
                    SettingsRow("Show Keyboard Button") {
                        Switch(
                            checked = showKeyboardButton,
                            onCheckedChange = { showKeyboardButton = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Accent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "More options coming soon.",
                    color = ForegroundInactive,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Foreground,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundTab, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(title: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Foreground, modifier = Modifier.weight(1f), maxLines = 1)
        content()
    }
}

@Composable
private fun <T> DropdownPicker(
    selected: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selected }?.second ?: "Choose..."

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedLabel, color = Accent)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (value == selected) "✓ $label" else label,
                            color = if (value == selected) Accent else Foreground
                        )
                    },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
