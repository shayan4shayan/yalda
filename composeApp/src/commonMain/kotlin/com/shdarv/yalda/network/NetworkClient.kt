package com.shdarv.yalda.network

import com.shdarv.yalda.models.DATASET_SCHEMA_VERSION
import com.shdarv.yalda.models.DatasetCatalog
import com.shdarv.yalda.models.DatasetCategoryMetadata
import com.shdarv.yalda.models.DatasetEntry
import com.shdarv.yalda.models.DatasetPack
import com.shdarv.yalda.models.DatasetPackFile
import com.shdarv.yalda.models.DatasetValidator
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

class NetworkClient(val client: HttpClient = createHttpClient()) {
    suspend inline fun <reified T> get(url: String): T = client.get(url).body()
    suspend inline fun <reified Req, reified Res> post(url: String, payload: Req): Res =
        client.post(url) { setBody(payload) }.body()
    suspend inline fun <reified Req, reified Res> put(url: String, payload: Req): Res =
        client.put(url) { setBody(payload) }.body()
    suspend inline fun <reified T> delete(url: String): T = client.delete(url).body()
}

const val baseUrl = "https://raw.githubusercontent.com/shayan4shayan/yalda-public-dataset/refs/heads/main"
const val catalogV2Url = "$baseUrl/catalog.v2.json"
const val indexUrl = "$baseUrl/files.index.json"

enum class DatasetCatalogSource { NetworkV2, CacheV2, LegacyIndex }

data class LoadedDatasetCatalog(
    val catalog: DatasetCatalog,
    val source: DatasetCatalogSource,
    val rawV2: String? = null
)

object github {
    private val client = NetworkClient()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun getCatalog(cachedV2: String?): LoadedDatasetCatalog {
        val networkV2 = runCatching {
            val raw: String = client.get(catalogV2Url)
            val catalog = DatasetValidator.validateCatalog(json.decodeFromString<DatasetCatalog>(raw))
            LoadedDatasetCatalog(catalog, DatasetCatalogSource.NetworkV2, raw)
        }.getOrNull()
        if (networkV2 != null) return networkV2

        val cached = cachedV2?.takeIf(String::isNotBlank)?.let { raw ->
            runCatching {
                val catalog = DatasetValidator.validateCatalog(json.decodeFromString<DatasetCatalog>(raw))
                LoadedDatasetCatalog(catalog, DatasetCatalogSource.CacheV2, raw)
            }.getOrNull()
        }
        if (cached != null) return cached

        val legacyFiles = runCatching {
            val raw: String = client.get(indexUrl)
            json.decodeFromString<List<RemoteFile>>(raw)
        }.getOrNull()
        if (legacyFiles != null) {
            val packs = legacyFiles.mapIndexed { index, file ->
                DatasetPack(
                    id = "legacy-en-fa-${slug(file.name)}-${index + 1}",
                    version = "legacy-1",
                    title = file.name,
                    sourceLanguage = "en",
                    targetLanguage = "fa",
                    url = file.address,
                    wordCount = 0,
                    sha256 = "0".repeat(64),
                    license = "CC BY 4.0",
                    recommendedDefault = false,
                    legacy = true
                )
            }
            return LoadedDatasetCatalog(
                DatasetCatalog(DATASET_SCHEMA_VERSION, "legacy-1", packs),
                DatasetCatalogSource.LegacyIndex
            )
        }
        error("Dataset catalog is unavailable. Check your connection and try again.")
    }

    suspend fun getPack(pack: DatasetPack): DatasetPackFile {
        val raw: String = client.get(pack.url)
        if (pack.legacy) {
            val words = json.decodeFromString<List<RemoteWordEntry>>(raw)
            val entries = words.mapIndexed { index, word ->
                DatasetEntry(
                    id = "${pack.id}-${slug(word.word)}-${index + 1}",
                    word = word.word,
                    meaning = word.meaning,
                    description = word.description
                )
            }
            return DatasetValidator.validateLegacyPack(
                pack,
                DatasetPackFile(
                    schemaVersion = DATASET_SCHEMA_VERSION,
                    packId = pack.id,
                    version = pack.version,
                    sourceLanguage = "en",
                    targetLanguage = "fa",
                    category = DatasetCategoryMetadata(pack.id, pack.title),
                    entries = entries
                )
            )
        }
        DatasetValidator.verifyChecksum(pack.sha256, raw.encodeToByteArray())
        return DatasetValidator.validatePack(pack, json.decodeFromString<DatasetPackFile>(raw))
    }

    private fun slug(value: String): String = value.lowercase()
        .map { character -> if (character.isLetterOrDigit()) character else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')
        .ifBlank { "pack" }
}
