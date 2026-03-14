package com.aigch.openparsec.settings

import android.content.Context
import android.content.SharedPreferences
import com.aigch.openparsec.OpenParsecApp

/**
 * Persistent app settings using SharedPreferences.
 * Ported from iOS SettingsHandler with @AppStorage.
 */
object SettingsHandler {
    private const val PREFS_NAME = "openparsec_settings"

    private val prefs: SharedPreferences by lazy {
        OpenParsecApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var resolution: ParsecResolution
        get() = ParsecResolution.fromString(prefs.getString("resolution", ParsecResolution.CLIENT.key) ?: ParsecResolution.CLIENT.key)
        set(value) = prefs.edit().putString("resolution", value.key).apply()

    var bitrate: Int
        get() = prefs.getInt("bitrate", 0)
        set(value) = prefs.edit().putInt("bitrate", value).apply()

    var decoder: DecoderPref
        get() = DecoderPref.fromInt(prefs.getInt("decoder", DecoderPref.H264.ordinal))
        set(value) = prefs.edit().putInt("decoder", value.ordinal).apply()

    var cursorMode: CursorMode
        get() = CursorMode.fromInt(prefs.getInt("cursorMode", CursorMode.TOUCHPAD.ordinal))
        set(value) = prefs.edit().putInt("cursorMode", value.ordinal).apply()

    var cursorScale: Double
        get() = prefs.getFloat("cursorScale", 0.5f).toDouble()
        set(value) = prefs.edit().putFloat("cursorScale", value.toFloat()).apply()

    var mouseSensitivity: Double
        get() = prefs.getFloat("mouseSensitivity", 1.0f).toDouble()
        set(value) = prefs.edit().putFloat("mouseSensitivity", value.toFloat()).apply()

    var noOverlay: Boolean
        get() = prefs.getBoolean("noOverlay", false)
        set(value) = prefs.edit().putBoolean("noOverlay", value).apply()

    var hideStatusBar: Boolean
        get() = prefs.getBoolean("hideStatusBar", true)
        set(value) = prefs.edit().putBoolean("hideStatusBar", value).apply()

    var rightClickPosition: RightClickPosition
        get() = RightClickPosition.fromInt(prefs.getInt("rightClickPosition", RightClickPosition.FIRST_FINGER.ordinal))
        set(value) = prefs.edit().putInt("rightClickPosition", value.ordinal).apply()

    var preferredFramesPerSecond: Int
        get() = prefs.getInt("preferredFramesPerSecond", 60)
        set(value) = prefs.edit().putInt("preferredFramesPerSecond", value).apply()

    var decoderCompatibility: Boolean
        get() = prefs.getBoolean("decoderCompatibility", false)
        set(value) = prefs.edit().putBoolean("decoderCompatibility", value).apply()

    var showKeyboardButton: Boolean
        get() = prefs.getBoolean("showKeyboardButton", true)
        set(value) = prefs.edit().putBoolean("showKeyboardButton", value).apply()
}

enum class DecoderPref {
    H264,
    H265;

    companion object {
        fun fromInt(value: Int): DecoderPref = entries.getOrElse(value) { H264 }
    }
}

enum class CursorMode {
    TOUCHPAD,
    DIRECT;

    companion object {
        fun fromInt(value: Int): CursorMode = entries.getOrElse(value) { TOUCHPAD }
    }
}

enum class RightClickPosition {
    FIRST_FINGER,
    MIDDLE,
    SECOND_FINGER;

    companion object {
        fun fromInt(value: Int): RightClickPosition = entries.getOrElse(value) { FIRST_FINGER }
    }
}

/**
 * Predefined resolutions matching the iOS ParsecResolution enum.
 */
enum class ParsecResolution(val key: String, val width: Int, val height: Int, val desc: String) {
    HOST("Host Resolution", 0, 0, "Host Resolution"),
    CLIENT("Client Resolution", 3480, 2160, "Client Resolution"),
    R3840x2160("3840x2160 (16:9)", 3840, 2160, "3840x2160 (16:9)"),
    R3840x1600("3840x1600 (21:9)", 3840, 1600, "3840x1600 (21:9)"),
    R3440x1440("3440x1440 (21:9)", 3440, 1440, "3440x1440 (21:9)"),
    R2560x1600("2560x1600 (16:10)", 2560, 1600, "2560x1600 (16:10)"),
    R2560x1440("2560x1440 (16:9)", 2560, 1440, "2560x1440 (16:9)"),
    R2560x1080("2560x1080 (21:9)", 2560, 1080, "2560x1080 (21:9)"),
    R1920x1200("1920x1200 (16:10)", 1920, 1200, "1920x1200 (16:10)"),
    R1920x1080("1920x1080 (16:9)", 1920, 1080, "1920x1080 (16:9)"),
    R1680x1050("1680x1050 (16:10)", 1680, 1050, "1680x1050 (16:10)"),
    R1600x1200("1600x1200 (4:3)", 1600, 1200, "1600x1200 (4:3)"),
    R1366x768("1366x768 (16:9)", 1366, 768, "1366x768 (16:9)"),
    R1280x1024("1280x1024 (5:4)", 1280, 1024, "1280x1024 (5:4)"),
    R1280x800("1280x800 (16:10)", 1280, 800, "1280x800 (16:10)"),
    R1280x720("1280x720 (16:9)", 1280, 720, "1280x720 (16:9)"),
    R1024x768("1024x768 (4:3)", 1024, 768, "1024x768 (4:3)");

    companion object {
        private var clientWidth: Int = 3480
        private var clientHeight: Int = 2160

        val bitrates = listOf(3, 5, 7, 10, 15, 20, 25, 30, 35, 40, 45, 50)

        fun fromString(key: String): ParsecResolution {
            return entries.find { it.key == key } ?: CLIENT
        }

        fun updateClientResolution(width: Int, height: Int) {
            clientWidth = width
            clientHeight = height
        }

        fun getClientWidth(): Int = clientWidth
        fun getClientHeight(): Int = clientHeight
    }
}
