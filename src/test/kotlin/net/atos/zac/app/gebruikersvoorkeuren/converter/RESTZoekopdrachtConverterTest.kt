/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.gebruikersvoorkeuren.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.gebruikersvoorkeuren.model.Werklijst
import net.atos.zac.gebruikersvoorkeuren.model.createZoekopdracht
import java.time.ZonedDateTime

class RESTZoekopdrachtConverterTest : BehaviorSpec({
    Context("convert(Zoekopdracht)") {
        Given("a Zoekopdracht domain model") {
            val zoekopdracht = createZoekopdracht(
                id = 10L,
                name = "fakeNaam",
                lijstID = Werklijst.MIJN_ZAKEN,
                medewerkerID = "fakeMedewerker",
                actief = true,
                creationDate = ZonedDateTime.parse("2024-01-15T10:00:00+01:00")
            ).apply { json = """{"fakeField":"fakeValue"}""" }

            When("convert(Zoekopdracht) is called") {
                val result = RESTZoekopdrachtConverter.convert(zoekopdracht)

                Then("it maps all fields correctly") {
                    result.id shouldBe 10L
                    result.naam shouldBe "fakeNaam"
                    result.lijstID shouldBe Werklijst.MIJN_ZAKEN
                    result.actief shouldBe true
                    result.creatiedatum shouldBe zoekopdracht.creatiedatum.toLocalDate()
                    result.json shouldBe """{"fakeField":"fakeValue"}"""
                }
            }
        }
    }

    Context("convert(List<Zoekopdracht>)") {
        Given("a list of Zoekopdrachten") {
            val list = listOf(
                createZoekopdracht(id = 1L, name = "fakeNaam1"),
                createZoekopdracht(id = 2L, name = "fakeNaam2")
            )

            When("convert(List<Zoekopdracht>) is called") {
                val result = RESTZoekopdrachtConverter.convert(list)

                Then("all items are converted") {
                    result.size shouldBe 2
                    result[0].id shouldBe 1L
                    result[0].naam shouldBe "fakeNaam1"
                    result[1].id shouldBe 2L
                    result[1].naam shouldBe "fakeNaam2"
                }
            }
        }
    }
})
