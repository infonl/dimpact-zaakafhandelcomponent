/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.admin.model.ReferenceTableValue
import nl.info.zac.admin.model.ZaaktypeZaakafzenderParameters

class RestReplyToTest : BehaviorSpec({
    context("toRestReplyTo") {
        given("a ReferenceTableValue with a name") {
            val waarde = ReferenceTableValue().apply {
                name = "fakeMail@example.com"
            }

            `when`("it is converted to a reply-to") {
                val result = waarde.toRestReplyTo()

                then("it returns a RestReplyTo with the correct mail and isSpeciaal=false") {
                    result.mail shouldBe "fakeMail@example.com"
                    result.isSpeciaal shouldBe false
                }
            }
        }
    }

    context("toRestReplyTos") {
        given("a list of ReferenceTableValues") {
            val waarden = listOf(
                ReferenceTableValue().apply { name = "fakeMail2@example.com" },
                ReferenceTableValue().apply { name = "fakeMail1@example.com" }
            )

            `when`("they are converted to reply-tos") {
                val result = waarden.toRestReplyTos()

                then("it includes SpecialMail entries sorted before regular entries") {
                    val specialEntries = result.filter { it.isSpeciaal }
                    val regularEntries = result.filter { !it.isSpeciaal }
                    specialEntries.size shouldBe ZaaktypeZaakafzenderParameters.SpecialMail.entries.size
                    regularEntries.size shouldBe 2
                }

                then("special entries appear before regular entries in the result") {
                    result.first().isSpeciaal shouldBe true
                }

                then("regular entries are sorted alphabetically by mail") {
                    val regularEntries = result.filter { !it.isSpeciaal }
                    regularEntries[0].mail shouldBe "fakeMail1@example.com"
                    regularEntries[1].mail shouldBe "fakeMail2@example.com"
                }
            }
        }
    }
})
