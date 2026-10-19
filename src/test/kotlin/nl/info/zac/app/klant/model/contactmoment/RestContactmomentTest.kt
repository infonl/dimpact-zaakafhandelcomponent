/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.contactmoment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.klant.createActor
import nl.info.client.klant.createActoridentificator
import nl.info.client.klant.createKlantcontact

class RestContactmomentTest : BehaviorSpec({
    context("Converting a klantcontact to a REST contactmoment") {
        given("a klantcontact with an actor with a name and an actor with a blank name") {
            val klantcontact = createKlantcontact(
                hadBetrokkenActoren = listOf(
                    createActor(naam = "fakeActorNaam"),
                    createActor(
                        naam = " ",
                        actoridentificator = createActoridentificator(
                            codeObjecttype = "fakeCodeObjecttype",
                            codeRegister = "fakeCodeRegister",
                            objectId = "fakeObjectId"
                        )
                    )
                )
            )

            `when`("it is converted") {
                val restContactmoment = klantcontact.toRestContactMoment(emptyMap())

                then("the medewerker lists the actors separated by commas, with the identificator for the actor without a name") {
                    restContactmoment.medewerker shouldBe "fakeActorNaam,fakeCodeObjecttype fakeCodeRegister %fakeObjectId"
                }
            }
        }

        given("a klantcontact without actors") {
            val klantcontact = createKlantcontact(hadBetrokkenActoren = emptyList())

            `when`("it is converted") {
                val restContactmoment = klantcontact.toRestContactMoment(emptyMap())

                then("there is no medewerker") {
                    restContactmoment.medewerker shouldBe null
                }
            }
        }
    }
})
