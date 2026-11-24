package com.shrey.prankspot.portal

import android.util.Log
import com.shrey.prankspot.domain.Visitor
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.InetAddress

/**
 * The local prank web server. Binds only to the hotspot's own address, answers GET only, and
 * serves pages built in memory: there's no file access and no admin endpoint.
 */
class PortalServer(private val pageProvider: () -> String) {
    private var server: EmbeddedServer<*, *>? = null
    private val _visitors = MutableStateFlow<Map<String, Visitor>>(emptyMap())
    val visitors: StateFlow<Map<String, Visitor>> = _visitors.asStateFlow()

    var port: Int = 0
        private set
    private var host: String = ""

    /** Starts on the first free port of [PORTS]. Returns the URL or throws. */
    fun start(host: String): String {
        stop()
        _visitors.value = emptyMap()
        var last: Exception? = null
        for (p in PORTS) {
            try {
                val s = embeddedServer(CIO, port = p, host = host) { routes() }
                s.start(wait = false)
                server = s
                port = p
                this.host = host
                return "http://$host:$p/"
            } catch (e: Exception) {
                Log.w(TAG, "Port $p unavailable on $host", e)
                last = e
            }
        }
        throw IllegalStateException("No free port for the prank page", last)
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 200, timeoutMillis = 1_000)
        server = null
    }

    private fun io.ktor.server.application.Application.routes() {
        routing {
            get("/health") { call.respondText("ok") }
            get("/") {
                record(call)
                call.response.header(HttpHeaders.CacheControl, "no-store")
                call.respondText(runCatching(pageProvider).getOrElse { PageRenderer.fallback() }, ContentType.Text.Html)
            }
            // Captive-portal probes from Android, iOS and Windows, and any other path: send to the prank.
            route("{...}") {
                handle {
                    if (call.request.httpMethod != HttpMethod.Get && call.request.httpMethod != HttpMethod.Head) {
                        call.respondText("Not allowed", status = HttpStatusCode.MethodNotAllowed)
                    } else if (call.request.uri.length > 2048) {
                        call.respondText("Too long", status = HttpStatusCode.BadRequest)
                    } else {
                        call.respondRedirect("/", permanent = false)
                    }
                }
            }
        }
    }

    private fun record(call: ApplicationCall) {
        val ip = call.request.origin.remoteHost
        // Only count guests on the hotspot's private network.
        val private = runCatching { InetAddress.getByName(ip).let { it.isSiteLocalAddress || it.isLoopbackAddress } }.getOrDefault(false)
        // The host phone opening its own page isn't a guest.
        if (!private || ip == host) return
        val ua = call.request.header(HttpHeaders.UserAgent).orEmpty().take(200)
        val now = System.currentTimeMillis()
        _visitors.update { m ->
            val old = m[ip]
            m + (ip to Visitor(ip, old?.firstSeen ?: now, now, (old?.visits ?: 0) + 1, ua))
        }
    }

    companion object {
        private const val TAG = "PortalServer"
        val PORTS = listOf(8080, 8888, 8000, 8181)
    }
}
