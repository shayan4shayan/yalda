package com.shdarv.yalda.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.shdarv.yalda.db.database
import com.shdarv.yalda.translation.TranslationViewModel
import kotlin.reflect.KClass

/**
 * Simple app-wide registry to keep singleton ViewModel instances.
 * Use with Composables via [rememberSingletonViewModel] helpers below.
 */
object ViewModelRegistry {
    private val cache: MutableMap<String, ViewModel> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    fun <T : ViewModel> getOrPut(
        kClass: KClass<T>, key: String?, factory: () -> T
    ): T {
        val composedKey = buildKey(kClass, key)
        val existing = cache[composedKey]
        if (existing != null) return existing as T
        val created = factory()
        cache[composedKey] = created
        return created
    }

    fun <T : ViewModel> contains(kClass: KClass<T>, key: String? = null): Boolean =
        cache.containsKey(buildKey(kClass, key))

    fun clearAll() {
        cache.clear()
    }

    fun <T : ViewModel> clear(kClass: KClass<T>, key: String? = null) {
        cache.remove(buildKey(kClass, key))
    }

    private fun <T : ViewModel> buildKey(kClass: KClass<T>, key: String?): String =
        (kClass.qualifiedName ?: kClass.simpleName ?: "VM") + (key?.let { "#$it" } ?: "")
}

/**
 * Returns a singleton ViewModel instance across the app's lifetime,
 * remembered in composition to avoid recomposition costs.
 */
@Composable
inline fun <reified T : ViewModel> rememberSingletonViewModel(
    key: String? = null, noinline factory: () -> T
): T = remember(key, T::class) {
    ViewModelRegistry.getOrPut(T::class, key, factory)
}

// Convenience helpers for current ViewModels

@Composable
fun rememberProfilesViewModel(key: String = "profiles"): ProfilesViewModel =
    rememberSingletonViewModel(key) {
        ProfilesViewModel(
            profileDao = database.get().profileDao(), wordEntryDao = database.get().wordEntryDao()
        )
    }

@Composable
fun rememberCategoriesViewModel(key: String = "categories"): CategoriesViewModel =
    rememberSingletonViewModel(key) {
        CategoriesViewModel(
            categoryDao = database.get().categoryDao(), wordEntryDao = database.get().wordEntryDao()
        )
    }

@Composable
fun rememberWordsViewModel(key: String = "words"): WordsViewModel =
    rememberSingletonViewModel(key) {
        WordsViewModel(
            wordEntryDao = database.get().wordEntryDao()
        )
    }

@Composable
fun rememberWordStoreViewModel(key: String = "store") : WordsStoreViewModel =
    rememberSingletonViewModel(key) {
        WordsStoreViewModel()
    }

@Composable
fun rememberSearchViewModel(key: String = "search"): SearchViewModel =
    rememberSingletonViewModel(key) {
        SearchViewModel(
            categoryDao = database.get().categoryDao(),
            wordEntryDao = database.get().wordEntryDao()
        )
    }

@Composable
fun rememberTranslationViewModel(key: String = "translation"): TranslationViewModel =
    rememberSingletonViewModel(key) {
        TranslationViewModel()
    }
