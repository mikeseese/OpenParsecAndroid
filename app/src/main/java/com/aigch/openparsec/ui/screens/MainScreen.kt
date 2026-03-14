package com.aigch.openparsec.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aigch.openparsec.network.ApiClient
import com.aigch.openparsec.network.NetworkHandler
import com.aigch.openparsec.network.SessionStore
import com.aigch.openparsec.parsec.CParsec
import com.aigch.openparsec.parsec.ParsecStatus
import com.aigch.openparsec.ui.theme.Accent
import com.aigch.openparsec.ui.theme.BackgroundCard
import com.aigch.openparsec.ui.theme.BackgroundGray
import com.aigch.openparsec.ui.theme.BackgroundPrompt
import com.aigch.openparsec.ui.theme.BackgroundTab
import com.aigch.openparsec.ui.theme.Foreground
import com.aigch.openparsec.ui.theme.ForegroundInactive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main screen showing hosts and friends lists.
 * Ported from iOS MainView.swift
 */

data class IdentifiableHostInfo(
    val id: String,
    val hostname: String,
    val userId: Int,
    val userName: String,
    val connections: Int
)

data class IdentifiableUserInfo(
    val id: Int,
    val username: String
)

private enum class Page { HOSTS, FRIENDS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onConnect: () -> Unit,
    onLogout: () -> Unit
) {
    var page by remember { mutableStateOf(Page.HOSTS) }
    val hosts = remember { mutableStateListOf<IdentifiableHostInfo>() }
    val friends = remember { mutableStateListOf<IdentifiableUserInfo>() }
    var userInfo by remember { mutableStateOf<IdentifiableUserInfo?>(null) }

    var hostCountStr by remember { mutableStateOf("0 hosts") }
    var friendCountStr by remember { mutableStateOf("0 friends") }
    var refreshTime by remember { mutableStateOf("") }

    var isRefreshing by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectingToName by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun refreshHosts() {
        val clinfo = NetworkHandler.clinfo ?: return
        scope.launch {
            isRefreshing = true
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.get(
                        "https://kessel-api.parsec.app/v2/hosts?mode=desktop&public=false",
                        clinfo.session_id
                    )
                }
                if (response.statusCode == 200) {
                    val json = JSONObject(response.body)
                    val dataArray = json.optJSONArray("data")
                    hosts.clear()
                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val h = dataArray.getJSONObject(i)
                            val user = h.getJSONObject("user")
                            hosts.add(
                                IdentifiableHostInfo(
                                    id = h.getString("peer_id"),
                                    hostname = h.getString("name"),
                                    userId = user.getInt("id"),
                                    userName = user.getString("name"),
                                    connections = h.optInt("players", 0)
                                )
                            )
                        }
                    }
                    val grammar = if (hosts.size == 1) "host" else "hosts"
                    hostCountStr = "${hosts.size} $grammar"
                    val fmt = SimpleDateFormat("M/d/yyyy h:mm a", Locale.getDefault())
                    refreshTime = "Last refreshed at ${fmt.format(Date())}"
                } else {
                    val json = JSONObject(response.body)
                    errorMessage = "Error gathering hosts: ${json.optString("error", "Unknown error")}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isRefreshing = false
            }
        }
    }

    fun refreshSelf() {
        val clinfo = NetworkHandler.clinfo ?: return
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.get("https://kessel-api.parsec.app/me", clinfo.session_id)
                }
                if (response.statusCode == 200) {
                    val json = JSONObject(response.body)
                    val data = json.getJSONObject("data")
                    userInfo = IdentifiableUserInfo(
                        id = data.getInt("id"),
                        username = data.getString("name")
                    )
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "Error refreshing self: ${e.message}")
            }
        }
    }

    fun refreshFriends() {
        val clinfo = NetworkHandler.clinfo ?: return
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.get("https://kessel-api.parsec.app/friendships", clinfo.session_id)
                }
                if (response.statusCode == 200) {
                    val json = JSONObject(response.body)
                    val dataArray = json.optJSONArray("data")
                    friends.clear()
                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val f = dataArray.getJSONObject(i)
                            friends.add(
                                IdentifiableUserInfo(
                                    id = f.getInt("user_id"),
                                    username = f.getString("user_name")
                                )
                            )
                        }
                    }
                    val grammar = if (friends.size == 1) "friend" else "friends"
                    friendCountStr = "${friends.size} $grammar"
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "Error refreshing friends: ${e.message}")
            }
        }
    }

    fun connectTo(host: IdentifiableHostInfo) {
        CParsec.initialize()
        connectingToName = host.hostname
        isConnecting = true

        scope.launch {
            CParsec.connect(host.id)
            // Poll connection status
            while (isConnecting) {
                delay(1000)
                val status = CParsec.getStatus()
                if (status == ParsecStatus.CONNECTING) continue
                isConnecting = false
                if (status == ParsecStatus.OK) {
                    onConnect()
                } else {
                    errorMessage = "Error connecting to host (code $status)"
                }
                break
            }
        }
    }

    fun cancelConnection() {
        isConnecting = false
        CParsec.disconnect()
    }

    fun logout() {
        SessionStore.clear()
        NetworkHandler.clinfo = null
        onLogout()
    }

    LaunchedEffect(Unit) {
        refreshHosts()
        refreshSelf()
        refreshFriends()
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; logout() }) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error dialog
    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Settings dialog
    if (showSettingsDialog) {
        SettingsScreen(
            onDismiss = { showSettingsDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (page == Page.HOSTS) hostCountStr else friendCountStr,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Foreground
                        )
                        if (page == Page.HOSTS) {
                            IconButton(onClick = { refreshHosts() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Accent)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Accent)
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundTab)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BackgroundTab) {
                NavigationBarItem(
                    selected = page == Page.HOSTS,
                    onClick = { page = Page.HOSTS },
                    icon = { Icon(Icons.Default.Computer, contentDescription = "Hosts") },
                    label = { Text("Hosts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Accent,
                        selectedTextColor = Accent,
                        unselectedIconColor = ForegroundInactive,
                        unselectedTextColor = ForegroundInactive,
                        indicatorColor = BackgroundTab
                    )
                )
                NavigationBarItem(
                    selected = page == Page.FRIENDS,
                    onClick = { page = Page.FRIENDS },
                    icon = { Icon(Icons.Default.People, contentDescription = "Friends") },
                    label = { Text("Friends") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Accent,
                        selectedTextColor = Accent,
                        unselectedIconColor = ForegroundInactive,
                        unselectedTextColor = ForegroundInactive,
                        indicatorColor = BackgroundTab
                    )
                )
            }
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (page == Page.HOSTS) {
                // Hosts list
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (refreshTime.isNotEmpty()) {
                        Text(
                            text = refreshTime,
                            color = ForegroundInactive,
                            textAlign = TextAlign.Center
                        )
                    }
                    hosts.forEach { host ->
                        HostCard(host = host, onConnect = { connectTo(host) })
                    }
                }
            } else {
                // Friends list
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    userInfo?.let { user ->
                        Text("You", color = ForegroundInactive, textAlign = TextAlign.Center)
                        UserCard(user = user)
                    }
                    if (friends.isNotEmpty()) {
                        Text("Friends", color = ForegroundInactive, textAlign = TextAlign.Center)
                        friends.forEach { friend ->
                            UserCard(user = friend)
                        }
                    }
                }
            }

            // Connecting overlay
            if (isConnecting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 350.dp)
                            .background(BackgroundPrompt, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Requesting connection to $connectingToName...",
                            color = Foreground,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { cancelConnection() },
                            colors = ButtonDefaults.buttonColors(containerColor = BackgroundCard)
                        ) {
                            Text("Cancel", color = Color.Red)
                        }
                    }
                }
            }

            // Refreshing overlay
            if (isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .background(BackgroundPrompt, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Refreshing hosts...", color = Foreground, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun HostCard(host: IdentifiableHostInfo, onConnect: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 400.dp)
            .fillMaxWidth()
            .background(BackgroundCard, RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AsyncImage(
            model = "https://parsecusercontent.com/cors-resize-image/w=64,h=64,fit=crop,background=white,q=90,f=jpeg/avatars/${host.userId}/avatar",
            contentDescription = "Avatar",
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = host.hostname,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Foreground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${host.userName}#${host.userId}",
            fontSize = 16.sp,
            color = ForegroundInactive,
            textAlign = TextAlign.Center
        )
        if (host.connections > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Foreground, modifier = Modifier.size(16.dp))
                Text(" ${host.connections}", color = Foreground, fontSize = 14.sp)
            }
        }
        Button(
            onClick = onConnect,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Connect", color = Color.White)
        }
    }
}

@Composable
private fun UserCard(user: IdentifiableUserInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundCard, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = "https://parsecusercontent.com/cors-resize-image/w=48,h=48,fit=crop,background=white,q=90,f=jpeg/avatars/${user.id}/avatar",
            contentDescription = "Avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = "${user.username}#${user.id}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Foreground
        )
    }
}
