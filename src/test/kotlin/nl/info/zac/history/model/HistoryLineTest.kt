/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.checkUnnecessaryStub
import jakarta.json.bind.JsonbBuilder
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HistoryLineTest : BehaviorSpec({
    afterEach {
        checkUnnecessaryStub()
    }

    Given("a HistoryLine instance") {
        When("it is serialized to JSON") {
            val json = JsonbBuilder.create().toJson(HistoryLine("label", "old", "new"))

            Then("the wire format keeps the original oudeWaarde/nieuweWaarde field names") {
                json shouldContain "\"oudeWaarde\":\"old\""
                json shouldContain "\"nieuweWaarde\":\"new\""
                json shouldNotContain "oldValue"
                json shouldNotContain "newValue"
            }
        }
    }

    Given("a LocalDate") {
        When("toValue is called") {
            val value = LocalDate.of(2026, 7, 14).toValue()

            Then("the result is formatted as dd-MM-yyyy") {
                value shouldBe "14-07-2026"
            }
        }
    }

    Given("a ZonedDateTime") {
        When("toValue is called") {
            val value = ZonedDateTime.of(2026, 7, 14, 9, 30, 0, 0, ZoneId.systemDefault()).toValue()

            Then("the result is formatted as dd-MM-yyyy HH:mm") {
                value shouldBe "14-07-2026 09:30"
            }
        }
    }

    Given("a StatusEnum") {
        When("toValue is called") {
            val value = StatusEnum.DEFINITIEF.toValue()

            Then("the result is the enum's toString") {
                value shouldBe StatusEnum.DEFINITIEF.toString()
            }
        }
    }

    Given("a VertrouwelijkheidaanduidingEnum") {
        When("toValue is called") {
            val value = VertrouwelijkheidaanduidingEnum.OPENBAAR.toValue()

            Then("the result is the enum's toString") {
                value shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR.toString()
            }
        }
    }

    Given("a Boolean") {
        When("toValue is called with true") {
            val value = true.toValue()

            Then("the result is 'Ja'") {
                value shouldBe "Ja"
            }
        }

        When("toValue is called with false") {
            val value = false.toValue()

            Then("the result is 'Nee'") {
                value shouldBe "Nee"
            }
        }
    }
})
