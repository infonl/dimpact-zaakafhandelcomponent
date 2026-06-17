/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model.besluit

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import nl.info.zac.app.zaak.model.createRestBesluitChangeData

class RestBesluitChangeDataTest : BehaviorSpec({

    Given("Besluit") {
        val besluit = createBesluit()
        val restBesluitWijzigenGegevens = createRestBesluitChangeData()

        When("updated with change data") {
            val updatedBesluit = besluit.updateBesluitWithBesluitChangeData(restBesluitWijzigenGegevens)

            Then("update is correct") {
                with(updatedBesluit) {
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
})
