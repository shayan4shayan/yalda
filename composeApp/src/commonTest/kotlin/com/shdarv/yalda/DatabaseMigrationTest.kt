package com.shdarv.yalda

import com.shdarv.yalda.db.LegacyWordOwner
import com.shdarv.yalda.db.projectLegacyCategoryOwners
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseMigrationTest {
    @Test
    fun sharedCategoriesAreCopiedAndEmptyCategoriesGoToOldestProfile() {
        val projection = projectLegacyCategoryOwners(
            profileIdsInCreationOrder = listOf(1, 2),
            legacyCategoryIds = listOf(10, 20),
            wordOwners = listOf(
                LegacyWordOwner(categoryId = 10, profileId = 1),
                LegacyWordOwner(categoryId = 10, profileId = 2)
            )
        )

        assertEquals(
            setOf(
                LegacyWordOwner(categoryId = 10, profileId = 1),
                LegacyWordOwner(categoryId = 10, profileId = 2),
                LegacyWordOwner(categoryId = 20, profileId = 1)
            ),
            projection
        )
    }
}
