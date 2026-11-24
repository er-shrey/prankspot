package com.shrey.prankspot.domain

import org.json.JSONArray
import org.json.JSONObject

object CatalogParser {
    fun parse(json: String): List<Category> {
        val cats = JSONObject(json).getJSONArray("categories")
        return (0 until cats.length()).map { i ->
            val c = cats.getJSONObject(i)
            val id = c.getString("id")
            val pranks = c.getJSONArray("pranks")
            Category(
                id = id,
                name = c.getString("name"),
                emoji = c.getString("emoji"),
                description = c.optString("description"),
                ssids = c.getJSONArray("ssids").strings(),
                pranks = (0 until pranks.length()).map { j -> prank(id, pranks.getJSONObject(j)) },
            )
        }
    }

    private fun prank(categoryId: String, p: JSONObject): PrankTemplate {
        val bg = p.getJSONArray("bg")
        return PrankTemplate(
            id = p.getString("id"),
            categoryId = categoryId,
            name = p.getString("name"),
            emoji = p.optString("emoji", "😄"),
            title = p.getString("title"),
            message = p.getString("message"),
            extras = p.optJSONArray("extras")?.strings().orEmpty(),
            button = p.optString("button", "Continue"),
            reveal = p.optString("reveal", "It's just a prank!"),
            bgStart = bg.getString(0),
            bgEnd = bg.getString(bg.length() - 1),
            textColor = p.optString("text", "#FFFFFF"),
            accent = p.optString("accent", "#FFD54F"),
            animation = p.optString("animation", "loading"),
            sound = p.optString("sound", "chime"),
        )
    }

    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
}
