package com.aigch.openparsec.parsec

import android.graphics.Bitmap

/**
 * Mouse/cursor state information.
 * Ported from iOS MouseInfo struct.
 */
data class MouseInfo(
    var pngCursor: Boolean = false,
    var mouseX: Int = 1,
    var mouseY: Int = 1,
    var cursorWidth: Int = 0,
    var cursorHeight: Int = 0,
    var cursorHotX: Int = 0,
    var cursorHotY: Int = 0,
    var cursorImg: Bitmap? = null,
    var cursorHidden: Boolean = false,
    var mousePositionRelative: Boolean = false
)

/**
 * Parsec connection status codes.
 * Maps to the C SDK ParsecStatus enum values.
 */
object ParsecStatus {
    const val OK = 0
    const val CONNECTING = 20
    const val ERR = -1
}

/**
 * Parsec mouse button identifiers.
 */
object ParsecMouseButton {
    const val LEFT = 1
    const val MIDDLE = 2
    const val RIGHT = 3
}

/**
 * Parsec keyboard key event.
 * Ported from iOS KeyBoardKeyEvent.
 */
data class KeyboardKeyEvent(
    val keyCode: Int,
    val isPressBegin: Boolean
)

/**
 * Parsec gamepad button identifiers.
 */
object ParsecGamepadButton {
    const val DPAD_UP = 0
    const val DPAD_DOWN = 1
    const val DPAD_LEFT = 2
    const val DPAD_RIGHT = 3
    const val A = 4
    const val B = 5
    const val X = 6
    const val Y = 7
    const val BACK = 8
    const val START = 10
    const val LSHOULDER = 11
    const val RSHOULDER = 12
    const val LSTICK = 13
    const val RSTICK = 14
    const val GUIDE = 9
}

/**
 * Parsec gamepad axis identifiers.
 */
object ParsecGamepadAxis {
    const val LX = 0
    const val LY = 1
    const val RX = 2
    const val RY = 3
    const val TRIGGER_L = 4
    const val TRIGGER_R = 5
}

/**
 * Interface for the Parsec SDK implementation.
 * Ported from iOS ParsecService protocol.
 */
interface ParsecService {
    val clientWidth: Float
    val clientHeight: Float
    val hostWidth: Float
    val hostHeight: Float
    val mouseInfo: MouseInfo

    fun connect(peerID: String): Int
    fun disconnect()
    fun getStatus(): Int
    fun setFrame(width: Float, height: Float, scale: Float)
    fun renderGLFrame(timeout: Int = 16)
    fun setMuted(muted: Boolean)
    fun applyConfig()
    fun sendMouseMessage(button: Int, x: Int, y: Int, pressed: Boolean)
    fun sendMouseClickMessage(button: Int, pressed: Boolean)
    fun sendMouseDelta(dx: Int, dy: Int)
    fun sendMousePosition(x: Int, y: Int)
    fun sendKeyboardMessage(event: KeyboardKeyEvent)
    fun sendGameControllerButtonMessage(controllerId: Int, button: Int, pressed: Boolean)
    fun sendGameControllerAxisMessage(controllerId: Int, axis: Int, value: Short)
    fun sendGameControllerUnplugMessage(controllerId: Int)
    fun sendWheelMsg(x: Int, y: Int)
    fun sendUserData(type: ParsecUserDataType, message: ByteArray)
    fun updateHostVideoConfig()
}
