package com.aigch.openparsec.parsec

/**
 * Static facade for the Parsec SDK.
 * Ported from iOS CParsec class.
 *
 * Provides a singleton-style access to the Parsec SDK implementation.
 * All methods delegate to the underlying ParsecService implementation.
 */
object CParsec {
    private var parsecImpl: ParsecService? = null

    val hostWidth: Float get() = parsecImpl?.hostWidth ?: 1920f
    val hostHeight: Float get() = parsecImpl?.hostHeight ?: 1080f
    val clientWidth: Float get() = parsecImpl?.clientWidth ?: 1920f
    val clientHeight: Float get() = parsecImpl?.clientHeight ?: 1080f
    val mouseInfo: MouseInfo get() = parsecImpl?.mouseInfo ?: MouseInfo()

    fun initialize() {
        parsecImpl = ParsecSDKBridge()
    }

    fun destroy() {
        parsecImpl = null
    }

    fun connect(peerID: String): Int {
        return parsecImpl?.connect(peerID) ?: ParsecStatus.ERR
    }

    fun disconnect() {
        parsecImpl?.disconnect()
    }

    fun getStatus(): Int {
        return parsecImpl?.getStatus() ?: ParsecStatus.ERR
    }

    fun setFrame(width: Float, height: Float, scale: Float) {
        parsecImpl?.setFrame(width, height, scale)
    }

    fun renderGLFrame(timeout: Int = 16) {
        parsecImpl?.renderGLFrame(timeout)
    }

    fun setMuted(muted: Boolean) {
        parsecImpl?.setMuted(muted)
    }

    fun applyConfig() {
        parsecImpl?.applyConfig()
    }

    fun updateHostVideoConfig() {
        parsecImpl?.updateHostVideoConfig()
    }

    fun sendMouseMessage(button: Int, x: Int, y: Int, pressed: Boolean) {
        parsecImpl?.sendMouseMessage(button, x, y, pressed)
    }

    fun sendMouseClickMessage(button: Int, pressed: Boolean) {
        parsecImpl?.sendMouseClickMessage(button, pressed)
    }

    fun sendMouseDelta(dx: Int, dy: Int) {
        parsecImpl?.sendMouseDelta(dx, dy)
    }

    fun sendMousePosition(x: Int, y: Int) {
        parsecImpl?.sendMousePosition(x, y)
    }

    fun sendKeyboardMessage(event: KeyboardKeyEvent) {
        parsecImpl?.sendKeyboardMessage(event)
    }

    fun sendGameControllerButtonMessage(controllerId: Int, button: Int, pressed: Boolean) {
        parsecImpl?.sendGameControllerButtonMessage(controllerId, button, pressed)
    }

    fun sendGameControllerAxisMessage(controllerId: Int, axis: Int, value: Short) {
        parsecImpl?.sendGameControllerAxisMessage(controllerId, axis, value)
    }

    fun sendGameControllerUnplugMessage(controllerId: Int) {
        parsecImpl?.sendGameControllerUnplugMessage(controllerId)
    }

    fun sendWheelMsg(x: Int, y: Int) {
        parsecImpl?.sendWheelMsg(x, y)
    }

    fun sendUserData(type: ParsecUserDataType, message: ByteArray) {
        parsecImpl?.sendUserData(type, message)
    }

    fun getImpl(): ParsecService? = parsecImpl
}
