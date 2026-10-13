package com.shdarv.yalda

import com.shdarv.yalda.localization.AppStringCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppStringCatalogTest {
    @Test
    fun everySupportedLanguageHasEveryUiString() {
        assertEquals(8, AppStringCatalog.supportedLanguageCodes.size)
        AppStringCatalog.rows.forEach { row ->
            AppStringCatalog.supportedLanguageCodes.forEach { languageCode ->
                assertTrue(
                    row.value(languageCode).isNotBlank(),
                    "Missing $languageCode translation for ${row.en}"
                )
            }
        }
    }

    @Test
    fun translationsPreserveFormatArguments() {
        val placeholder = Regex("\\{\\d+}")
        AppStringCatalog.rows.forEach { row ->
            val expected = placeholder.findAll(row.en).map { it.value }.toSet()
            AppStringCatalog.supportedLanguageCodes.forEach { languageCode ->
                assertEquals(
                    expected,
                    placeholder.findAll(row.value(languageCode)).map { it.value }.toSet(),
                    "Placeholder mismatch for $languageCode translation of ${row.en}"
                )
            }
        }
    }

    @Test
    fun eachProfileLanguageSelectsItsOwnCatalog() {
        assertEquals("Biblioteca", AppStringCatalog.stringsFor("es")("Library"))
        assertEquals("Bibliothèque", AppStringCatalog.stringsFor("fr")("Library"))
        assertEquals("Bibliothek", AppStringCatalog.stringsFor("de")("Library"))
        assertEquals("ライブラリ", AppStringCatalog.stringsFor("ja")("Library"))
        assertEquals("کتابخانه", AppStringCatalog.stringsFor("fa")("Library"))
        assertEquals("Raccolta", AppStringCatalog.stringsFor("it")("Library"))
        assertEquals("Biblioteca", AppStringCatalog.stringsFor("pt")("Library"))
        assertEquals("Library", AppStringCatalog.stringsFor("en")("Library"))
    }

    @Test
    fun dynamicServiceMessagesAreLocalized() {
        val persian = AppStringCatalog.stringsFor("fa")

        assertEquals("در حال آماده‌سازی ترجمه آفلاین…", persian.runtime("Preparing English → Persian..."))
        assertEquals("بسته زبان انگلیسی حذف شد.", persian.runtime("English language pack was removed."))
        assertEquals(
            "Basics: 2 افزوده، 1 به‌روزرسانی، 3 ویرایش حفظ و 4 تداخل رد شد.",
            persian.runtime("Basics: 2 added, 1 updated, 3 edits preserved, 4 collisions skipped.")
        )
    }
}
