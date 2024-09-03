/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createResultaatType
import net.atos.client.zgw.ztc.model.generated.ArchiefnominatieEnum
import java.net.URI
import java.util.UUID

class RESTResultaattypeConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val restResultaattypeConverter = RestResultaattypeConverter(ztcClientService)

    Given("A resultaattype without archiefactietermijn") {
        val resultaattypeUUID = UUID.randomUUID()
        val resultaatType = createResultaatType(
            url = URI("http://example.com/resultaattypes/$resultaattypeUUID"),
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            archiefactietermijn = null
        )
        When("the resultaattype is converted") {
            val restResultaatType = restResultaattypeConverter.convertResultaattype(resultaatType)

            Then("the resultaattype is converted correctly") {
                with(restResultaatType) {
                    id shouldBe resultaattypeUUID
                    naam shouldBe resultaatType.omschrijving
                    toelichting shouldBe resultaatType.toelichting
                    archiefNominatie shouldBe resultaatType.archiefnominatie.name
                    archiefTermijn shouldBe null
                    besluitVerplicht shouldBe false
                    naamGeneriek shouldBe resultaatType.omschrijvingGeneriek
                    vervaldatumBesluitVerplicht shouldBe false
                }
            }
        }
    }
})
