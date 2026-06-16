/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import nl.info.client.zgw.brc.model.generated.createBesluitInformatieObject
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBesluitType
import nl.info.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import nl.info.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieobject
import nl.info.zac.app.zaak.model.createRestBesluitCreateData
import nl.info.zac.configuration.ConfigurationService
import java.time.LocalDate

class RestBesluitConverterTest : BehaviorSpec({
    val brcClientService = mockk<BrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val restInformatieobjectConverter = mockk<RestInformatieobjectConverter>()
    val ztcClientService = mockk<ZtcClientService>()
    val configurationService = mockk<ConfigurationService>()
    val restBesluitConverter = RestBesluitConverter(
        brcClientService,
        drcClientService,
        restInformatieobjectConverter,
        ztcClientService,
        configurationService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    Given("Besluit toevoegen data with a vervaldatum") {
        val zaak = createZaak()
        val besluitCreateData = createRestBesluitCreateData(
            ingangsdatum = LocalDate.now().plusDays(1),
            vervaldatum = LocalDate.now().plusDays(2),
            publicationDate = LocalDate.now().plusDays(3),
            lastResponseDate = LocalDate.now().plusDays(4)
        )
        val besluittype = createBesluitType()

        every { ztcClientService.readBesluittype(besluitCreateData.besluittypeUuid) } returns besluittype
        every { configurationService.readVerantwoordelijkeOrganisatie() } returns "316245124"

        When("this data is converted to a besluit") {
            val dateNow = LocalDate.now()
            val besluit = restBesluitConverter.convertToBesluit(zaak, besluitCreateData)

            Then("the besluit is correctly converted and should have a vervalreden of type 'tijdelijk'") {
                with(besluit) {
                    this.zaak shouldBe zaak.url
                    this.besluittype shouldBe besluittype.url
                    datum shouldBe dateNow
                    ingangsdatum shouldBe besluitCreateData.ingangsdatum
                    toelichting shouldBe besluitCreateData.toelichting
                    vervaldatum shouldBe besluitCreateData.vervaldatum
                    vervalreden shouldBe VervalredenEnum.TIJDELIJK
                    publicatiedatum shouldBe besluitCreateData.publicationDate
                    uiterlijkeReactiedatum shouldBe besluitCreateData.lastResponseDate
                }
            }
        }
    }

    Given("Besluit") {
        val besluit = createBesluit()
        val besluitType = createBesluitType(
            publicationEnabled = true,
            publicationPeriod = "P10D",
            reactionPeriod = "P2D"
        )
        val besluitInformatieObject = createBesluitInformatieObject()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
        val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()

        every { ztcClientService.readBesluittype(besluit.besluittype) } returns besluitType
        every { brcClientService.listBesluitInformatieobjecten(besluit.url) } returns listOf(besluitInformatieObject)
        every {
            drcClientService.readEnkelvoudigInformatieobject(besluitInformatieObject.informatieobject)
        } returns enkelvoudigInformatieObject
        every {
            restInformatieobjectConverter.convertInformatieobjectenToREST(listOf(enkelvoudigInformatieObject))
        } returns listOf(restEnkelvoudigInformatieobject)

        When("it is converted to a rest representation") {
            val restBesluit = restBesluitConverter.convertToRestBesluit(besluit)

            Then("the conversion is correct") {
                with(restBesluit) {
                    uuid shouldBe besluit.url.extractUuid()
                    with(besluittype!!) {
                        id shouldBe besluitType.url.extractUuid()
                        naam shouldBe besluitType.omschrijving
                        toelichting shouldBe besluitType.toelichting
                        informatieobjecttypen shouldBe besluitType.informatieobjecttypen
                        with(publication) {
                            enabled shouldBe true
                            publicationTerm shouldBe "10 dagen"
                            publicationTermDays shouldBe 10
                            responseTerm shouldBe "2 dagen"
                            responseTermDays shouldBe 2
                        }
                    }
                    datum shouldBe besluit.datum
                    ingangsdatum shouldBe besluit.datum
                    toelichting shouldBe "fakeReason"
                    vervaldatum shouldBe besluit.vervaldatum
                    vervalreden shouldBe besluit.vervalreden
                    publicationDate shouldBe besluit.publicatiedatum
                    lastResponseDate shouldBe besluit.uiterlijkeReactiedatum
                    informatieobjecten!! shouldContain restEnkelvoudigInformatieobject
                }
            }
        }
    }
})
