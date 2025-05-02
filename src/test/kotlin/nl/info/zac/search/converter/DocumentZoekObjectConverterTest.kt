/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.model.Archiefnominatie
import net.atos.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobject
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.identity.IdentityService
import nl.info.zac.search.model.DocumentIndicatie
import java.net.URI
import java.util.UUID

class DocumentZoekObjectConverterTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val brcClientService = mockk<BrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val documentZoekObjectConverter = DocumentZoekObjectConverter(
        identityService = identityService,
        brcClientService = brcClientService,
        ztcClientService = ztcClientService,
        drcClientService = drcClientService,
        zrcClientService = zrcClientService,
        enkelvoudigInformatieObjectLockService = enkelvoudigInformatieObjectLockService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given(
        """
            An enkelvoudig informatieobject with no indicatiegebruiksrecht, no archiefnominatie 
            and a related zaakinformatieobject for a zaak
        """
        ) {
        val documentUUID = UUID.randomUUID()
        val zaaktypeUUID = UUID.randomUUID()
        val informatieObjectType = createInformatieObjectType()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(
            uuid = documentUUID,
            indicatieGebruiksrecht = null
        )
        val zaakInformatieobject = createZaakInformatieobject(informatieobjectUUID = documentUUID)
        val zaakType = createZaakType(uri = URI("https://example.com/zaaktypes/$zaaktypeUUID"))
        val zaak = createZaak(
            zaakTypeURI = zaakType.url,
            archiefnominatie = null
        )

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
        every { zrcClientService.readZaak(any<UUID>()) } returns zaak
        every { ztcClientService.readZaaktype(any<URI>()) } returns zaakType
        every { ztcClientService.readInformatieobjecttype(any<URI>()) } returns informatieObjectType
        every { brcClientService.isInformatieObjectGekoppeldAanBesluit(any()) } returns false

        When("convert is called on the UUID of the enkelvoudig informatieobject") {
            val documentZoekObject = documentZoekObjectConverter.convert(documentUUID.toString())

            Then("it should return the expected DocumentZoekObject without any indicaties") {
                with(documentZoekObject!!) {
                    identificatie shouldBe enkelvoudigInformatieObject.identificatie
                    titel shouldBe enkelvoudigInformatieObject.titel
                    beschrijving shouldBe enkelvoudigInformatieObject.beschrijving
                    zaaktypeOmschrijving shouldBe zaakType.omschrijving
                    zaaktypeUuid shouldBe zaaktypeUUID.toString()
                    zaaktypeIdentificatie shouldBe zaakType.identificatie
                    zaakIdentificatie shouldBe zaak.identificatie
                    zaakUuid shouldBe zaak.uuid.toString()
                    // because the archiefnominatie is null, the zaak should be considered 'afgehandeld'
                    isZaakAfgehandeld shouldBe true
                    getDocumentIndicaties().size shouldBe 0
                }
            }
        }
    }

    Given(
        """
            An enkelvoudig informatieobject with an 'indicatie gebruiksrecht', an archiefnominatie
            and a related zaakinformatieobject for a zaak
            """
        ) {
        val documentUUID = UUID.randomUUID()
        val zaaktypeUUID = UUID.randomUUID()
        val informatieObjectType = createInformatieObjectType()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(
            uuid = documentUUID,
            indicatieGebruiksrecht = true
        )
        val zaakInformatieobject = createZaakInformatieobject(informatieobjectUUID = documentUUID)
        val zaakType = createZaakType(uri = URI("https://example.com/zaaktypes/$zaaktypeUUID"))
        val zaak = createZaak(
            zaakTypeURI = zaakType.url,
            archiefnominatie = Archiefnominatie.VERNIETIGEN
        )

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
        every { zrcClientService.readZaak(any<UUID>()) } returns zaak
        every { ztcClientService.readZaaktype(any<URI>()) } returns zaakType
        every { ztcClientService.readInformatieobjecttype(any<URI>()) } returns informatieObjectType
        every { brcClientService.isInformatieObjectGekoppeldAanBesluit(any()) } returns false

        When("convert is called on the UUID of the enkelvoudig informatieobject") {
            val documentZoekObject = documentZoekObjectConverter.convert(documentUUID.toString())

            Then("it should return the expected DocumentZoekObject with an 'indicatie gebruiksrecht'") {
                with(documentZoekObject!!) {
                    identificatie shouldBe enkelvoudigInformatieObject.identificatie
                    titel shouldBe enkelvoudigInformatieObject.titel
                    beschrijving shouldBe enkelvoudigInformatieObject.beschrijving
                    zaaktypeOmschrijving shouldBe zaakType.omschrijving
                    zaaktypeUuid shouldBe zaaktypeUUID.toString()
                    zaaktypeIdentificatie shouldBe zaakType.identificatie
                    zaakIdentificatie shouldBe zaak.identificatie
                    zaakUuid shouldBe zaak.uuid.toString()
                    // because the archiefnominatie is set, the zaak should not be considered 'afgehandeld'
                    isZaakAfgehandeld shouldBe false
                    with(getDocumentIndicaties()) {
                        size shouldBe 1
                        first() shouldBe DocumentIndicatie.GEBRUIKSRECHT
                    }
                }
            }
        }
    }
})
