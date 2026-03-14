package com.aigch.openparsec.network

/**
 * Data models for Parsec API responses.
 * Ported from iOS NetworkHandler.swift
 */

data class ErrorInfo(
    val error: String = ""
)

data class ClientInfo(
    val instance_id: String = "",
    val user_id: Int = 0,
    val session_id: String = "",
    val host_peer_id: String = ""
)

data class UserInfo(
    val id: Int = 0,
    val name: String = "",
    val warp: Boolean = false,
    val team_id: String = ""
)

data class HostInfo(
    val user: UserInfo = UserInfo(),
    val peer_id: String = "",
    val game_id: String = "",
    val description: String = "",
    val max_players: Int = 0,
    val mode: String = "",
    val name: String = "",
    val event_name: String = "",
    val players: Int = 0,
    val guest_access: Boolean = false,
    val online: Boolean = false,
    val build: String = ""
)

data class HostInfoList(
    val data: List<HostInfo>? = null,
    val has_more: Boolean = false
)

data class SelfInfoData(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val warp: Boolean = false,
    val staff: Boolean = false,
    val team_id: String = "",
    val is_confirmed: Boolean = false,
    val team_is_active: Boolean = false,
    val is_saml: Boolean = false,
    val is_gateway_enabled: Boolean = false,
    val is_relay_enabled: Boolean = false,
    val has_tfa: Boolean = false,
    val cohort_channel: String = ""
)

data class SelfInfo(
    val data: SelfInfoData = SelfInfoData()
)

data class FriendInfo(
    val user_id: Int = 0,
    val user_name: String = ""
)

data class FriendInfoList(
    val data: List<FriendInfo>? = null,
    val has_more: Boolean = false
)

/**
 * Static holder for the current client session info.
 * Ported from iOS NetworkHandler class.
 */
object NetworkHandler {
    var clinfo: ClientInfo? = null
}
