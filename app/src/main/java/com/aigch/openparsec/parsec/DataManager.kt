package com.aigch.openparsec.parsec

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared runtime state for the streaming session.
 * Ported from iOS SharedModel/DataManager.
 */
object DataManager {
    var resolutionX by mutableIntStateOf(0)
    var resolutionY by mutableIntStateOf(0)
    var bitrate by mutableIntStateOf(0)
    var constantFps by mutableStateOf(false)
    var output by mutableStateOf("none")
    val displayConfigs = mutableStateListOf<ParsecDisplayConfig>()
}
