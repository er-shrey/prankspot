package com.shrey.prankspot.domain

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/** Hands out funny names for one category, never repeating until the pool runs out. */
class SsidGenerator(private val random: Random = Random.Default) {
    private val shown = mutableMapOf<String, MutableSet<String>>()

    fun suggestions(category: Category, count: Int = 6, fits: (String) -> Boolean = { true }): List<String> {
        val seen = shown.getOrPut(category.id) { mutableSetOf() }
        val all = category.ssids.filter(fits).ifEmpty { category.ssids }
        var pool = all.filterNot { it in seen }
        if (pool.size < count) {
            // Pool exhausted for this session: start over, but keep the new batch unique.
            seen.removeAll(all.toSet())
            pool = all
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

/**
 * Wi-Fi Direct networks must be named "DIRECT-xy…" (x, y letters or digits). We use
 * "DIRECT-xy " + the funny name, so guests see e.g. "DIRECT-FB FBI Surveillance Van".
 */
object DirectName {
    const val PREFIX_BYTES = 10 // "DIRECT-xy "
    const val MAX_NAME_BYTES = SsidRules.MAX_BYTES - PREFIX_BYTES

    fun full(name: String): String = "DIRECT-${tag(name)} $name"

    /** Two letters from the name's first words, so the prefix looks deliberate. */
    fun tag(name: String): String {
        val initials = name.split(' ', '-', '_').mapNotNull { w -> w.firstOrNull { it.isLetterOrDigit() && it.code < 0x80 } }
        val letters = (initials + name.filter { it.isLetterOrDigit() && it.code < 0x80 }.toList()).take(2)
        return letters.joinToString("").uppercase().padEnd(2, 'X')
    }

    fun validate(name: String): String? = SsidRules.validate(name) ?: when {
        name.toByteArray(Charsets.UTF_8).size > MAX_NAME_BYTES ->
            "Too long for this mode: $MAX_NAME_BYTES bytes max after the DIRECT-xy prefix"
        else -> null
    }

    fun fits(name: String) = validate(name) == null
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
