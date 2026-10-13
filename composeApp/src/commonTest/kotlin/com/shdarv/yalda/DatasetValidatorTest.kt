package com.shdarv.yalda

import com.shdarv.yalda.models.DATASET_SCHEMA_VERSION
import com.shdarv.yalda.models.DatasetCatalog
import com.shdarv.yalda.models.DatasetCategoryMetadata
import com.shdarv.yalda.models.DatasetEntry
import com.shdarv.yalda.models.DatasetImportPlanner
import com.shdarv.yalda.models.DatasetMergeAction
import com.shdarv.yalda.models.DatasetPack
import com.shdarv.yalda.models.DatasetPackFile
import com.shdarv.yalda.models.DatasetValidationException
import com.shdarv.yalda.models.DatasetValidator
import com.shdarv.yalda.models.Sha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DatasetValidatorTest {
    private val entry = DatasetEntry("en-fa.travel.airport", "airport", "فرودگاه", "A place used by aircraft and passengers.")
    private val pack = DatasetPack(
        id = "en-fa.travel",
        version = "2",
        title = "Travel",
        sourceLanguage = "en",
        targetLanguage = "fa",
        url = "https://example.test/travel.v2.json",
        wordCount = 1,
        sha256 = "a".repeat(64),
        license = "CC BY 4.0",
        recommendedDefault = true
    )
    private val file = DatasetPackFile(
        schemaVersion = DATASET_SCHEMA_VERSION,
        packId = pack.id,
        version = pack.version,
        sourceLanguage = pack.sourceLanguage,
        targetLanguage = pack.targetLanguage,
        category = DatasetCategoryMetadata(pack.id, pack.title),
        entries = listOf(entry)
    )

    @Test
    fun sha256MatchesPublishedVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.hex("abc".encodeToByteArray())
        )
    }

    @Test
    fun validCatalogAndPackPass() {
        DatasetValidator.validateCatalog(DatasetCatalog(2, "3", listOf(pack)))
        assertEquals(file, DatasetValidator.validatePack(pack, file))
    }

    @Test
    fun wrongPairCountChecksumAndDuplicateIdsFail() {
        assertFailsWith<DatasetValidationException> {
            DatasetValidator.validatePack(pack, file.copy(targetLanguage = "es"))
        }
        assertFailsWith<DatasetValidationException> {
            DatasetValidator.validatePack(pack.copy(wordCount = 2), file)
        }
        assertFailsWith<DatasetValidationException> {
            DatasetValidator.verifyChecksum("0".repeat(64), "different".encodeToByteArray())
        }
        assertFailsWith<DatasetValidationException> {
            DatasetValidator.validatePack(
                pack.copy(wordCount = 2),
                file.copy(entries = listOf(entry, entry.copy(word = "terminal")))
            )
        }
    }

    @Test
    fun importPlannerProtectsUserContentAndAllowsUpdates() {
        assertEquals(DatasetMergeAction.Insert, DatasetImportPlanner.decide(null, false))
        assertEquals(DatasetMergeAction.SkipUserOwnedCollision, DatasetImportPlanner.decide(null, true))
        assertEquals(DatasetMergeAction.UpdateDatasetEntry, DatasetImportPlanner.decide(false, true))
        assertEquals(DatasetMergeAction.PreserveUserEdit, DatasetImportPlanner.decide(true, false))
    }
}
