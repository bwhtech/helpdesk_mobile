package io.github.kaulith.helpdeskanalytics.e2e

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate

/** A Helpdesk site on the device itself, answering the calls the app makes. */
object FakeSite {

    private val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()

    val clientCertificates: HandshakeCertificates = HandshakeCertificates.Builder()
        .addTrustedCertificate(certificate.certificate)
        .build()

    private val server = MockWebServer().apply {
        useHttps(HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory())
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = respond(request)
        }
    }

    val url: String
        get() {
            if (!server.started) server.start()
            return "https://localhost:${server.port}"
        }

    // A request for /api/resource/HD%20Ticket is answered from fake_site/api/resource/HD Ticket.json.
    private fun respond(request: RecordedRequest): MockResponse {
        val path = request.url.pathSegments.joinToString("/")
        val body = runCatching {
            InstrumentationRegistry.getInstrumentation().context.assets
                .open("fake_site/$path.json").bufferedReader().use { it.readText() }
        }.getOrElse {
            Log.w("FakeSite", "unhandled ${request.method} ${request.target}")
            return MockResponse(code = 404)
        }
        return MockResponse.Builder().addHeader("Content-Type", "application/json").body(body).build()
    }
}
