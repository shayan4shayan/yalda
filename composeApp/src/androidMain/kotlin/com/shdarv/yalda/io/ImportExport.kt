package com.shdarv.yalda.io


import android.content.Context
import com.shdarv.yalda.db.CategoryWithWordsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

actual class FileExporter(private val context: Context) {
    actual suspend fun exportCategory(categoryWithWords: CategoryWithWordsDto, fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = Json.encodeToString(categoryWithWords)
                // Ensure the filename has the .yld extension
                val actualFileName = if (fileName.endsWith(".yld")) fileName else "$fileName.yld"

                // Using app-specific directory. You might want to use shared storage
                // with appropriate permissions and Storage Access Framework for broader access.
                val file = File(context.filesDir, actualFileName)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                true
            } catch (e: Exception) {
                // Log error e
                e.printStackTrace()
                false
            }
        }
    }
}

actual class FileImporter(private val context: Context) {
    actual suspend fun importCategory(fileName: String): CategoryWithWordsDto? {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure the filename has the .yld extension if not provided
                val actualFileName = if (fileName.endsWith(".yld")) fileName else "$fileName.yld"
                val file = File(context.filesDir, actualFileName) // Or from a URI if using SAF

                if (!file.exists()) return@withContext null

                val jsonString = file.inputStream().use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        reader.readText()
                    }
                }
                Json.decodeFromString<CategoryWithWordsDto>(jsonString)
            } catch (e: Exception) {
                // Log error e
                e.printStackTrace()
                null
            }
        }
    }
}


fun ioDataHandler.init(context: Context) {
    this.init(
        FileExporter(context),
        FileImporter(context)
    )
}
