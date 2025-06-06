/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.brc.model.generated.VervalredenEnum

class RestDecisionChangeDataTest : BehaviorSpec({

    Given("Besluit") {
        val besluit = createBesluit()
        val restBesluitWijzigenGegevens = createRestDecisionChangeData()

        When("updated with change data") {
            val updatedBesluit = besluit.updateDecisionWithDecisionChangeData(restBesluitWijzigenGegevens)

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
