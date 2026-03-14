package com.aigch.openparsec.input

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.aigch.openparsec.parsec.CParsec
import com.aigch.openparsec.parsec.ParsecGamepadAxis
import com.aigch.openparsec.parsec.ParsecGamepadButton
import com.aigch.openparsec.parsec.ParsecMouseButton
import com.aigch.openparsec.settings.SettingsHandler

/**
 * Game controller handler for Android.
 * Ported from iOS GameController.swift / GamepadController.
 *
 * Handles gamepad button/axis events and USB/Bluetooth mouse input.
 */
class GameControllerHandler(private val context: Context) : InputManager.InputDeviceListener {

    fun register() {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(this, null)
    }

    fun unregister() {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.unregisterInputDeviceListener(this)
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        // Controller connected
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        CParsec.sendGameControllerUnplugMessage(1)
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        // Controller configuration changed
    }

    /**
     * Handle gamepad key events.
     * Returns true if the event was consumed.
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        val device = event.device ?: return false
        if (!isGamepad(device)) return false

        val pressed = event.action == KeyEvent.ACTION_DOWN
        val parsecButton = mapGamepadButton(event.keyCode) ?: return false
        CParsec.sendGameControllerButtonMessage(1, parsecButton, pressed)
        return true
    }

    /**
     * Handle gamepad motion events (sticks, triggers).
     * Returns true if the event was consumed.
     */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        val device = event.device ?: return false
        if (!isGamepad(device)) return false

        // Left thumbstick
        val lx = event.getAxisValue(MotionEvent.AXIS_X)
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)
        CParsec.sendGameControllerAxisMessage(1, ParsecGamepadAxis.LX, buttonFloatToParsecInt(lx))
        CParsec.sendGameControllerAxisMessage(1, ParsecGamepadAxis.LY, buttonFloatToParsecInt(ly))

        // Right thumbstick
        val rx = event.getAxisValue(MotionEvent.AXIS_Z)
        val ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        CParsec.sendGameControllerAxisMessage(1, ParsecGamepadAxis.RX, buttonFloatToParsecInt(rx))
        CParsec.sendGameControllerAxisMessage(1, ParsecGamepadAxis.RY, buttonFloatToParsecInt(ry))

        // Triggers
        val lt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
        val rt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
        CParsec.sendGameControllerAxisMessage(1, ParsecGamepadAxis.TRIGGER_L, buttonFloatToParsecInt(lt))
        CParsec.sendGameControllerAxisMessage(1, ParsecGamepadAxis.TRIGGER_R, buttonFloatToParsecInt(rt))

        // D-pad via hat axes
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        CParsec.sendGameControllerButtonMessage(1, ParsecGamepadButton.DPAD_LEFT, hatX < -0.5f)
        CParsec.sendGameControllerButtonMessage(1, ParsecGamepadButton.DPAD_RIGHT, hatX > 0.5f)
        CParsec.sendGameControllerButtonMessage(1, ParsecGamepadButton.DPAD_UP, hatY < -0.5f)
        CParsec.sendGameControllerButtonMessage(1, ParsecGamepadButton.DPAD_DOWN, hatY > 0.5f)

        return true
    }

    /**
     * Handle mouse motion events.
     * Returns true if the event was consumed.
     */
    fun handleMouseMotionEvent(event: MotionEvent): Boolean {
        val device = event.device ?: return false
        if (!isMouse(device)) return false

        if (event.action == MotionEvent.ACTION_HOVER_MOVE || event.action == MotionEvent.ACTION_MOVE) {
            val sensitivity = SettingsHandler.mouseSensitivity.toFloat()
            val dx = (event.getAxisValue(MotionEvent.AXIS_RELATIVE_X) * sensitivity).toInt()
            val dy = (event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y) * sensitivity).toInt()
            if (dx != 0 || dy != 0) {
                CParsec.sendMouseDelta(dx, dy)
            }
        }
        return true
    }

    /**
     * Handle mouse button events.
     * Returns true if the event was consumed.
     */
    fun handleMouseButtonEvent(event: KeyEvent): Boolean {
        val device = event.device ?: return false
        if (!isMouse(device)) return false

        val pressed = event.action == KeyEvent.ACTION_DOWN
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                CParsec.sendMouseClickMessage(ParsecMouseButton.RIGHT, pressed)
                return true
            }
        }
        return false
    }

    private fun isGamepad(device: InputDevice): Boolean {
        val sources = device.sources
        return sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
    }

    private fun isMouse(device: InputDevice): Boolean {
        return device.sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE
    }

    private fun mapGamepadButton(keyCode: Int): Int? {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> ParsecGamepadButton.A
            KeyEvent.KEYCODE_BUTTON_B -> ParsecGamepadButton.B
            KeyEvent.KEYCODE_BUTTON_X -> ParsecGamepadButton.X
            KeyEvent.KEYCODE_BUTTON_Y -> ParsecGamepadButton.Y
            KeyEvent.KEYCODE_BUTTON_L1 -> ParsecGamepadButton.LSHOULDER
            KeyEvent.KEYCODE_BUTTON_R1 -> ParsecGamepadButton.RSHOULDER
            KeyEvent.KEYCODE_BUTTON_THUMBL -> ParsecGamepadButton.LSTICK
            KeyEvent.KEYCODE_BUTTON_THUMBR -> ParsecGamepadButton.RSTICK
            KeyEvent.KEYCODE_BUTTON_START -> ParsecGamepadButton.START
            KeyEvent.KEYCODE_BUTTON_SELECT -> ParsecGamepadButton.BACK
            KeyEvent.KEYCODE_BUTTON_MODE -> ParsecGamepadButton.GUIDE
            KeyEvent.KEYCODE_DPAD_UP -> ParsecGamepadButton.DPAD_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> ParsecGamepadButton.DPAD_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> ParsecGamepadButton.DPAD_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> ParsecGamepadButton.DPAD_RIGHT
            else -> null
        }
    }

    /**
     * Convert float axis value (-1.0 to 1.0) to Parsec Int16 range.
     * Ported from iOS ButtonFloatToParsecInt.
     */
    private fun buttonFloatToParsecInt(value: Float): Short {
        val newVal = (65535f * value - 1f) / 2f
        return newVal.toInt().toShort()
    }
}
