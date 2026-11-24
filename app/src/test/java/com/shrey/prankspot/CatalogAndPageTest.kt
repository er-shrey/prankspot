package com.shrey.prankspot

import com.google.common.truth.Truth.assertThat
import com.shrey.prankspot.domain.CatalogParser
import com.shrey.prankspot.domain.SsidRules
import com.shrey.prankspot.portal.PageRenderer
import org.junit.Test
import java.io.File

class CatalogAndPageTest {
    private val catalog = CatalogParser.parse(File("src/main/assets/catalog.json").readText())

    @Test fun catalogMeetsMvp() {
        assertThat(catalog.size).isAtLeast(12)
        catalog.forEach { c ->
            assertThat(c.ssids.size).isAtLeast(20)
            assertThat(c.ssids).containsNoDuplicates()
            assertThat(c.pranks).isNotEmpty()
            c.ssids.forEach { assertThat(SsidRules.validate(it)).isNull() }
        }
        val pranks = catalog.flatMap { it.pranks }
        assertThat(pranks.size).isAtLeast(12)
        assertThat(pranks.map { it.id }).containsNoDuplicates()
    }

    @Test fun everyPageRendersAndSaysItIsAPrank() {
        catalog.flatMap { it.pranks }.forEach { p ->
            val html = PageRenderer.render(p)
            assertThat(html).startsWith("<!doctype html>")
            assertThat(html).contains("harmless PrankSpot prank")
            assertThat(html).doesNotContain("<form")
            assertThat(html).doesNotContain("<input")
            assertThat(html).doesNotContain("http://")
            assertThat(html).doesNotContain("https://")
        }
    }

    @Test fun customTextIsEscaped() {
        val p = catalog.first().pranks.first()
        val html = PageRenderer.render(p, customTitle = "<script>alert(1)</script>", customMessage = "Tom & \"Jerry\"")
        assertThat(html).doesNotContain("<script>alert(1)")
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;")
        assertThat(html).contains("Tom &amp; &quot;Jerry&quot;")
    }

    @Test fun jsStringsCannotCloseTheScriptTag() {
        assertThat(PageRenderer.js("</script><b>")).isEqualTo("\"\\u003c/script\\u003e\\u003cb\\u003e\"")
        assertThat(PageRenderer.css("red;}body{display:none")).isEqualTo("#888888")
        assertThat(PageRenderer.css("#FF00aa")).isEqualTo("#FF00aa")
    }
}
