/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.model.createBesluit
import net.atos.client.zgw.brc.model.generated.VervalredenEnum
import net.atos.client.zgw.brc.model.generated.createBesluitInformatieObject
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObject
import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createBesluitType
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieobject
import net.atos.zac.app.zaak.model.createRESTBesluitVastleggenGegevens
import java.time.LocalDate

class RestBesluitConverterTest : BehaviorSpec({
    val brcClientService = mockk<BrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val restInformatieobjectConverter = mockk<RestInformatieobjectConverter>()
    val ztcClientService = mockk<ZtcClientService>()
    val restBesluitConverter = RestBesluitConverter(
        brcClientService,
        drcClientService,
        restInformatieobjectConverter,
        ztcClientService
    )

    Given("Besluit toevoegen data with a vervaldatum") {
        val zaak = createZaak()
        val besluitToevoegenGegevens = createRESTBesluitVastleggenGegevens(
            ingangsdatum = LocalDate.now().plusDays(1),
            vervaldatum = LocalDate.now().plusDays(2),
            publicatiedatum = LocalDate.now().plusDays(3),
            uiterlijkeReactiedatum = LocalDate.now().plusDays(4)
        )
        val besluittype = createBesluitType()

        every { ztcClientService.readBesluittype(besluitToevoegenGegevens.besluittypeUuid) } returns besluittype

        When("this data is converted to a besluit") {
            val dateNow = LocalDate.now()
            val besluit = restBesluitConverter.convertToBesluit(zaak, besluitToevoegenGegevens)

            Then("the besluit is correctly converted and should have a vervalreden of type 'tijdelijk'") {
                with(besluit) {
                    this.zaak shouldBe zaak.url
                    this.besluittype shouldBe besluittype.url
                    datum shouldBe dateNow
                    ingangsdatum shouldBe besluitToevoegenGegevens.ingangsdatum
                    toelichting shouldBe besluitToevoegenGegevens.toelichting
                    vervaldatum shouldBe besluitToevoegenGegevens.vervaldatum
                    vervalreden shouldBe VervalredenEnum.TIJDELIJK
                    publicatiedatum shouldBe besluitToevoegenGegevens.publicatiedatum
                    uiterlijkeReactiedatum shouldBe besluitToevoegenGegevens.uiterlijkeReactiedatum
                }
            }
        }
    }

    Given("Besluit") {
        val besluit = createBesluit()
        val besluitType = createBesluitType(
            publicatieIndicatie = true,
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
                        publicatieIndicatie shouldBe true
                        publicatietermijn shouldBe "10 dagen"
                        reactietermijn shouldBe "2 dagen"
                    }
                    datum shouldBe besluit.datum
                    ingangsdatum shouldBe besluit.datum
                    toelichting shouldBe "dummyReason"
                    vervaldatum shouldBe besluit.vervaldatum
                    vervalreden shouldBe besluit.vervalreden
                    publicatiedatum shouldBe besluit.publicatiedatum
                    uiterlijkeReactiedatum shouldBe besluit.uiterlijkeReactiedatum
                    informatieobjecten!! shouldContain restEnkelvoudigInformatieobject
                }
            }
        }
    }
})
