/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.net.URI

class MailLinkTest : BehaviorSpec({
    context("Rendering a mail link as HTML") {
        given("a mail link with a prefix and a suffix") {
            val mailLink = MailLink(
                identificatie = "fakeIdentificatie",
                url = URI("https://example.com/zaak"),
                prefix = "de zaak",
                suffix = "bij <b>ons</b>"
            )

            `when`("it is rendered") {
                val html = mailLink.toHtml()

                then("the prefix and suffix are separated by spaces and HTML-escaped") {
                    html shouldBe "Klik om naar de zaak <a href=\"https://example.com/zaak\" " +
                        "title=\"de zaakafhandelcomponent...\">fakeIdentificatie</a> bij &lt;b&gt;ons&lt;/b&gt; te gaan."
                }
            }
        }

        given("a mail link without a prefix and a suffix") {
            val mailLink = MailLink(
                identificatie = "fakeIdentificatie",
                url = URI("https://example.com/zaak"),
                prefix = null,
                suffix = null
            )

            `when`("it is rendered") {
                val html = mailLink.toHtml()

                then("the link is rendered without extra spaces") {
                    html shouldBe "Klik om naar <a href=\"https://example.com/zaak\" " +
                        "title=\"de zaakafhandelcomponent...\">fakeIdentificatie</a> te gaan."
                }
            }
        }
    }
})
