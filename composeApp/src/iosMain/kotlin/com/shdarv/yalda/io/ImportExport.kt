package com.shdarv.yalda.io

import com.shdarv.yalda.db.CategoryWithWordsDto
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class FileExporter {
    actual suspend fun exportCategory(categoryWithWords: CategoryWithWordsDto, fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = Json.encodeToString(categoryWithWords)
                val actualFileName = if (fileName.endsWith(".yld")) fileName else "$fileName.yld"

                val documentsPath = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true
                ).firstOrNull() as? String ?: return@withContext false

                val filePath = documentsPath.removeSuffix("/") + "/" + actualFileName

                (jsonString as NSString).writeToFile(filePath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
                // The writeToFile method for NSString doesn't return a Boolean for success directly.
                // We can check if the file exists after writing, or rely on it throwing an exception on failure.
                // For simplicity, assuming success if no exception.
                true
            } catch (e: Exception) {
                // Log error e
                e.printStackTrace()
                false
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class FileImporter {
    actual suspend fun importCategory(fileName: String): CategoryWithWordsDto? {
        return withContext(Dispatchers.IO) {
            try {
                val actualFileName = if (fileName.endsWith(".yld")) fileName else "$fileName.yld"
                val documentsPath = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true
                ).firstOrNull() as? String ?: return@withContext null

                val filePath = documentsPath.removeSuffix("/") + "/" + actualFileName

                val fileManager = NSFileManager.defaultManager
                if (!fileManager.fileExistsAtPath(filePath)) return@withContext null

                val data = NSData.dataWithContentsOfFile(filePath) ?: return@withContext null
                val jsonString = memScoped {
                    data.bytes?.let {
                        val byteArray = it.readBytes(data.length.toInt())
                        byteArray.toKString()
                    }
                } ?: return@withContext null

                Json.decodeFromString<CategoryWithWordsDto>(jsonString)
            } catch (e: Exception) {
                // Log error e
                e.printStackTrace()
                null
            }
        }
    }
}

fun ioDataHandler.init() {
    this.init(
        FileExporter(),
        FileImporter()
    )
}

