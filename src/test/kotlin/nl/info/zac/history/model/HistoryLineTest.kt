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

    given("a HistoryLine instance") {
        `when`("it is serialized to JSON") {
            val json = JsonbBuilder.create().toJson(HistoryLine("label", "old", "new"))

            then("the wire format keeps the original oudeWaarde/nieuweWaarde field names") {
                json shouldContain "\"oudeWaarde\":\"old\""
                json shouldContain "\"nieuweWaarde\":\"new\""
                json shouldNotContain "oldValue"
                json shouldNotContain "newValue"
            }
        }
    }

    given("a LocalDate") {
        `when`("toValue is called") {
            val value = LocalDate.of(2026, 7, 14).toValue()

            then("the result is formatted as dd-MM-yyyy") {
                value shouldBe "14-07-2026"
            }
        }
    }

    given("a ZonedDateTime") {
        `when`("toValue is called") {
            val value = ZonedDateTime.of(2026, 7, 14, 9, 30, 0, 0, ZoneId.systemDefault()).toValue()

            then("the result is formatted as dd-MM-yyyy HH:mm") {
                value shouldBe "14-07-2026 09:30"
            }
        }
    }

    given("a StatusEnum") {
        `when`("toValue is called") {
            val value = StatusEnum.DEFINITIEF.toValue()

            then("the result is the enum's toString") {
                value shouldBe StatusEnum.DEFINITIEF.toString()
            }
        }
    }

    given("a VertrouwelijkheidaanduidingEnum") {
        `when`("toValue is called") {
            val value = VertrouwelijkheidaanduidingEnum.OPENBAAR.toValue()

            then("the result is the enum's toString") {
                value shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR.toString()
            }
        }
    }

    given("a Boolean") {
        `when`("toValue is called with true") {
            val value = true.toValue()

            then("the result is 'Ja'") {
                value shouldBe "Ja"
            }
        }

        `when`("toValue is called with false") {
            val value = false.toValue()

            then("the result is 'Nee'") {
                value shouldBe "Nee"
            }
        }
    }
})
