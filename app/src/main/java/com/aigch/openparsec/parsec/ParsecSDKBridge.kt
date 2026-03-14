package com.aigch.openparsec.parsec

import android.util.Log
import com.aigch.openparsec.network.NetworkHandler
import com.aigch.openparsec.settings.DecoderPref
import com.aigch.openparsec.settings.SettingsHandler
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parsec SDK bridge implementation for Android.
 * Ported from iOS ParsecSDKBridge.swift
 *
 * Note: This is a stub implementation. The actual Parsec SDK integration
 * requires the Parsec Android SDK native library (.so files) loaded via JNI.
 * The architecture and method signatures match the iOS implementation to
 * facilitate future SDK integration.
 */
class ParsecSDKBridge : ParsecService {
    companion object {
        private const val TAG = "ParsecSDKBridge"

        fun clamp(value: Float, minValue: Float, maxValue: Float): Float {
            return value.coerceIn(minValue, maxValue)
        }

        fun clamp(value: Int, minValue: Int, maxValue: Int): Int {
            return value.coerceIn(minValue, maxValue)
        }
    }

    override var hostWidth: Float = 1920f
    override var hostHeight: Float = 1080f
    override var clientWidth: Float = 1920f
    override var clientHeight: Float = 1080f
    override var mouseInfo: MouseInfo = MouseInfo()

    private var isVirtualShiftOn = false
    private var backgroundTaskRunning = true
    private var didSetResolution = false

    init {
        Log.d(TAG, "ParsecSDKBridge initialized")
        // TODO: Load Parsec SDK native library
        // System.loadLibrary("parsec")
        // Initialize native Parsec SDK via JNI
    }

    override fun connect(peerID: String): Int {
        Log.d(TAG, "Connecting to peer: $peerID")

        val resolution = SettingsHandler.resolution
        val sessionId = NetworkHandler.clinfo?.session_id ?: return ParsecStatus.ERR

        // TODO: Call native ParsecClientConnect via JNI
        // Configure video decoder settings from SettingsHandler
        // parsecClientCfg.video.decoderH265 = SettingsHandler.decoder == DecoderPref.H265
        // parsecClientCfg.video.resolutionX = resolution.width
        // parsecClientCfg.video.resolutionY = resolution.height
        // parsecClientCfg.video.decoderCompatibility = SettingsHandler.decoderCompatibility

        backgroundTaskRunning = true
        return ParsecStatus.CONNECTING
    }

    override fun disconnect() {
        Log.d(TAG, "Disconnecting")
        backgroundTaskRunning = false
        // TODO: Call native ParsecClientDisconnect via JNI
    }

    override fun getStatus(): Int {
        // TODO: Call native ParsecClientGetStatus via JNI
        return ParsecStatus.OK
    }

    override fun setFrame(width: Float, height: Float, scale: Float) {
        clientWidth = width
        clientHeight = height
        mouseInfo.mouseX = (width / 2).toInt()
        mouseInfo.mouseY = (height / 2).toInt()
        // TODO: Call native ParsecClientSetDimensions via JNI
    }

    override fun renderGLFrame(timeout: Int) {
        // TODO: Call native ParsecClientGLRenderFrame via JNI
    }

    override fun setMuted(muted: Boolean) {
        // TODO: Call native audio_mute via JNI
    }

    override fun applyConfig() {
        Log.d(TAG, "Applying config")
        // TODO: Call native ParsecClientSetConfig via JNI
    }

    override fun sendMouseMessage(button: Int, x: Int, y: Int, pressed: Boolean) {
        sendMousePosition(x, y)
        // TODO: Send mouse button via JNI ParsecClientSendMessage
    }

    override fun sendMouseClickMessage(button: Int, pressed: Boolean) {
        // TODO: Send mouse click via JNI ParsecClientSendMessage
    }

    override fun sendMouseDelta(dx: Int, dy: Int) {
        if (mouseInfo.mousePositionRelative) {
            // TODO: Send relative mouse move via JNI
        } else {
            sendMousePosition(mouseInfo.mouseX + dx, mouseInfo.mouseY + dy)
        }
    }

    override fun sendMousePosition(x: Int, y: Int) {
        mouseInfo.mouseX = clamp(x, 0, clientWidth.toInt())
        mouseInfo.mouseY = clamp(y, 0, clientHeight.toInt())
        // TODO: Send mouse motion via JNI ParsecClientSendMessage
    }

    override fun sendKeyboardMessage(event: KeyboardKeyEvent) {
        // TODO: Send keyboard message via JNI ParsecClientSendMessage
    }

    override fun sendGameControllerButtonMessage(controllerId: Int, button: Int, pressed: Boolean) {
        // TODO: Send gamepad button via JNI ParsecClientSendMessage
    }

    override fun sendGameControllerAxisMessage(controllerId: Int, axis: Int, value: Short) {
        // TODO: Send gamepad axis via JNI ParsecClientSendMessage
    }

    override fun sendGameControllerUnplugMessage(controllerId: Int) {
        // TODO: Send gamepad unplug via JNI ParsecClientSendMessage
    }

    override fun sendWheelMsg(x: Int, y: Int) {
        // TODO: Send mouse wheel via JNI ParsecClientSendMessage
    }

    override fun sendUserData(type: ParsecUserDataType, message: ByteArray) {
        // TODO: Send user data via JNI ParsecClientSendUserData
    }

    override fun updateHostVideoConfig() {
        val videoConfig = ParsecUserDataVideoConfig()
        videoConfig.video[0].resolutionX = DataManager.resolutionX
        videoConfig.video[0].resolutionY = DataManager.resolutionY
        videoConfig.video[0].encoderMaxBitrate = DataManager.bitrate
        videoConfig.video[0].fullFPS = DataManager.constantFps
        videoConfig.video[0].output = DataManager.output

        try {
            val json = videoConfigToJson(videoConfig)
            CParsec.sendUserData(ParsecUserDataType.SET_VIDEO_CONFIG, json.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding video config: ${e.message}")
        }
    }

    private fun videoConfigToJson(config: ParsecUserDataVideoConfig): String {
        val json = JSONObject()
        json.put("virtualMicrophone", config.virtualMicrophone)
        json.put("virtualTablet", config.virtualTablet)
        val videoArray = JSONArray()
        for (v in config.video) {
            val videoObj = JSONObject()
            videoObj.put("encoderFPS", v.encoderFPS)
            videoObj.put("resolutionX", v.resolutionX)
            videoObj.put("resolutionY", v.resolutionY)
            videoObj.put("fullFPS", v.fullFPS)
            videoObj.put("hostOS", v.hostOS)
            videoObj.put("output", v.output)
            videoObj.put("encoderMaxBitrate", v.encoderMaxBitrate)
            videoArray.put(videoObj)
        }
        json.put("video", videoArray)
        return json.toString()
    }
}
