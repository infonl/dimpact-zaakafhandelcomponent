/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.admin.model.ReferenceTableValue
import nl.info.zac.admin.model.ZaaktypeCmmnZaakafzenderParameters

class RESTReplyToConverterTest : BehaviorSpec({
    Context("convertReplyTo") {
        Given("a ReferenceTableValue with a name") {
            val waarde = ReferenceTableValue().apply {
                name = "fakeMail@example.com"
            }

            When("convertReplyTo is called") {
                val result = RESTReplyToConverter.convertReplyTo(waarde)

                Then("it returns a RESTReplyTo with the correct mail and speciaal=false") {
                    result.mail shouldBe "fakeMail@example.com"
                    result.speciaal shouldBe false
                }
            }
        }
    }

    Context("convertReplyTos") {
        Given("a list of ReferenceTableValues") {
            val waarden = listOf(
                ReferenceTableValue().apply { name = "fakeMail2@example.com" },
                ReferenceTableValue().apply { name = "fakeMail1@example.com" }
            )

            When("convertReplyTos is called") {
                val result = RESTReplyToConverter.convertReplyTos(waarden)

                Then("it includes SpecialMail entries sorted before regular entries") {
                    val specialEntries = result.filter { it.speciaal }
                    val regularEntries = result.filter { !it.speciaal }
                    specialEntries.size shouldBe ZaaktypeCmmnZaakafzenderParameters.SpecialMail.entries.size
                    regularEntries.size shouldBe 2
                }

                Then("special entries appear before regular entries in the result") {
                    result.first().speciaal shouldBe true
                }

                Then("regular entries are sorted alphabetically by mail") {
                    val regularEntries = result.filter { !it.speciaal }
                    regularEntries[0].mail shouldBe "fakeMail1@example.com"
                    regularEntries[1].mail shouldBe "fakeMail2@example.com"
                }
            }
        }
    }
})
