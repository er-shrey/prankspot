package com.shrey.prankspot.data

import android.content.Context
import com.shrey.prankspot.domain.CatalogParser
import com.shrey.prankspot.domain.Category
import com.shrey.prankspot.domain.PrankTemplate

/** Categories, names and pranks ship inside the APK, so they work fully offline. */
class CatalogRepository(context: Context) {
    val categories: List<Category> by lazy {
        CatalogParser.parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
    }

    val pranks: List<PrankTemplate> by lazy { categories.flatMap { it.pranks } }

    fun category(id: String): Category = categories.firstOrNull { it.id == id } ?: categories.last()

    fun prank(id: String): PrankTemplate? = pranks.firstOrNull { it.id == id }

    fun defaultPrankFor(categoryId: String): PrankTemplate = category(categoryId).pranks.first()
}
