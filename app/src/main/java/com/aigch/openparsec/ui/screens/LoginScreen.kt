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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aigch.openparsec.network.ApiClient
import com.aigch.openparsec.network.ClientInfo
import com.aigch.openparsec.network.NetworkHandler
import com.aigch.openparsec.network.SessionStore
import com.aigch.openparsec.ui.theme.Accent
import com.aigch.openparsec.ui.theme.BackgroundField
import com.aigch.openparsec.ui.theme.BackgroundGray
import com.aigch.openparsec.ui.theme.BackgroundPrompt
import com.aigch.openparsec.ui.theme.Foreground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Login screen with email/password authentication and 2FA support.
 * Ported from iOS LoginView.swift
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var tfaCode by remember { mutableStateOf("") }
    var isTFARequired by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun authenticate(tfa: String = "") {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val response = withContext(Dispatchers.IO) {
                    val body = JSONObject().apply {
                        put("email", email)
                        put("password", password)
                        put("tfa", tfa)
                    }
                    ApiClient.post("https://kessel-api.parsec.app/v1/auth", body)
                }

                Log.d("LoginScreen", "Status: ${response.statusCode}")
                Log.d("LoginScreen", "Body: ${response.body}")

                when {
                    response.statusCode == 201 -> {
                        val json = JSONObject(response.body)
                        val clientInfo = ClientInfo(
                            instance_id = json.optString("instance_id", ""),
                            user_id = json.optInt("user_id", 0),
                            session_id = json.optString("session_id", ""),
                            host_peer_id = json.optString("host_peer_id", "")
                        )
                        NetworkHandler.clinfo = clientInfo
                        SessionStore.save(clientInfo)
                        Log.d("LoginScreen", "*** Login succeeded! ***")
                        onLoginSuccess()
                    }
                    response.statusCode >= 400 -> {
                        val json = JSONObject(response.body)
                        val tfaRequired = json.optBoolean("tfa_required", false)
                        if (tfaRequired) {
                            isTFARequired = true
                        } else {
                            errorMessage = json.optString("error", "Login failed")
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Network error: ${e.message}"
                Log.e("LoginScreen", "Error: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Logo area
            Text(
                text = "OpenParsec",
                color = Foreground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
            )

            // Email field
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundField,
                    unfocusedContainerColor = BackgroundField,
                    focusedTextColor = Foreground,
                    unfocusedTextColor = Foreground
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading
            )

            // Password field
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundField,
                    unfocusedContainerColor = BackgroundField,
                    focusedTextColor = Foreground,
                    unfocusedTextColor = Foreground
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading
            )

            // Login button
            Button(
                onClick = { authenticate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading
            ) {
                Text("Login", color = Color.White)
            }

            // Error message
            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Loading / TFA overlay
        if (isLoading || isTFARequired) {
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
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading...", color = Foreground, textAlign = TextAlign.Center)
                    } else if (isTFARequired) {
                        Text(
                            "Please enter your 2FA code from your authenticator app",
                            color = Foreground,
                            textAlign = TextAlign.Center
                        )
                        TextField(
                            value = tfaCode,
                            onValueChange = { tfaCode = it },
                            label = { Text("2FA Code") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = BackgroundField,
                                unfocusedContainerColor = BackgroundField,
                                focusedTextColor = Foreground,
                                unfocusedTextColor = Foreground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isTFARequired = false },
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", color = Foreground)
                            }
                            Button(
                                onClick = { authenticate(tfaCode) },
                                modifier = Modifier.weight(1f).height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Enter", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
