package com.aigch.openparsec.parsec

/**
 * Data structures for Parsec user data messages.
 * Ported from iOS ParsecUserData.swift
 */

data class ParsecUserDataVideo(
    var encoderFPS: Int = 0,
    var resolutionX: Int = 0,
    var resolutionY: Int = 0,
    var fullFPS: Boolean = false,
    var hostOS: Int = 0,
    var output: String = "none",
    var encoderMaxBitrate: Int = 50
)

data class ParsecUserDataVideoConfig(
    var virtualMicrophone: Int = 0,
    var virtualTablet: Int = 0,
    var video: MutableList<ParsecUserDataVideo> = mutableListOf(
        ParsecUserDataVideo(),
        ParsecUserDataVideo(),
        ParsecUserDataVideo()
    )
)

data class ParsecDisplayConfig(
    var name: String = "",
    var adapterName: String = "",
    var id: String = ""
)

enum class ParsecUserDataType(val value: Int) {
    GET_VIDEO_CONFIG(9),
    GET_ADAPTER_INFO(10),
    SET_VIDEO_CONFIG(11)
}
