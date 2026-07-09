package com.shdarv.yalda.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Shared Ktor HttpClient configured with Kotlinx Serialization.
 * Platform engines are provided via androidMain (OkHttp) and iosMain (Darwin).
 */
fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            }
        )
    }

    install(Logging) {
        level = LogLevel.ALL
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
        //accept(ContentType.Application.Json)
        accept(ContentType.Any)
    }
}

