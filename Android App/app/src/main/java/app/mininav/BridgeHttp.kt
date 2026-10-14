package app.mininav

import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Bounded, read-only HTTP/1.1 endpoint. No request bodies or remote bindings. */
class BridgeHttp(private val token: String, private val state: () -> String, private val onRead: () -> Unit = {}) : AutoCloseable {
    private var socket: ServerSocket? = null
    private val clients = java.util.concurrent.ConcurrentHashMap.newKeySet<Socket>()
    private val pool = ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(4))
    val port get() = socket?.localPort ?: 0
    fun start(port: Int) {
        socket = ServerSocket(port, 8, InetAddress.getByName("127.0.0.1"))
        Thread({
            while (socket?.isClosed == false) {
                val client = try { socket!!.accept() } catch (_: Exception) { break }
                clients.add(client)
                try { pool.execute { client.use { serve(it) }; clients.remove(client) } }
                catch (_: java.util.concurrent.RejectedExecutionException) { clients.remove(client); client.close() }
            }
        }, "MiniNav HTTP").apply { isDaemon = true; start() }
    }
    private fun serve(client: Socket) {
        try {
            client.soTimeout = 1500
            val input = client.getInputStream(); val buffer = ByteArrayOutputStream()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            var end = 0
            while (buffer.size() < 8192 && end != 0x0d0a0d0a) {
                val remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
                if (remaining <= 0) return
                client.soTimeout = minOf(1500, remaining.toInt()).coerceAtLeast(1)
                val b = input.read(); if (b < 0) return
                buffer.write(b); end = (end shl 8) or b
            }
            if (end != 0x0d0a0d0a) { respond(client, 431, "Headers too large"); return }
            val lines = buffer.toString("US-ASCII").split("\r\n")
            val request = lines[0].split(' ')
            val headers = linkedMapOf<String, String>()
            for (line in lines.drop(1).filter { it.isNotEmpty() }) {
                val i = line.indexOf(':')
                if (i <= 0 || headers.put(line.substring(0, i).lowercase(), line.substring(i+1).trim()) != null) {
                    respond(client, 400, "Invalid headers"); return
                }
            }
            val auth = headers["authorization"].orEmpty()
            val status = when {
                request.size != 3 || request[2] != "HTTP/1.1" -> 400
                request[0] != "GET" -> 405
                request[1] != "/v1/state" -> 404
                headers["transfer-encoding"] != null || headers["content-length"]?.let { it != "0" } == true -> 400
                !MessageDigest.isEqual(auth.toByteArray(), "Bearer $token".toByteArray()) -> 401
                else -> 200
            }
            if (status == 200) { onRead(); respond(client, status, state()) }
            else respond(client, status, "Request rejected")
        } catch (_: Exception) { /* Closed/time-limited client. Never log request contents. */ }
    }
    private fun respond(client: Socket, code: Int, body: String) {
        val data = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code ${if(code == 200) "OK" else "Error"}\r\nContent-Type: ${if(code == 200) "application/json" else "text/plain"}; charset=utf-8\r\nContent-Length: ${data.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n"
        client.getOutputStream().apply { write(header.toByteArray()); write(data); flush() }
    }
    override fun close() { socket?.close(); clients.forEach { try { it.close() } catch (_: Exception) {} }; pool.shutdownNow() }
}
