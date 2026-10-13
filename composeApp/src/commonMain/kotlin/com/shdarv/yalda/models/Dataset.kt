package com.shdarv.yalda.models

import kotlinx.serialization.Serializable

const val DATASET_SCHEMA_VERSION = 2

@Serializable
data class DatasetCatalog(
    val schemaVersion: Int,
    val catalogVersion: String,
    val packs: List<DatasetPack>
)

@Serializable
data class DatasetPack(
    val id: String,
    val version: String,
    val title: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val url: String,
    val wordCount: Int,
    val sha256: String,
    val license: String,
    val recommendedDefault: Boolean,
    val legacy: Boolean = false
)

@Serializable
data class DatasetCategoryMetadata(
    val id: String,
    val title: String
)

@Serializable
data class DatasetPackFile(
    val schemaVersion: Int,
    val packId: String,
    val version: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val category: DatasetCategoryMetadata,
    val entries: List<DatasetEntry>
)

@Serializable
data class DatasetEntry(
    val id: String,
    val word: String,
    val meaning: String,
    val description: String
)

@Serializable
data class RemoteFile(val name: String, val address: String)

@Serializable
data class RemoteWordEntry(val word: String, val meaning: String, val description: String)

data class DatasetImportSummary(
    val added: Int = 0,
    val updated: Int = 0,
    val unchanged: Int = 0,
    val preservedUserEdits: Int = 0,
    val skippedCollisions: Int = 0
) {
    val totalHandled: Int
        get() = added + updated + unchanged + preservedUserEdits + skippedCollisions

    fun displayText(packTitle: String): String =
        "$packTitle: $added added, $updated updated, $preservedUserEdits edits preserved, " +
            "$skippedCollisions collisions skipped."
}

enum class DatasetMergeAction {
    Insert,
    UpdateDatasetEntry,
    PreserveUserEdit,
    SkipUserOwnedCollision
}

object DatasetImportPlanner {
    fun decide(
        existingDatasetEntryIsUserModified: Boolean?,
        hasUserOwnedWordCollision: Boolean
    ): DatasetMergeAction = when {
        existingDatasetEntryIsUserModified == true -> DatasetMergeAction.PreserveUserEdit
        existingDatasetEntryIsUserModified == false -> DatasetMergeAction.UpdateDatasetEntry
        hasUserOwnedWordCollision -> DatasetMergeAction.SkipUserOwnedCollision
        else -> DatasetMergeAction.Insert
    }
}

class DatasetValidationException(message: String) : IllegalArgumentException(message)

object DatasetValidator {
    private val languageCode = Regex("^[a-z]{2,3}$")
    private val sha256 = Regex("^[0-9a-fA-F]{64}$")

    fun validateCatalog(catalog: DatasetCatalog): DatasetCatalog {
        requireValid(catalog.schemaVersion == DATASET_SCHEMA_VERSION, "Unsupported catalog schema.")
        requireValid(catalog.catalogVersion.isNotBlank(), "Catalog version is required.")
        requireValid(catalog.packs.map { it.id }.distinct().size == catalog.packs.size, "Duplicate pack ID.")
        catalog.packs.forEach(::validatePackMetadata)
        return catalog
    }

    fun validatePack(pack: DatasetPack, file: DatasetPackFile): DatasetPackFile {
        requireValid(!pack.legacy, "Legacy packs use the legacy validator.")
        validatePackMetadata(pack)
        requireValid(file.schemaVersion == DATASET_SCHEMA_VERSION, "Unsupported pack schema.")
        requireValid(file.packId == pack.id, "Pack ID does not match its catalog entry.")
        requireValid(file.version == pack.version, "Pack version does not match its catalog entry.")
        requireValid(file.sourceLanguage == pack.sourceLanguage, "Pack source language does not match.")
        requireValid(file.targetLanguage == pack.targetLanguage, "Pack target language does not match.")
        requireValid(file.category.id.isNotBlank(), "Category ID is required.")
        requireValid(file.category.title.isNotBlank(), "Category title is required.")
        requireValid(file.entries.size == pack.wordCount, "Pack word count does not match the catalog.")
        requireValid(file.entries.map { it.id }.distinct().size == file.entries.size, "Duplicate dataset entry ID.")
        file.entries.forEach { entry ->
            requireValid(entry.id.isNotBlank(), "Dataset entry ID is required.")
            requireValid(entry.word.isNotBlank(), "Dataset word is required.")
            requireValid(entry.meaning.isNotBlank(), "Dataset meaning is required.")
            requireValid(entry.description.isNotBlank(), "Dataset description is required.")
        }
        return file
    }

