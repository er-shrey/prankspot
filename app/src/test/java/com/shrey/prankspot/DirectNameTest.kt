package com.shrey.prankspot

import com.google.common.truth.Truth.assertThat
import com.shrey.prankspot.domain.Category
import com.shrey.prankspot.domain.DirectName
import com.shrey.prankspot.domain.SsidGenerator
import com.shrey.prankspot.domain.SsidRules
import org.junit.Test
import kotlin.random.Random

class DirectNameTest {
    private val androidRule = Regex("^DIRECT-[a-zA-Z0-9]{2}.*")

    @Test fun fullNameMatchesAndroidsRuleAndFits() {
        listOf("FBI Surveillance Van", "Flight 404", "🎂 Cake", "x", "--- !!!").forEach { n ->
            val full = DirectName.full(n)
            assertThat(full).matches(androidRule.pattern)
            if (DirectName.fits(n)) assertThat(SsidRules.validate(full)).isNull()
        }
        assertThat(DirectName.full("FBI Surveillance Van")).isEqualTo("DIRECT-FS FBI Surveillance Van")
        assertThat(DirectName.full("Flight 404")).isEqualTo("DIRECT-F4 Flight 404")
        assertThat(DirectName.tag("!!")).isEqualTo("XX")
    }

    @Test fun lengthLimitLeavesRoomForPrefix() {
        assertThat(DirectName.validate("a".repeat(22))).isNull()
        assertThat(DirectName.validate("a".repeat(23))).isNotNull()
    }

    @Test fun suggestionsOnlyOfferNamesThatFit() {
        val cat = Category("t", "T", "x", "", listOf("Short", "Tiny", "This name is far too long for direct") + (1..10).map { "Name $it" }, emptyList())
        val g = SsidGenerator(Random(3))
        repeat(5) { g.suggestions(cat, 6, DirectName::fits).forEach { assertThat(DirectName.fits(it)).isTrue() } }
    }
}
