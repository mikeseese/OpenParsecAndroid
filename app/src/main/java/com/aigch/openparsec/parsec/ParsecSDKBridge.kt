package com.aigch.openparsec.parsec

import android.graphics.Bitmap
import android.util.Log
import com.aigch.openparsec.network.NetworkHandler
import com.aigch.openparsec.settings.DecoderPref
import com.aigch.openparsec.settings.SettingsHandler
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Parsec SDK bridge implementation for Android.
 * Ported from iOS ParsecSDKBridge.swift
 *
 * Delegates to native Parsec SDK via JNI. The native bridge library
 * (libparsec_bridge.so) wraps the Parsec C SDK functions.
 *
 * Requirements:
 * - libparsec.so in app/src/main/jniLibs/{abi}/
 * - libparsec_bridge.so built from app/src/main/cpp/
 */
class ParsecSDKBridge : ParsecService {
    companion object {
        private const val TAG = "ParsecSDKBridge"
        private const val MAX_KOTLIN_LOG_LINES = 200

        /** Kotlin-side log entries for connection lifecycle events. */
        private val kotlinLogLines = mutableListOf<String>()

        private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

        @Synchronized
        fun appendKotlinLog(level: String, tag: String, message: String) {
            val ts = timeFormat.format(Date())
            val line = "$ts [$level/$tag] $message"
            kotlinLogLines.add(line)
            if (kotlinLogLines.size > MAX_KOTLIN_LOG_LINES) {
                kotlinLogLines.removeAt(0)
            }
        }

        @Synchronized
        fun getKotlinLogs(): String = kotlinLogLines.joinToString("\n")

        init {
            try {
                System.loadLibrary("parsec_bridge")
                Log.d(TAG, "Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }

        fun clamp(value: Float, minValue: Float, maxValue: Float): Float {
            return value.coerceIn(minValue, maxValue)
        }

        fun clamp(value: Int, minValue: Int, maxValue: Int): Int {
            return value.coerceIn(minValue, maxValue)
        }
    }

    /* JNI handles (store native pointers as Long) */
    private var nativeParsec: Long = 0
    private var nativeAudio: Long = 0

    override var hostWidth: Float = 1920f
    override var hostHeight: Float = 1080f
    override var clientWidth: Float = 1920f
    override var clientHeight: Float = 1080f
    override var mouseInfo: MouseInfo = MouseInfo()

    private var isVirtualShiftOn = false
    @Volatile
    private var backgroundTaskRunning = true
    private var didSetResolution = false
    private var getFirstCursor = false

    /* --- Native JNI methods --- */

    private external fun nativeInit(): Long
    private external fun nativeDestroy(parsec: Long)
    private external fun nativeAudioInit(): Long
    private external fun nativeAudioDestroy(audio: Long)
    private external fun nativeConnect(
        parsec: Long, sessionId: String, peerId: String,
        resX: Int, resY: Int, decoderH265: Boolean, decoderCompat: Boolean
    ): Int
    private external fun nativeDisconnect(parsec: Long, audio: Long)
    private external fun nativeGetStatus(parsec: Long): Int
    private external fun nativeGetStatusEx(parsec: Long): Int
    private external fun nativeSetDimensions(parsec: Long, width: Int, height: Int, scale: Float)
    private external fun nativeRenderGLFrame(parsec: Long, timeout: Int)
    private external fun nativeSetConfig(
        parsec: Long, decoderH265: Boolean, decoderCompat: Boolean,
        mediaContainer: Int, protocol: Int, pngCursor: Boolean
    )
    private external fun nativeSendMouseButton(parsec: Long, button: Int, pressed: Boolean)
    private external fun nativeSendMouseMotion(parsec: Long, x: Int, y: Int, relative: Boolean)
    private external fun nativeSendMouseWheel(parsec: Long, x: Int, y: Int)
    private external fun nativeSendKeyboard(parsec: Long, code: Int, pressed: Boolean)
    private external fun nativeSendGamepadButton(parsec: Long, controllerId: Int, button: Int, pressed: Boolean)
    private external fun nativeSendGamepadAxis(parsec: Long, controllerId: Int, axis: Int, value: Short)
    private external fun nativeSendGamepadUnplug(parsec: Long, controllerId: Int)
    private external fun nativeSendUserData(parsec: Long, type: Int, message: ByteArray)
    private external fun nativePollAudio(parsec: Long, audio: Long, timeout: Int)
    private external fun nativePollEvents(parsec: Long, timeout: Int)
    private external fun nativeAudioMute(audio: Long, muted: Boolean)
    private external fun nativeAudioClear(audio: Long)
    private external fun nativeGetLogs(): String

    init {
        Log.d(TAG, "ParsecSDKBridge initializing")
        nativeAudio = nativeAudioInit()
        nativeParsec = nativeInit()
        if (nativeParsec != 0L) {
            Log.d(TAG, "ParsecSDKBridge initialized successfully")
        } else {
            Log.e(TAG, "ParsecSDKBridge native init failed")
        }
    }

    fun destroy() {
        backgroundTaskRunning = false
        if (nativeParsec != 0L) {
            nativeDestroy(nativeParsec)
            nativeParsec = 0
        }
        if (nativeAudio != 0L) {
            nativeAudioDestroy(nativeAudio)
            nativeAudio = 0
        }
        Log.d(TAG, "ParsecSDKBridge destroyed")
    }

    override fun connect(peerID: String): Int {
        Log.d(TAG, "Connecting to peer: $peerID")
        appendKotlinLog("D", TAG, "Connecting to peer: $peerID")
        if (nativeParsec == 0L) {
            appendKotlinLog("E", TAG, "connect() failed: null native handle")
            return ParsecStatus.ERR
        }

        val resolution = SettingsHandler.resolution
        val sessionId = NetworkHandler.clinfo?.session_id
        if (sessionId == null) {
            appendKotlinLog("E", TAG, "connect() failed: no session_id available")
            return ParsecStatus.ERR
        }

        appendKotlinLog("D", TAG, "Config: res=${resolution.width}x${resolution.height}, " +
                "decoder=${SettingsHandler.decoder}, compat=${SettingsHandler.decoderCompatibility}")

        val status = nativeConnect(
            nativeParsec, sessionId, peerID,
            resolution.width, resolution.height,
            SettingsHandler.decoder == DecoderPref.H265,
            SettingsHandler.decoderCompatibility
        )

        appendKotlinLog("D", TAG, "nativeConnect returned status=$status (${statusToString(status)})")

        if (status == ParsecStatus.CONNECTING || status == ParsecStatus.OK) {
            backgroundTaskRunning = true
            startBackgroundTask()
        } else {
            appendKotlinLog("E", TAG, "Connection failed immediately with status=$status")
        }

        return status
    }

    override fun disconnect() {
        Log.d(TAG, "Disconnecting")
        appendKotlinLog("D", TAG, "disconnect() called")
        backgroundTaskRunning = false
        if (nativeParsec != 0L) {
            nativeDisconnect(nativeParsec, nativeAudio)
        }
    }

    override fun getStatus(): Int {
        if (nativeParsec == 0L) return ParsecStatus.ERR
        return nativeGetStatus(nativeParsec)
    }

    fun getStatusEx(): Int {
        if (nativeParsec == 0L) return ParsecStatus.ERR
        return nativeGetStatusEx(nativeParsec)
    }

    override fun setFrame(width: Float, height: Float, scale: Float) {
        clientWidth = width
        clientHeight = height
        mouseInfo.mouseX = (width / 2).toInt()
        mouseInfo.mouseY = (height / 2).toInt()
        if (nativeParsec != 0L) {
            nativeSetDimensions(nativeParsec, width.toInt(), height.toInt(), scale)
        }
    }

    override fun renderGLFrame(timeout: Int) {
        if (nativeParsec != 0L) {
            nativeRenderGLFrame(nativeParsec, timeout)
        }
    }

    override fun setMuted(muted: Boolean) {
        if (nativeAudio != 0L) {
            nativeAudioMute(nativeAudio, muted)
        }
    }

    override fun applyConfig() {
        Log.d(TAG, "Applying config")
        if (nativeParsec == 0L) return
        nativeSetConfig(
            nativeParsec,
            SettingsHandler.decoder == DecoderPref.H265,
            SettingsHandler.decoderCompatibility,
            0, // mediaContainer
            1, // protocol
            false // pngCursor
        )
    }

    override fun sendMouseMessage(button: Int, x: Int, y: Int, pressed: Boolean) {
        sendMousePosition(x, y)
        if (nativeParsec != 0L) {
            nativeSendMouseButton(nativeParsec, button, pressed)
        }
    }

    override fun sendMouseClickMessage(button: Int, pressed: Boolean) {
        if (nativeParsec != 0L) {
            nativeSendMouseButton(nativeParsec, button, pressed)
        }
    }

    override fun sendMouseDelta(dx: Int, dy: Int) {
        if (mouseInfo.mousePositionRelative) {
            sendMouseRelativeMove(dx, dy)
        } else {
            sendMousePosition(mouseInfo.mouseX + dx, mouseInfo.mouseY + dy)
        }
    }

    override fun sendMousePosition(x: Int, y: Int) {
        mouseInfo.mouseX = clamp(x, 0, clientWidth.toInt())
        mouseInfo.mouseY = clamp(y, 0, clientHeight.toInt())
        if (nativeParsec != 0L) {
            nativeSendMouseMotion(nativeParsec, x, y, false)
        }
    }

    private fun sendMouseRelativeMove(dx: Int, dy: Int) {
        if (nativeParsec != 0L) {
            nativeSendMouseMotion(nativeParsec, dx, dy, true)
        }
    }

    override fun sendKeyboardMessage(event: KeyboardKeyEvent) {
        if (nativeParsec != 0L) {
            nativeSendKeyboard(nativeParsec, event.keyCode, event.isPressBegin)
        }
    }

    override fun sendGameControllerButtonMessage(controllerId: Int, button: Int, pressed: Boolean) {
        if (nativeParsec != 0L) {
            nativeSendGamepadButton(nativeParsec, controllerId, button, pressed)
        }
    }

    override fun sendGameControllerAxisMessage(controllerId: Int, axis: Int, value: Short) {
        if (nativeParsec != 0L) {
            nativeSendGamepadAxis(nativeParsec, controllerId, axis, value)
        }
    }

    override fun sendGameControllerUnplugMessage(controllerId: Int) {
        if (nativeParsec != 0L) {
            nativeSendGamepadUnplug(nativeParsec, controllerId)
        }
    }

    override fun sendWheelMsg(x: Int, y: Int) {
        if (nativeParsec != 0L) {
            nativeSendMouseWheel(nativeParsec, x, y)
        }
    }

    override fun sendUserData(type: ParsecUserDataType, message: ByteArray) {
        if (nativeParsec != 0L) {
            nativeSendUserData(nativeParsec, type.value, message)
        }
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

    /* --- Connection Logs --- */

    /**
     * Returns combined native + Kotlin logs for debugging connection issues.
     */
    fun getConnectionLogs(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Native Parsec SDK Logs ===")
        try {
            sb.appendLine(nativeGetLogs())
        } catch (e: Exception) {
            sb.appendLine("(Failed to retrieve native logs: ${e.message})")
        }
        sb.appendLine()
        sb.appendLine("=== Kotlin Bridge Logs ===")
        sb.appendLine(getKotlinLogs())
        return sb.toString()
    }

    private fun statusToString(status: Int): String {
        return when (status) {
            ParsecStatus.OK -> "OK"
            ParsecStatus.CONNECTING -> "CONNECTING"
            ParsecStatus.ERR -> "ERR_DEFAULT"
            -3 -> "NOT_RUNNING"
            -4 -> "ALREADY_RUNNING"
            -36000 -> "ERR_VERSION"
            else -> "UNKNOWN($status)"
        }
    }

    /* --- Background Tasks (matching iOS startBackgroundTask) --- */

    private fun startBackgroundTask() {
        // Audio polling thread
        Thread({
            Log.d(TAG, "Audio polling thread started")
            while (backgroundTaskRunning) {
                val parsec = nativeParsec
                val audio = nativeAudio
                if (parsec != 0L && audio != 0L) {
                    nativePollAudio(parsec, audio, 16)
                } else {
                    break
                }
            }
            Log.d(TAG, "Audio polling thread stopped")
        }, "ParsecAudioPoll").start()

        // Event polling thread
        Thread({
            Log.d(TAG, "Event polling thread started")
            while (backgroundTaskRunning) {
                val parsec = nativeParsec
                if (parsec != 0L) {
                    nativePollEvents(parsec, 16)
                } else {
                    break
                }
            }
            Log.d(TAG, "Event polling thread stopped")
        }, "ParsecEventPoll").start()
    }

    /* --- Event Handlers (called from native JNI via callback) --- */

    /**
     * Called from native nativePollEvents when a cursor event is received.
     * Ported from iOS handleCursorEvent().
     */
    @Suppress("unused") // Called from JNI
    private fun handleCursorEvent(
        hidden: Boolean, relative: Boolean, imageUpdate: Boolean,
        width: Int, height: Int, hotX: Int, hotY: Int,
        positionX: Int, positionY: Int, imageData: ByteArray?
    ) {
        val prevHidden = mouseInfo.cursorHidden
        mouseInfo.cursorHidden = hidden
        mouseInfo.mousePositionRelative = relative

        if (imageUpdate || !getFirstCursor) {
            getFirstCursor = true
            mouseInfo.cursorWidth = width
            mouseInfo.cursorHeight = height
            mouseInfo.cursorHotX = hotX
            mouseInfo.cursorHotY = hotY

            if (prevHidden && !hidden) {
                mouseInfo.mouseX = positionX
                mouseInfo.mouseY = positionY
            }

            // Decode cursor image from RGBA pixel data
            if (imageData != null && width > 0 && height > 0) {
                try {
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageData))
                    mouseInfo.cursorImg = bitmap
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding cursor image: ${e.message}")
                }
            }
        }
    }

    /**
     * Called from native nativePollEvents when a user data event is received.
     * Ported from iOS handleUserDataEvent().
     */
    @Suppress("unused") // Called from JNI
    private fun handleUserDataEvent(id: Int, data: ByteArray) {
        try {
            val jsonStr = String(data, Charsets.UTF_8)
            when (id) {
                ParsecUserDataType.SET_VIDEO_CONFIG.value -> {
                    val json = JSONObject(jsonStr)
                    val videoArray = json.optJSONArray("video")
                    if (videoArray != null && videoArray.length() > 0) {
                        val videoObj = videoArray.getJSONObject(0)
                        DataManager.resolutionX = videoObj.optInt("resolutionX", 0)
                        DataManager.resolutionY = videoObj.optInt("resolutionY", 0)
                        DataManager.bitrate = videoObj.optInt("encoderMaxBitrate", 0)
                        DataManager.constantFps = videoObj.optBoolean("fullFPS", false)

                        if (!didSetResolution) {
                            didSetResolution = true
                            DataManager.resolutionX = SettingsHandler.resolution.width
                            DataManager.resolutionY = SettingsHandler.resolution.height
                            updateHostVideoConfig()
                        }
                    }
                }
                GET_ADAPTER_INFO_ID -> {
                    val configArray = JSONArray(jsonStr)
                    val configs = mutableListOf<ParsecDisplayConfig>()
                    for (i in 0 until configArray.length()) {
                        val obj = configArray.getJSONObject(i)
                        configs.add(ParsecDisplayConfig(
                            name = obj.optString("name", ""),
                            adapterName = obj.optString("adapterName", ""),
                            id = obj.optString("id", "")
                        ))
                    }
                    DataManager.displayConfigs.clear()
                    DataManager.displayConfigs.addAll(configs)
                }
                else -> {
                    Log.d(TAG, "Unhandled user data type: $id")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing user data (id=$id): ${e.message}")
        }
    }

    /* --- JSON Helpers --- */

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

private const val GET_ADAPTER_INFO_ID = 12
