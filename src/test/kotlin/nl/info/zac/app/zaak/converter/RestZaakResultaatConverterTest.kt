/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.model.createResultaat
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBrondatumArchiefprocedure
import nl.info.client.zgw.ztc.model.createEigenschap
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import java.net.URI
import java.util.UUID

class RestZaakResultaatConverterTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val converter = RestZaakResultaatConverter(zrcClientService, ztcClientService)

    val zaaktypeURI = URI("https://example.com/zaaktype/${UUID.randomUUID()}")

    afterEach {
        checkUnnecessaryStub()
    }

    given(
        """
        A valid resultaatURI which corresponds to a resultaat and a resultaattype with
        a 'brondatumArchiefprocedure' with an 'afleidingswijze' of type 'VERVALDATUM_BESLUIT'
        """
    ) {
        val resultaatTypeUUID = UUID.randomUUID()
        val resultaatURI = URI("https://example.com/resultaat/${UUID.randomUUID()}")
        val resultaatTypeURI = URI("https://example.com/resultaattype/$resultaatTypeUUID")
        val resultaat = createResultaat(url = resultaatURI, resultaatTypeURI = resultaatTypeURI)
        val resultaattype = createResultaatType(
            url = resultaatTypeURI,
            brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = AfleidingswijzeEnum.VERVALDATUM_BESLUIT
            )
        )
        every { zrcClientService.readResultaat(resultaatURI) } returns resultaat
        every { ztcClientService.readResultaattype(resultaatTypeURI) } returns resultaattype

        `when`("it is converted to a REST zaak resultaat") {
            val restZaakResultaat = converter.convert(resultaatURI = resultaatURI, zaaktypeURI = zaaktypeURI)

            then("it should return a RestZaakResultaat with 'besluitVerplicht' set to true") {
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
                        datumKenmerkOmschrijving shouldBe null
                    }
                }
            }
        }
    }
    given(
        """
        A valid resultaatURI which corresponds to a resultaat and a resultaattype with
        a 'brondatumArchiefprocedure' with an 'afleidingswijze' not of type 'VERVALDATUM_BESLUIT' or 'INGANGSDATUM_BESLUIT'
        """
    ) {
        val resultaatTypeUUID = UUID.randomUUID()
        val resultaatURI = URI("https://example.com/resultaat/${UUID.randomUUID()}")
        val resultaatTypeURI = URI("https://example.com/resultaattype/$resultaatTypeUUID")
        val resultaat = createResultaat(url = resultaatURI, resultaatTypeURI = resultaatTypeURI)
        val resultaattype = createResultaatType(
            url = resultaatTypeURI,
            brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = AfleidingswijzeEnum.GERELATEERDE_ZAAK
            )
        )
        every { zrcClientService.readResultaat(resultaatURI) } returns resultaat
        every { ztcClientService.readResultaattype(resultaatTypeURI) } returns resultaattype

        `when`("it is converted to a REST zaak resultaat") {
            val restZaakResultaat = converter.convert(resultaatURI = resultaatURI, zaaktypeURI = zaaktypeURI)

            then("it should return a RestZaakResultaat with 'besluitVerplicht' set to false") {
                with(restZaakResultaat.resultaattype!!) {
                    besluitVerplicht shouldBe false
                    vervaldatumBesluitVerplicht shouldBe false
                }
            }
        }
    }
    given(
        """
        A valid resultaatURI which corresponds to a resultaat and a resultaattype with
        a 'brondatumArchiefprocedure' that does not have an 'afleidingswijze'
        """
    ) {
        val resultaatTypeUUID = UUID.randomUUID()
        val resultaatURI = URI("https://example.com/resultaat/${UUID.randomUUID()}")
        val resultaatTypeURI = URI("https://example.com/resultaattype/$resultaatTypeUUID")
        val resultaat = createResultaat(url = resultaatURI, resultaatTypeURI = resultaatTypeURI)
        val resultaattype = createResultaatType(
            url = resultaatTypeURI,
            brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = null
            )
        )
        every { zrcClientService.readResultaat(resultaatURI) } returns resultaat
        every { ztcClientService.readResultaattype(resultaatTypeURI) } returns resultaattype

        `when`("it is converted to a REST zaak resultaat") {
            val restZaakResultaat = converter.convert(resultaatURI = resultaatURI, zaaktypeURI = zaaktypeURI)

            then("it should return a RestZaakResultaat with 'besluitVerplicht' set to false") {
                with(restZaakResultaat.resultaattype!!) {
                    besluitVerplicht shouldBe false
                    vervaldatumBesluitVerplicht shouldBe false
                }
            }
        }
    }
    given(
        """
        A valid resultaatURI which corresponds to a resultaat and a resultaattype with
        a 'brondatumArchiefprocedure' with an 'afleidingswijze' of type 'EIGENSCHAP'
        matching one of the zaaktype's eigenschappen
        """
    ) {
        val resultaatTypeUUID = UUID.randomUUID()
        val resultaatURI = URI("https://example.com/resultaat/${UUID.randomUUID()}")
        val resultaatTypeURI = URI("https://example.com/resultaattype/$resultaatTypeUUID")
        val resultaat = createResultaat(url = resultaatURI, resultaatTypeURI = resultaatTypeURI)
        val resultaattype = createResultaatType(
            url = resultaatTypeURI,
            brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP,
                datumkenmerk = "fakeDatumkenmerk"
            )
        )
        val matchingEigenschap = createEigenschap(
            naam = "fakeDatumkenmerk",
            definitie = "fakeEigenschapDefinitie",
            zaaktype = zaaktypeURI
        )
        every { zrcClientService.readResultaat(resultaatURI) } returns resultaat
        every { ztcClientService.readResultaattype(resultaatTypeURI) } returns resultaattype
        every { ztcClientService.readEigenschappen(zaaktypeURI) } returns listOf(matchingEigenschap)

        `when`("it is converted to a REST zaak resultaat") {
            val restZaakResultaat = converter.convert(resultaatURI = resultaatURI, zaaktypeURI = zaaktypeURI)

            then("the datumKenmerkOmschrijving is taken from the matching eigenschap's definitie") {
                restZaakResultaat.resultaattype?.datumKenmerkOmschrijving shouldBe "fakeEigenschapDefinitie"
            }
        }
    }
    given(
        """
        A valid resultaatURI which corresponds to a resultaat and a resultaattype with
        a 'brondatumArchiefprocedure' with an 'afleidingswijze' of type 'EIGENSCHAP'
        not matching any of the zaaktype's eigenschappen
        """
    ) {
        val resultaatTypeUUID = UUID.randomUUID()
        val resultaatURI = URI("https://example.com/resultaat/${UUID.randomUUID()}")
        val resultaatTypeURI = URI("https://example.com/resultaattype/$resultaatTypeUUID")
        val resultaat = createResultaat(url = resultaatURI, resultaatTypeURI = resultaatTypeURI)
        val resultaattype = createResultaatType(
            url = resultaatTypeURI,
            brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP,
                datumkenmerk = "fakeDatumkenmerk"
            )
        )
        val nonMatchingEigenschap = createEigenschap(
            naam = "fakeOtherDatumkenmerk",
            definitie = "fakeEigenschapDefinitie",
            zaaktype = zaaktypeURI
        )
        every { zrcClientService.readResultaat(resultaatURI) } returns resultaat
        every { ztcClientService.readResultaattype(resultaatTypeURI) } returns resultaattype
        every { ztcClientService.readEigenschappen(zaaktypeURI) } returns listOf(nonMatchingEigenschap)

        `when`("it is converted to a REST zaak resultaat") {
            val restZaakResultaat = converter.convert(resultaatURI = resultaatURI, zaaktypeURI = zaaktypeURI)

            then("no datumKenmerkOmschrijving is set") {
                restZaakResultaat.resultaattype?.datumKenmerkOmschrijving shouldBe null
            }
        }
    }
    given(
        """
        A valid resultaatURI which corresponds to a resultaat and a resultaattype with
        a 'brondatumArchiefprocedure' with an 'afleidingswijze' of type 'EIGENSCHAP'
        matching one of the zaaktype's eigenschappen but with a blank definitie
        """
    ) {
        val resultaatTypeUUID = UUID.randomUUID()
        val resultaatURI = URI("https://example.com/resultaat/${UUID.randomUUID()}")
        val resultaatTypeURI = URI("https://example.com/resultaattype/$resultaatTypeUUID")
        val resultaat = createResultaat(url = resultaatURI, resultaatTypeURI = resultaatTypeURI)
        val resultaattype = createResultaatType(
            url = resultaatTypeURI,
            brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP,
                datumkenmerk = "fakeDatumkenmerk"
            )
        )
        val matchingEigenschapWithBlankDefinitie = createEigenschap(
            naam = "fakeDatumkenmerk",
            definitie = " ",
            zaaktype = zaaktypeURI
        )
        every { zrcClientService.readResultaat(resultaatURI) } returns resultaat
        every { ztcClientService.readResultaattype(resultaatTypeURI) } returns resultaattype
        every {
            ztcClientService.readEigenschappen(zaaktypeURI)
        } returns listOf(matchingEigenschapWithBlankDefinitie)

        `when`("it is converted to a REST zaak resultaat") {
            val restZaakResultaat = converter.convert(resultaatURI = resultaatURI, zaaktypeURI = zaaktypeURI)

            then("no datumKenmerkOmschrijving is set") {
                restZaakResultaat.resultaattype?.datumKenmerkOmschrijving shouldBe null
            }
        }
    }
})
