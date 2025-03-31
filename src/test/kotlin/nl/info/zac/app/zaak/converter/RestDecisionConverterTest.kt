/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieobject
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import nl.info.client.zgw.brc.model.generated.createBesluitInformatieObject
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBesluitType
import nl.info.zac.app.zaak.model.createRestDecisionCreateData
import nl.info.zac.configuratie.ConfiguratieService
import java.time.LocalDate

class RestDecisionConverterTest : BehaviorSpec({
    val brcClientService = mockk<BrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val restInformatieobjectConverter = mockk<RestInformatieobjectConverter>()
    val ztcClientService = mockk<ZtcClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val restDecisionConverter = RestDecisionConverter(
        brcClientService,
        drcClientService,
        restInformatieobjectConverter,
        ztcClientService,
        configuratieService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Besluit toevoegen data with a vervaldatum") {
        val zaak = createZaak()
        val decisionCreateData = createRestDecisionCreateData(
            ingangsdatum = LocalDate.now().plusDays(1),
            vervaldatum = LocalDate.now().plusDays(2),
            publicationDate = LocalDate.now().plusDays(3),
            lastResponseDate = LocalDate.now().plusDays(4)
        )
        val besluittype = createBesluitType()

        every { ztcClientService.readBesluittype(decisionCreateData.besluittypeUuid) } returns besluittype
        every { configuratieService.readVerantwoordelijkeOrganisatie() } returns "316245124"

        When("this data is converted to a besluit") {
            val dateNow = LocalDate.now()
            val besluit = restDecisionConverter.convertToBesluit(zaak, decisionCreateData)

            Then("the besluit is correctly converted and should have a vervalreden of type 'tijdelijk'") {
                with(besluit) {
                    this.zaak shouldBe zaak.url
                    this.besluittype shouldBe besluittype.url
                    datum shouldBe dateNow
                    ingangsdatum shouldBe decisionCreateData.ingangsdatum
                    toelichting shouldBe decisionCreateData.toelichting
                    vervaldatum shouldBe decisionCreateData.vervaldatum
                    vervalreden shouldBe VervalredenEnum.TIJDELIJK
                    publicatiedatum shouldBe decisionCreateData.publicationDate
                    uiterlijkeReactiedatum shouldBe decisionCreateData.lastResponseDate
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
            val restDecision = restDecisionConverter.convertToRestDecision(besluit)

            Then("the conversion is correct") {
                with(restDecision) {
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
                    toelichting shouldBe "dummyReason"
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
