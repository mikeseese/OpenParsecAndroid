package com.aigch.openparsec.input

import com.aigch.openparsec.parsec.CParsec
import com.aigch.openparsec.parsec.CursorPositionHelper
import com.aigch.openparsec.parsec.ParsecMouseButton
import com.aigch.openparsec.settings.CursorMode
import com.aigch.openparsec.settings.SettingsHandler

/**
 * Touch input handler for the streaming view.
 * Ported from iOS TouchHandlingView.swift and ParsecViewController gesture handling.
 *
 * Processes touch events and translates them to Parsec mouse input.
 */
object TouchHandler {
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    /**
     * Handle a single tap at the given screen position.
     * Sends a left mouse click.
     */
    fun onTap(x: Float, y: Float) {
        if (SettingsHandler.cursorMode == CursorMode.DIRECT) {
            val (hostX, hostY) = CursorPositionHelper.toHost(x.toInt(), y.toInt())
            CParsec.sendMouseMessage(ParsecMouseButton.LEFT, hostX, hostY, true)
            CParsec.sendMouseMessage(ParsecMouseButton.LEFT, hostX, hostY, false)
        } else {
            CParsec.sendMouseClickMessage(ParsecMouseButton.LEFT, true)
            CParsec.sendMouseClickMessage(ParsecMouseButton.LEFT, false)
        }
    }

    /**
     * Handle a two-finger tap (right click).
     */
    fun onTwoFingerTap(x: Float, y: Float) {
        CParsec.sendMouseClickMessage(ParsecMouseButton.RIGHT, true)
        CParsec.sendMouseClickMessage(ParsecMouseButton.RIGHT, false)
    }

    /**
     * Handle touch movement start.
     */
    fun onTouchDown(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
        isDragging = false
    }

    /**
     * Handle touch movement (drag).
     */
    fun onTouchMove(x: Float, y: Float) {
        val sensitivity = SettingsHandler.mouseSensitivity.toFloat()

        if (SettingsHandler.cursorMode == CursorMode.DIRECT) {
            val (hostX, hostY) = CursorPositionHelper.toHost(x.toInt(), y.toInt())
            CParsec.sendMousePosition(hostX, hostY)
        } else {
            val dx = ((x - lastTouchX) * sensitivity).toInt()
            val dy = ((y - lastTouchY) * sensitivity).toInt()
            CParsec.sendMouseDelta(dx, dy)
        }

        lastTouchX = x
        lastTouchY = y
        isDragging = true
    }

    /**
     * Handle long press start (drag).
     */
    fun onLongPressStart(x: Float, y: Float) {
        CParsec.sendMouseClickMessage(ParsecMouseButton.LEFT, true)
        lastTouchX = x
        lastTouchY = y
        isDragging = true
    }

    /**
     * Handle long press end.
     */
    fun onLongPressEnd() {
        CParsec.sendMouseClickMessage(ParsecMouseButton.LEFT, false)
        isDragging = false
    }

    /**
     * Handle two-finger scroll gesture.
     */
    fun onScroll(dx: Float, dy: Float) {
        CParsec.sendWheelMsg(dx.toInt(), dy.toInt())
    }

    /**
     * Handle touch up.
     */
    fun onTouchUp() {
        isDragging = false
    }
}
