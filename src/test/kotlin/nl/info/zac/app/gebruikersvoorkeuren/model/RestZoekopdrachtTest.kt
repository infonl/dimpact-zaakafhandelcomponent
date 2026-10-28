/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.gebruikersvoorkeuren.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.gebruikersvoorkeuren.model.Werklijst
import nl.info.zac.gebruikersvoorkeuren.model.createZoekopdracht
import java.time.ZonedDateTime

class RestZoekopdrachtTest : BehaviorSpec({
    context("toRestZoekopdracht") {
        given("a Zoekopdracht domain model") {
            val zoekopdracht = createZoekopdracht(
                id = 10L,
                name = "fakeNaam",
                lijstID = Werklijst.MIJN_ZAKEN,
                medewerkerID = "fakeMedewerker",
                actief = true,
                creationDate = ZonedDateTime.parse("2024-01-15T10:00:00+01:00"),
                json = """{"fakeField":"fakeValue"}"""
            )

            `when`("it is converted to a REST model") {
                val restZoekopdracht = zoekopdracht.toRestZoekopdracht()

                then("it maps all fields correctly") {
                    with(restZoekopdracht) {
                        id shouldBe 10L
                        naam shouldBe "fakeNaam"
                        lijstID shouldBe Werklijst.MIJN_ZAKEN
                        isActief shouldBe true
                        creatiedatum shouldBe zoekopdracht.creatiedatum.toLocalDate()
                        json shouldBe """{"fakeField":"fakeValue"}"""
                    }
                }
            }
        }
    }

    context("toZoekopdracht") {
        given("a RestZoekopdracht and a logged-in user") {
            val restZoekopdracht = RestZoekopdracht(
                id = 3L,
                lijstID = Werklijst.MIJN_TAKEN,
                isActief = true,
                naam = "fakeNaam",
                json = """{"fakeField":"fakeValue"}"""
            )
            val loggedInUser = createLoggedInUser(username = "fakeMedewerker")
            val timeBeforeConversion = ZonedDateTime.now().minusSeconds(1)

            `when`("it is converted to a domain model") {
                val zoekopdracht = restZoekopdracht.toZoekopdracht(loggedInUser)

                then("it maps all fields, takes the medewerker from the logged-in user and sets the current time") {
                    with(zoekopdracht) {
                        id shouldBe 3L
                        lijstID shouldBe Werklijst.MIJN_TAKEN
                        medewerkerID shouldBe "fakeMedewerker"
                        naam shouldBe "fakeNaam"
                        isActief shouldBe true
                        json shouldBe """{"fakeField":"fakeValue"}"""
                        creatiedatum shouldBeAfter timeBeforeConversion
                    }
                }
            }
        }
    }
})
