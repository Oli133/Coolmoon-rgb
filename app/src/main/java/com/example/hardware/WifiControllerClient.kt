package com.example.hardware

import com.example.model.WifiControllerConfig
import com.example.model.WifiProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

data class WifiDispatchResult(
    val success: Boolean,
    val message: String
)

class WifiControllerClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .writeTimeout(2500, TimeUnit.MILLISECONDS)
        .build()

    suspend fun pingController(config: WifiControllerConfig): WifiDispatchResult = withContext(Dispatchers.IO) {
        try {
            when (config.protocol) {
                WifiProtocol.MAGIC_HOME_TCP -> {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(config.ipAddress.trim(), config.port), 2200)
                    }
                    WifiDispatchResult(true, "TCP handshake succeeded on ${config.ipAddress}:${config.port}")
                }
                WifiProtocol.WLED_HTTP_JSON -> {
                    val req = Request.Builder()
                        .url("http://${config.ipAddress.trim()}:${config.port}/json/info")
                        .get()
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            WifiDispatchResult(true, "WLED controller reachable (${resp.code})")
                        } else {
                            WifiDispatchResult(false, "HTTP ${resp.code} from ${config.ipAddress}")
                        }
                    }
                }
                WifiProtocol.WLED_UDP_DRGB -> {
                    val reachable = InetAddress.getByName(config.ipAddress.trim()).isReachable(1800)
                    if (reachable) {
                        WifiDispatchResult(true, "Host ${config.ipAddress} reachable for UDP ${config.port}")
                    } else {
                        WifiDispatchResult(false, "Host ${config.ipAddress} did not respond to ping")
                    }
                }
            }
        } catch (e: Exception) {
            WifiDispatchResult(false, e.localizedMessage ?: "Connection timed out")
        }
    }

    suspend fun sendBytesTcp(config: WifiControllerConfig, payload: ByteArray): WifiDispatchResult =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(config.ipAddress.trim(), config.port), 2000)
                    socket.getOutputStream().use { os ->
                        os.write(payload)
                        os.flush()
                    }
                }
                WifiDispatchResult(true, "Sent ${payload.size} bytes to ${config.ipAddress}:${config.port}")
            } catch (e: Exception) {
                WifiDispatchResult(false, "TCP send failed: ${e.localizedMessage ?: "Unreachable"}")
            }
        }

    suspend fun sendWledJson(config: WifiControllerConfig, jsonBody: String): WifiDispatchResult =
        withContext(Dispatchers.IO) {
            try {
                val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
                val req = Request.Builder()
                    .url("http://${config.ipAddress.trim()}:${config.port}/json/state")
                    .post(body)
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        WifiDispatchResult(true, "Updated WLED state via HTTP")
                    } else {
                        WifiDispatchResult(false, "WLED returned HTTP ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                WifiDispatchResult(false, "WLED HTTP failed: ${e.localizedMessage ?: "Unreachable"}")
            }
        }

    suspend fun sendUdpPacket(config: WifiControllerConfig, payload: ByteArray): WifiDispatchResult =
        withContext(Dispatchers.IO) {
            try {
                DatagramSocket().use { socket ->
                    val address = InetAddress.getByName(config.ipAddress.trim())
                    val packet = DatagramPacket(payload, payload.size, address, config.port)
                    socket.send(packet)
                }
                WifiDispatchResult(true, "Sent ${payload.size}B UDP frame to ${config.ipAddress}:${config.port}")
            } catch (e: Exception) {
                WifiDispatchResult(false, "UDP send failed: ${e.localizedMessage ?: "Unreachable"}")
            }
        }
}
