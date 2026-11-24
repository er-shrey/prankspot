package com.shrey.prankspot.domain

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/** Hands out funny names for one category, never repeating until the pool runs out. */
class SsidGenerator(private val random: Random = Random.Default) {
    private val shown = mutableMapOf<String, MutableSet<String>>()

    fun suggestions(category: Category, count: Int = 6): List<String> {
        val seen = shown.getOrPut(category.id) { mutableSetOf() }
        var pool = category.ssids.filterNot { it in seen }
        if (pool.size < count) {
            // Pool exhausted for this session: start over, but keep the new batch unique.
            seen.clear()
            pool = category.ssids
        }
        val picked = pool.shuffled(random).take(count)
        seen += picked
        return picked
    }
}

object SsidRules {
    const val MAX_BYTES = 32

    /** Null when valid, otherwise a short reason. */
    fun validate(ssid: String): String? = when {
        ssid.isBlank() -> "Give your network a name"
        ssid != ssid.trim() -> "Remove spaces at the start or end"
        ssid.any { it.isISOControl() } -> "Name has an invisible control character"
        ssid.toByteArray(Charsets.UTF_8).size > MAX_BYTES ->
            "Too long: Wi-Fi names fit 32 bytes (emoji count as 4)"
        else -> null
    }

    /** Emoji and other non-ASCII names work on most phones but show as gibberish on some. */
    fun hasCompatibilityRisk(ssid: String): Boolean = ssid.any { it.code > 0x7E }
}

object PasswordRules {
    fun validate(password: String, security: Security): String? = when (security) {
        Security.OPEN -> null
        Security.WPA2 -> when {
            password.length < 8 -> "At least 8 characters"
            password.length > 63 -> "At most 63 characters"
            password.any { it.code !in 0x20..0x7E } -> "Use plain letters, digits and symbols only"
            else -> null
        }
    }
}

/** Easy-to-say passwords like "WobblyPickle47", so you can read one out loud. */
class PasswordGenerator(private val random: Random = SecureRandom().asKotlinRandom()) {
    private val adjectives = listOf(
        "Wobbly", "Sneaky", "Fluffy", "Grumpy", "Turbo", "Sparkly", "Cosmic", "Soggy", "Mighty", "Giggly",
        "Spicy", "Sleepy", "Bouncy", "Crunchy", "Fancy", "Jolly", "Nifty", "Zippy", "Wacky", "Funky",
    )
    private val nouns = listOf(
        "Pickle", "Penguin", "Noodle", "Waffle", "Llama", "Potato", "Muffin", "Taco", "Walrus", "Banana",
        "Goblin", "Pancake", "Hamster", "Donut", "Panda", "Nugget", "Robot", "Unicorn", "Cactus", "Yeti",
    )

    fun generate(): String =
        adjectives.random(random) + nouns.random(random) + random.nextInt(10, 100)
}

object WifiQr {
    /** The ZXing/Android "WIFI:" join format; a phone camera scan offers to join. */
    fun joinPayload(ssid: String, password: String?, security: Security): String {
        val type = if (security == Security.OPEN || password.isNullOrEmpty()) "nopass" else "WPA"
        val pass = if (type == "nopass") "" else "P:${escape(password!!)};"
        return "WIFI:T:$type;S:${escape(ssid)};$pass;"
    }

    private fun escape(s: String) = buildString {
        for (c in s) {
            if (c in "\\;,:\"") append('\\')
            append(c)
        }
    }
}