    fun validateLegacyPack(pack: DatasetPack, file: DatasetPackFile): DatasetPackFile {
        requireValid(pack.legacy, "Expected a legacy pack.")
        requireValid(pack.sourceLanguage == "en" && pack.targetLanguage == "fa", "Legacy packs are English to Persian only.")
        requireValid(file.entries.isNotEmpty(), "Legacy pack is empty.")
        requireValid(file.entries.map { it.id }.distinct().size == file.entries.size, "Duplicate legacy entry ID.")
        file.entries.forEach { entry ->
            requireValid(entry.word.isNotBlank() && entry.meaning.isNotBlank(), "Legacy pack contains an empty word or meaning.")
        }
        return file
    }

    fun verifyChecksum(expected: String, content: ByteArray) {
        requireValid(Sha256.hex(content).equals(expected, ignoreCase = true), "Pack checksum does not match the catalog.")
    }

    private fun validatePackMetadata(pack: DatasetPack) {
        requireValid(pack.id.isNotBlank(), "Pack ID is required.")
        requireValid(pack.version.isNotBlank(), "Pack version is required.")
        requireValid(pack.title.isNotBlank(), "Pack title is required.")
        requireValid(languageCode.matches(pack.sourceLanguage), "Invalid source language code.")
        requireValid(languageCode.matches(pack.targetLanguage), "Invalid target language code.")
        requireValid(pack.sourceLanguage != pack.targetLanguage, "Pack languages must differ.")
        requireValid(pack.url.startsWith("https://"), "Pack URL must use HTTPS.")
        requireValid(pack.wordCount >= 0, "Pack word count cannot be negative.")
        requireValid(sha256.matches(pack.sha256), "Pack SHA-256 is invalid.")
        requireValid(pack.license.isNotBlank(), "Pack license is required.")
    }

    private fun requireValid(condition: Boolean, message: String) {
        if (!condition) throw DatasetValidationException(message)
    }
}

/** Small common-code SHA-256 implementation so Android and iOS validate identical raw bytes. */
object Sha256 {
    private val constants = intArrayOf(
        0x428a2f98, 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
        0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
        0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
        0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
        0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
        0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
        0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(),
        0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt()
    )

    fun hex(input: ByteArray): String {
        val bitLength = input.size.toLong() * 8L
        val paddedSize = ((input.size + 9 + 63) / 64) * 64
        val data = ByteArray(paddedSize)
        input.copyInto(data)
        data[input.size] = 0x80.toByte()
        for (index in 0 until 8) {
            data[paddedSize - 1 - index] = (bitLength ushr (index * 8)).toByte()
        }

        val hash = intArrayOf(
            0x6a09e667, 0xbb67ae85.toInt(), 0x3c6ef372, 0xa54ff53a.toInt(),
            0x510e527f, 0x9b05688c.toInt(), 0x1f83d9ab, 0x5be0cd19
        )
        val words = IntArray(64)
        for (offset in data.indices step 64) {
            for (index in 0 until 16) {
                val base = offset + index * 4
                words[index] = ((data[base].toInt() and 0xff) shl 24) or
                    ((data[base + 1].toInt() and 0xff) shl 16) or
                    ((data[base + 2].toInt() and 0xff) shl 8) or
                    (data[base + 3].toInt() and 0xff)
            }
            for (index in 16 until 64) {
                val s0 = rotateRight(words[index - 15], 7) xor rotateRight(words[index - 15], 18) xor (words[index - 15] ushr 3)
                val s1 = rotateRight(words[index - 2], 17) xor rotateRight(words[index - 2], 19) xor (words[index - 2] ushr 10)
                words[index] = words[index - 16] + s0 + words[index - 7] + s1
            }
            var a = hash[0]
            var b = hash[1]
            var c = hash[2]
            var d = hash[3]
            var e = hash[4]
            var f = hash[5]
            var g = hash[6]
            var h = hash[7]
            for (index in 0 until 64) {
                val sum1 = rotateRight(e, 6) xor rotateRight(e, 11) xor rotateRight(e, 25)
                val choice = (e and f) xor (e.inv() and g)
                val temp1 = h + sum1 + choice + constants[index] + words[index]
                val sum0 = rotateRight(a, 2) xor rotateRight(a, 13) xor rotateRight(a, 22)
                val majority = (a and b) xor (a and c) xor (b and c)
                val temp2 = sum0 + majority
                h = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }
            hash[0] += a; hash[1] += b; hash[2] += c; hash[3] += d
            hash[4] += e; hash[5] += f; hash[6] += g; hash[7] += h
        }
        return hash.joinToString("") { it.toUInt().toString(16).padStart(8, '0') }
    }

    private fun rotateRight(value: Int, count: Int): Int =
        (value ushr count) or (value shl (32 - count))
}
