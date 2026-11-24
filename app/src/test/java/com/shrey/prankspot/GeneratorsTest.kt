package com.shrey.prankspot

import com.google.common.truth.Truth.assertThat
import com.shrey.prankspot.domain.Category
import com.shrey.prankspot.domain.PasswordGenerator
import com.shrey.prankspot.domain.PasswordRules
import com.shrey.prankspot.domain.Security
import com.shrey.prankspot.domain.SsidGenerator
import com.shrey.prankspot.domain.SsidRules
import com.shrey.prankspot.domain.WifiQr
import org.junit.Test
import kotlin.random.Random

class GeneratorsTest {
    private val cat = Category("t", "T", "x", "", (1..20).map { "Name $it" }, emptyList())

    @Test fun suggestionsDontRepeatUntilPoolRunsOut() {
        val g = SsidGenerator(Random(1))
        val first = (0 until 3).flatMap { g.suggestions(cat, 6) }
        assertThat(first).containsNoDuplicates()
        assertThat(first).hasSize(18)
        val next = g.suggestions(cat, 6)
        assertThat(next).containsNoDuplicates()
        assertThat(next).hasSize(6)
    }

    @Test fun ssidRules() {
        assertThat(SsidRules.validate("Flight 404")).isNull()
        assertThat(SsidRules.validate("")).isNotNull()
        assertThat(SsidRules.validate(" padded")).isNotNull()
        assertThat(SsidRules.validate("a".repeat(32))).isNull()
        assertThat(SsidRules.validate("a".repeat(33))).isNotNull()
        // 8 emoji × 4 bytes = 32 bytes: fits; one more doesn't.
        assertThat(SsidRules.validate("🎂".repeat(8))).isNull()
        assertThat(SsidRules.validate("🎂".repeat(8) + "a")).isNotNull()
        assertThat(SsidRules.hasCompatibilityRisk("Cake 🎂")).isTrue()
        assertThat(SsidRules.hasCompatibilityRisk("Cake")).isFalse()
    }

    @Test fun generatedPasswordsAreValidWpa2() {
        val g = PasswordGenerator(Random(7))
        repeat(200) {
            val p = g.generate()
            assertThat(PasswordRules.validate(p, Security.WPA2)).isNull()
        }
    }

    @Test fun passwordRules() {
        assertThat(PasswordRules.validate("short", Security.WPA2)).isNotNull()
        assertThat(PasswordRules.validate("x".repeat(64), Security.WPA2)).isNotNull()
        assertThat(PasswordRules.validate("pässwörd1", Security.WPA2)).isNotNull()
        assertThat(PasswordRules.validate("", Security.OPEN)).isNull()
    }

    @Test fun wifiQrEscapesSpecialCharacters() {
        assertThat(WifiQr.joinPayload("A;B", "p:w\"d", Security.WPA2)).isEqualTo("WIFI:T:WPA;S:A\\;B;P:p\\:w\\\"d;;")
        assertThat(WifiQr.joinPayload("Open", null, Security.OPEN)).isEqualTo("WIFI:T:nopass;S:Open;;")
    }
}
