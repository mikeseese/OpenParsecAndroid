package com.aigch.openparsec.network

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simple HTTP client for Parsec API calls.
 * Ported from iOS URLSession usage.
 */
object ApiClient {
    private const val TAG = "ApiClient"
    private const val USER_AGENT = "parsec/150-93b Windows/11 libmatoya/4.0"

    data class ApiResponse(
        val statusCode: Int,
        val body: String
    )

    fun post(urlString: String, jsonBody: JSONObject, authToken: String? = null): ApiResponse {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            authToken?.let {
                conn.setRequestProperty("Authorization", "Bearer $it")
            }
            conn.doOutput = true
            conn.doInput = true

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val statusCode = conn.responseCode
            val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            Log.d(TAG, "POST $urlString -> $statusCode")
            return ApiResponse(statusCode, body)
        } finally {
            conn.disconnect()
        }
    }

    fun get(urlString: String, authToken: String? = null): ApiResponse {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            authToken?.let {
                conn.setRequestProperty("Authorization", "Bearer $it")
            }
            conn.doInput = true

            val statusCode = conn.responseCode
            val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            Log.d(TAG, "GET $urlString -> $statusCode")
            return ApiResponse(statusCode, body)
        } finally {
            conn.disconnect()
        }
    }
}
