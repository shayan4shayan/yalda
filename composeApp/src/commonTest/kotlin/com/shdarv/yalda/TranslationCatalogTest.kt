package com.shdarv.yalda

import com.shdarv.yalda.translation.TranslationCatalog
import com.shdarv.yalda.translation.translationPairIsReady
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TranslationCatalogTest {
    @Test
    fun exposesEveryDirectedCombinationOfEightLanguages() {
        assertEquals(8, TranslationCatalog.languages.size)
        assertEquals(56, TranslationCatalog.directedPairs.size)
        assertEquals(56, TranslationCatalog.directedPairs.map { it.id }.distinct().size)
        TranslationCatalog.languages.forEach { source ->
            TranslationCatalog.languages.filter { it != source }.forEach { target ->
                assertNotNull(TranslationCatalog.pairFor(source.code, target.code))
            }
        }
    }

    @Test
    fun readinessRequiresBothSharedLanguagePacks() {
        val pair = assertNotNull(TranslationCatalog.pairFor("de", "fa"))
        assertFalse(translationPairIsReady(pair, emptySet()))
        assertFalse(translationPairIsReady(pair, setOf("de")))
        assertTrue(translationPairIsReady(pair, setOf("en", "de", "fa")))
    }
}
