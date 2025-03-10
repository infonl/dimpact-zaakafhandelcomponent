/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.client.zgw.model.createResultaat
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBrondatumArchiefprocedure
import nl.info.client.zgw.ztc.model.createResultaatType
import java.net.URI
import java.util.UUID

class RestZaakResultaatConverterTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val converter = RestZaakResultaatConverter(zrcClientService, ztcClientService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given(
        """
        A valid resultaatURI which corresponds to a resultaat and a resultaattype with 
        a 'brondatumArchiefprocedure' with an 'afleidingswijze' of type 'VERVALDATUM_BESLUIT'
        """
    ) {
        val resultaatTypeUUID = UUID.randomUUID()
        val resultaatURI = URI("http://example.com/resultaat/${UUID.randomUUID()}")
        val resultaatTypeURI = URI("http://example.com/resultaattype/$resultaatTypeUUID")
        val resultaat = createResultaat(url = resultaatURI, resultaatTypeURI = resultaatTypeURI)
        val resultaattype = createResultaatType(
            url = resultaatTypeURI,
            brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = AfleidingswijzeEnum.VERVALDATUM_BESLUIT
            )
        )
        every { zrcClientService.readResultaat(resultaatURI) } returns resultaat
        every { ztcClientService.readResultaattype(resultaatTypeURI) } returns resultaattype

        When("it is converted to a REST zaak resultaat") {
            val restZaakResultaat = converter.convert(resultaatURI)

            Then("it should return a RestZaakResultaat with 'besluitVerplicht' set to true") {
                with(restZaakResultaat) {
                    toelichting shouldBe resultaattype.toelichting
                    with(this.resultaattype!!) {
                        id shouldBe resultaatTypeUUID
                        naam shouldBe resultaattype.omschrijving
                        naamGeneriek shouldBe resultaattype.omschrijvingGeneriek
                        toelichting shouldBe resultaattype.toelichting
                        archiefNominatie shouldBe resultaattype.archiefnominatie.name
                        archiefTermijn shouldBe resultaattype.archiefactietermijn
                        besluitVerplicht shouldBe true
                        vervaldatumBesluitVerplicht shouldBe true
                    }
                }
            }
        }
    }
    Given(
        """
        A valid resultaatURI which corresponds to a resultaat and a resultaattype with 
        a 'brondatumArchiefprocedure' with an 'afleidingswijze' not of type 'VERVALDATUM_BESLUIT' or 'INGANGSDATUM_BESLUIT'
        """
    ) {
        val resultaatTypeUUID = UUID.randomUUID()
        val resultaatURI = URI("http://example.com/resultaat/${UUID.randomUUID()}")
        val resultaatTypeURI = URI("http://example.com/resultaattype/$resultaatTypeUUID")
        val resultaat = createResultaat(url = resultaatURI, resultaatTypeURI = resultaatTypeURI)
        val resultaattype = createResultaatType(
            url = resultaatTypeURI,
            brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = AfleidingswijzeEnum.GERELATEERDE_ZAAK
            )
        )
        every { zrcClientService.readResultaat(resultaatURI) } returns resultaat
        every { ztcClientService.readResultaattype(resultaatTypeURI) } returns resultaattype

        When("it is converted to a REST zaak resultaat") {
            val restZaakResultaat = converter.convert(resultaatURI)

            Then("it should return a RestZaakResultaat with 'besluitVerplicht' set to false") {
                with(restZaakResultaat.resultaattype!!) {
                    besluitVerplicht shouldBe false
                    vervaldatumBesluitVerplicht shouldBe false
                }
            }
        }
    }
    Given(
        """
        A valid resultaatURI which corresponds to a resultaat and a resultaattype with 
        a 'brondatumArchiefprocedure' that does not have an 'afleidingswijze'
        """
    ) {
        val resultaatTypeUUID = UUID.randomUUID()
        val resultaatURI = URI("http://example.com/resultaat/${UUID.randomUUID()}")
        val resultaatTypeURI = URI("http://example.com/resultaattype/$resultaatTypeUUID")
        val resultaat = createResultaat(url = resultaatURI, resultaatTypeURI = resultaatTypeURI)
        val resultaattype = createResultaatType(
            url = resultaatTypeURI,
            brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = null
            )
        )
        every { zrcClientService.readResultaat(resultaatURI) } returns resultaat
        every { ztcClientService.readResultaattype(resultaatTypeURI) } returns resultaattype

        When("it is converted to a REST zaak resultaat") {
            val restZaakResultaat = converter.convert(resultaatURI)

            Then("it should return a RestZaakResultaat with 'besluitVerplicht' set to false") {
                with(restZaakResultaat.resultaattype!!) {
                    besluitVerplicht shouldBe false
                    vervaldatumBesluitVerplicht shouldBe false
                }
            }
        }
    }
})
