/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model.besluit

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import nl.info.zac.app.zaak.model.createRestBesluitChangeData

class RestBesluitChangeDataTest : BehaviorSpec({

    Given("Change data with the optional date fields populated") {
        val restBesluitWijzigenGegevens = createRestBesluitChangeData()

        When("converted to a besluit patch") {
            val besluitPatch = restBesluitWijzigenGegevens.toBesluitPatch()

            Then("the patch contains the supplied values and a tijdelijk vervalreden") {
                with(besluitPatch) {
                    toelichting shouldBe restBesluitWijzigenGegevens.toelichting
                    ingangsdatum shouldBe restBesluitWijzigenGegevens.ingangsdatum
                    vervaldatum shouldBe restBesluitWijzigenGegevens.vervaldatum
                    vervalreden shouldBe VervalredenEnum.TIJDELIJK
                    publicatiedatum shouldBe restBesluitWijzigenGegevens.publicationDate
                    uiterlijkeReactiedatum shouldBe restBesluitWijzigenGegevens.lastResponseDate
                }
            }
        }
    }

    Given("Change data that clears the optional date fields") {
        val restBesluitWijzigenGegevens = createRestBesluitChangeData().copy(
            vervaldatum = null,
            publicationDate = null,
            lastResponseDate = null
        )

        When("converted to a besluit patch") {
            val besluitPatch = restBesluitWijzigenGegevens.toBesluitPatch()

            Then("the optional date fields are cleared and the vervalreden is reset to the blank value") {
                with(besluitPatch) {
                    vervaldatum shouldBe null
                    vervalreden shouldBe VervalredenEnum.EMPTY
                    publicatiedatum shouldBe null
                    uiterlijkeReactiedatum shouldBe null
                }
            }
        }
    }
})
