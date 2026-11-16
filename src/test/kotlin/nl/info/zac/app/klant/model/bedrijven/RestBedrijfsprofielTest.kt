/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import nl.info.client.kvk.model.createBasisprofiel
import nl.info.client.kvk.model.createBasisprofielAdres
import nl.info.client.kvk.model.createBasisprofielHoofdvestiging
import nl.info.client.kvk.model.createEigenaar
import nl.info.client.kvk.model.createVestiging

class RestBedrijfsprofielTest : BehaviorSpec({

    given("a basisprofiel of which the eigenaar has addresses and the hoofdvestiging has other addresses") {
        val basisprofiel = createBasisprofiel(
            eigenaar = createEigenaar(
                adressen = listOf(createBasisprofielAdres(type = "bezoekadres", volledigAdres = "fakeEigenaarAdres"))
            ),
            hoofdvestiging = createBasisprofielHoofdvestiging(
                adressen = listOf(
                    createBasisprofielAdres(type = "bezoekadres", volledigAdres = "fakeHoofdvestigingAdres")
                )
            )
        )

        `when`("converted to RestBedrijfsprofiel") {
            val restBedrijfsprofiel = basisprofiel.toRestBedrijfsprofiel()

            then("the addresses are those of the eigenaar") {
                restBedrijfsprofiel.adressen!! shouldHaveSize 1
                restBedrijfsprofiel.adressen!!.first().volledigAdres shouldBe "fakeEigenaarAdres"
            }
        }
    }

    given("a basisprofiel of which only the hoofdvestiging has addresses, as KVK returns for a rechtspersoon with vestigingen") {
        val basisprofiel = createBasisprofiel(
            eigenaar = createEigenaar(adressen = null),
            hoofdvestiging = createBasisprofielHoofdvestiging(
                adressen = listOf(
                    createBasisprofielAdres(type = "bezoekadres", volledigAdres = "fakeHoofdvestigingAdres")
                )
            )
        )

        `when`("converted to RestBedrijfsprofiel") {
            val restBedrijfsprofiel = basisprofiel.toRestBedrijfsprofiel()

            then("the addresses are those of the hoofdvestiging") {
                restBedrijfsprofiel.adressen!! shouldHaveSize 1
                restBedrijfsprofiel.adressen!!.first().volledigAdres shouldBe "fakeHoofdvestigingAdres"
            }
        }
    }

    given("a vestiging with websites") {
        val vestiging = createVestiging().apply {
            websites = listOf("fakeWebsite1", "fakeWebsite2")
        }

        `when`("converted to RestBedrijfsprofiel") {
            val restBedrijfsprofiel = vestiging.toRestBedrijfsprofiel()

            then("the website is the first website of the vestiging") {
                restBedrijfsprofiel.website shouldBe "fakeWebsite1"
            }
        }
    }

    given("a vestiging with an empty list of websites") {
        val vestiging = createVestiging().apply {
            websites = emptyList()
        }

        `when`("converted to RestBedrijfsprofiel") {
            val restBedrijfsprofiel = vestiging.toRestBedrijfsprofiel()

            then("the profiel has no website") {
                restBedrijfsprofiel.website shouldBe null
            }
        }
    }
})
