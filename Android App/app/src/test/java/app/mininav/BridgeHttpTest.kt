package app.mininav

import java.net.Socket
import java.net.BindException
import java.util.concurrent.Executors
import org.junit.Assert.*
import org.junit.Test

class BridgeHttpTest {
    private val token="0123456789abcdef0123456789abcdef"
    private fun call(port: Int, request: String): String = Socket("127.0.0.1",port).use {
        it.soTimeout=3000; it.getOutputStream().write(request.toByteArray()); it.getInputStream().bufferedReader().readText()
    }
    private fun request(method: String="GET", path: String="/v1/state", auth: String=token) = "$method $path HTTP/1.1\r\nHost: 127.0.0.1\r\nAuthorization: Bearer $auth\r\n\r\n"
    @Test fun authenticationAndReadOnly() {
        BridgeHttp(token,{"{\"protocolVersion\":1}"}).use { s ->
            s.start(0)
            assertTrue(call(s.port,request()).contains("200 OK"))
            assertTrue(call(s.port,request(auth="wrong")).contains("401 Error"))
            assertTrue(call(s.port,request(method="POST")).contains("405 Error"))
            assertTrue(call(s.port,request(path="/other")).contains("404 Error"))
            assertTrue(call(s.port,"GET /v1/state HTTP/1.1\r\nX: ${"x".repeat(8192)}\r\n\r\n").contains("431 Error"))
            assertTrue(call(s.port,request().replace("\r\n\r\n","\r\nContent-Length: 1\r\n\r\n")).contains("400 Error"))
            assertTrue(call(s.port,request().replace("\r\n\r\n","\r\nAuthorization: duplicate\r\n\r\n")).contains("400 Error"))
        }
    }
    @Test fun conflictingPortAndConcurrentPolls() {
        BridgeHttp(token,{"{}"}).use { s ->
            s.start(0)
            BridgeHttp(token,{"{}"}).use { other -> assertThrows(BindException::class.java) { other.start(s.port) } }
            val workers=Executors.newFixedThreadPool(4)
            try { val results=(1..4).map { workers.submit<String> { call(s.port,request()) } }; results.forEach { assertTrue(it.get().contains("200 OK")) } }
            finally { workers.shutdownNow() }
        }
    }
    @Test fun slowHeadersCannotHoldWorkerIndefinitely() {
        BridgeHttp(token,{"{}"}).use { server ->
            server.start(0)
            Socket("127.0.0.1",server.port).use { client ->
                client.soTimeout=3000
                val writer=Executors.newSingleThreadExecutor()
                try {
                    val pending=writer.submit {
                        try { repeat(20) { client.getOutputStream().write('G'.code); Thread.sleep(200) } }
                        catch (_: Exception) { }
                    }
                    val started=System.nanoTime()
                    assertEquals(-1,client.getInputStream().read())
                    assertTrue(java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started)<2800)
                    pending.cancel(true)
                } finally { writer.shutdownNow() }
            }
        }
    }
}
