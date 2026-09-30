package com.example.util

import android.content.Context
import android.util.Log
import java.io.BufferedOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

object LocalAssetServer {
    private const val TAG = "LocalAssetServer"
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    @Volatile
    private var isRunning = false
    var serverPort = 8080
        private set

    fun start(context: Context): Int {
        if (isRunning && serverSocket != null && !serverSocket!!.isClosed) {
            return serverPort
        }

        try {
            val loopback = InetAddress.getByName("127.0.0.1")
            try {
                serverSocket = ServerSocket(8080, 50, loopback)
                serverPort = 8080
            } catch (_: Exception) {
                serverSocket = ServerSocket(0, 50, loopback)
                serverPort = serverSocket!!.localPort
            }

            isRunning = true
            Log.i(TAG, "LocalAssetServer started on http://127.0.0.1:$serverPort/")

            val appContext = context.applicationContext

            executor.execute {
                while (isRunning) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        executor.execute { handleClient(appContext, socket) }
                    } catch (e: Exception) {
                        if (!isRunning) break
                        Log.e(TAG, "Error accepting client", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocalAssetServer", e)
        }

        return serverPort
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    private fun handleClient(context: Context, socket: Socket) {
        socket.use { client ->
            client.soTimeout = 10000
            try {
                val input = client.getInputStream()
                val reader = input.bufferedReader()
                val requestLine = reader.readLine() ?: return

                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                var path = parts[1]

                if (path == "/") path = "/leaflet_map.html"
                if (path.contains("?")) path = path.substringBefore("?")

                val assetPath = path.trimStart('/')

                val output = BufferedOutputStream(client.getOutputStream())

                if (method != "GET" && method != "HEAD") {
                    sendResponse(output, 405, "Method Not Allowed", "text/plain", "Method Not Allowed".toByteArray())
                    return
                }

                try {
                    val inputStream: InputStream = context.assets.open(assetPath)
                    val data = inputStream.use { it.readBytes() }
                    val mimeType = getMimeType(assetPath)

                    sendResponse(output, 200, "OK", mimeType, data)
                } catch (e: Exception) {
                    Log.w(TAG, "Asset not found: $assetPath")
                    sendResponse(output, 404, "Not Found", "text/plain", "404 Not Found".toByteArray())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client request", e)
            }
        }
    }

    private fun sendResponse(
        output: BufferedOutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray
    ) {
        val headers = StringBuilder()
        headers.append("HTTP/1.1 $statusCode $statusText\r\n")
        headers.append("Content-Type: $contentType\r\n")
        headers.append("Content-Length: ${body.size}\r\n")
        headers.append("Access-Control-Allow-Origin: *\r\n")
        headers.append("Access-Control-Allow-Methods: GET, HEAD, OPTIONS\r\n")
        headers.append("Access-Control-Allow-Headers: *\r\n")
        headers.append("Cache-Control: no-cache, no-store, must-revalidate\r\n")
        headers.append("Connection: close\r\n")
        headers.append("\r\n")

        output.write(headers.toString().toByteArray(Charsets.UTF_8))
        output.write(body)
        output.flush()
    }

    private fun getMimeType(path: String): String {
        return when {
            path.endsWith(".html", ignoreCase = true) -> "text/html; charset=utf-8"
            path.endsWith(".js", ignoreCase = true) -> "application/javascript; charset=utf-8"
            path.endsWith(".css", ignoreCase = true) -> "text/css; charset=utf-8"
            path.endsWith(".glb", ignoreCase = true) -> "model/gltf-binary"
            path.endsWith(".gltf", ignoreCase = true) -> "model/gltf+json"
            path.endsWith(".png", ignoreCase = true) -> "image/png"
            path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            path.endsWith(".json", ignoreCase = true) -> "application/json; charset=utf-8"
            path.endsWith(".wasm", ignoreCase = true) -> "application/wasm"
            else -> "application/octet-stream"
        }
    }
}
