package com.shdarv.yalda.network

import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.db.WordEntryDto
import com.shdarv.yalda.models.RemoteFile
import com.shdarv.yalda.models.RemoteWordEntry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json

class NetworkClient(
    val client: HttpClient = createHttpClient()
) {
    suspend inline fun <reified T> get(url: String): T = client.get(url).body()

    suspend inline fun <reified Req, reified Res> post(url: String, payload: Req): Res =
        client.post(url) { setBody(payload) }.body()

    suspend inline fun <reified Req, reified Res> put(url: String, payload: Req): Res =
        client.put(url) { setBody(payload) }.body()

    suspend inline fun <reified T> delete(url: String): T = client.delete(url).body()
}

const val baseUrl = "https://raw.githubusercontent.com/shayan4shayan/yalda-public-dataset/refs/heads/main"

const val indexUrl = "$baseUrl/files.index.json"

object github {

    private val client = NetworkClient()

    suspend fun getFilesList(): List<RemoteFile> {
        var response: List<RemoteFile>
        try {
            val contentString: String = client.get(indexUrl)
            response = Json.decodeFromString(contentString)
        } catch (e: Exception) {
            e.printStackTrace()
            response = emptyList()
        }
        return response
    }

    suspend fun getWordsList(address: String): List<RemoteWordEntry> {
        var response: List<RemoteWordEntry>
        try {
            val contentString: String = client.get(address)
            response = Json.decodeFromString(contentString)
        } catch (e: Exception) {
            e.printStackTrace()
            response = emptyList()
        }
        return response
    }
}
