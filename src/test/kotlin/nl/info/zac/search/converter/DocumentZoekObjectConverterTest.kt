/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakEigenschap
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
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

    afterEach {
        checkUnnecessaryStub()
    }

    given(
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
        val zaakInformatieobject = createZaakInformatieobjectForReads(informatieobject = URI("https://example.com/$documentUUID"))
        val zaakType = createZaakType(uri = URI("https://example.com/zaaktypes/$zaaktypeUUID"))
        val zaak = createZaak(
            zaaktypeUri = zaakType.url,
            archiefnominatie = null
        )

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
        every { zrcClientService.readZaak(any<UUID>()) } returns zaak
        every { ztcClientService.readZaaktype(any<URI>()) } returns zaakType
        every { ztcClientService.readInformatieobjecttype(any<URI>()) } returns informatieObjectType
        every { brcClientService.isInformatieObjectGekoppeldAanBesluit(any()) } returns false
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
            createZaakEigenschap(naam = "ZAAK_GEAUTORISEERD", waarde = "true")
        )

        `when`("convert is called on the UUID of the enkelvoudig informatieobject") {
            val documentZoekObject = documentZoekObjectConverter.convert(documentUUID.toString())

            then("it should return the expected DocumentZoekObject without any indicaties") {
                with(documentZoekObject!!) {
                    identificatie shouldBe enkelvoudigInformatieObject.identificatie
                    titel shouldBe enkelvoudigInformatieObject.titel
                    beschrijving shouldBe enkelvoudigInformatieObject.beschrijving
                    zaaktypeOmschrijving shouldBe zaakType.omschrijving
                    zaaktypeUuid shouldBe zaaktypeUUID.toString()
                    zaaktypeIdentificatie shouldBe zaakType.identificatie
                    zaakIdentificatie shouldBe zaak.identificatie
                    zaakUuid shouldBe zaak.uuid.toString()
                    // because the archiefnominatie is null, the zaak is still open and not considered 'afgehandeld'
                    isZaakAfgehandeld shouldBe false
                    isZaakspecifiekGeautoriseerd shouldBe true
                    getDocumentIndicaties().size shouldBe 0
                }
            }
        }
    }

    given(
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
        val zaakInformatieobject = createZaakInformatieobjectForReads(informatieobject = URI("https://example.com/$documentUUID"))
        val zaakType = createZaakType(uri = URI("https://example.com/zaaktypes/$zaaktypeUUID"))
        val zaak = createZaak(
            zaaktypeUri = zaakType.url,
            archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
        )

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
        every { zrcClientService.readZaak(any<UUID>()) } returns zaak
        every { ztcClientService.readZaaktype(any<URI>()) } returns zaakType
        every { ztcClientService.readInformatieobjecttype(any<URI>()) } returns informatieObjectType
        every { brcClientService.isInformatieObjectGekoppeldAanBesluit(any()) } returns false
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("convert is called on the UUID of the enkelvoudig informatieobject") {
            val documentZoekObject = documentZoekObjectConverter.convert(documentUUID.toString())

            then("it should return the expected DocumentZoekObject with an 'indicatie gebruiksrecht'") {
                with(documentZoekObject!!) {
                    identificatie shouldBe enkelvoudigInformatieObject.identificatie
                    titel shouldBe enkelvoudigInformatieObject.titel
                    beschrijving shouldBe enkelvoudigInformatieObject.beschrijving
                    zaaktypeOmschrijving shouldBe zaakType.omschrijving
                    zaaktypeUuid shouldBe zaaktypeUUID.toString()
                    zaaktypeIdentificatie shouldBe zaakType.identificatie
                    zaakIdentificatie shouldBe zaak.identificatie
                    zaakUuid shouldBe zaak.uuid.toString()
                    // because the archiefnominatie is set, the zaak is closed and considered 'afgehandeld'
                    isZaakAfgehandeld shouldBe true
                    isZaakspecifiekGeautoriseerd shouldBe false
                    with(getDocumentIndicaties()) {
                        size shouldBe 1
                        first() shouldBe DocumentIndicatie.GEBRUIKSRECHT
                    }
                }
            }
        }
    }

    given(
        """
            An enkelvoudig informatieobject with a null 'bestandsomvang'
            and a related zaakinformatieobject for a zaak
        """
    ) {
        val documentUUID = UUID.randomUUID()
        val zaaktypeUUID = UUID.randomUUID()
        val informatieObjectType = createInformatieObjectType()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(
            uuid = documentUUID,
            indicatieGebruiksrecht = null,
            bestandsomvang = null
        )
        val zaakInformatieobject = createZaakInformatieobjectForReads(informatieobject = URI("https://example.com/$documentUUID"))
        val zaakType = createZaakType(uri = URI("https://example.com/zaaktypes/$zaaktypeUUID"))
        val zaak = createZaak(
            zaaktypeUri = zaakType.url,
            archiefnominatie = null
        )

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
        every { zrcClientService.readZaak(any<UUID>()) } returns zaak
        every { ztcClientService.readZaaktype(any<URI>()) } returns zaakType
        every { ztcClientService.readInformatieobjecttype(any<URI>()) } returns informatieObjectType
        every { brcClientService.isInformatieObjectGekoppeldAanBesluit(any()) } returns false
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("convert is called on the UUID of the enkelvoudig informatieobject") {
            val documentZoekObject = documentZoekObjectConverter.convert(documentUUID.toString())

            then("it should return the expected DocumentZoekObject with a 'bestandsomvang' of 0") {
                documentZoekObject!!.bestandsomvang shouldBe 0
            }
        }
    }

    given("an already-retrieved zaak, converted via the zaak-driven combined reindex entry point") {
        val documentUUID = UUID.randomUUID()
        val zaaktypeUUID = UUID.randomUUID()
        val informatieObjectType = createInformatieObjectType()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(
            uuid = documentUUID,
            indicatieGebruiksrecht = null
        )
        val zaakInformatieobject = createZaakInformatieobjectForReads(informatieobject = URI("https://example.com/$documentUUID"))
        val zaakType = createZaakType(uri = URI("https://example.com/zaaktypes/$zaaktypeUUID"))
        val zaak = createZaak(zaaktypeUri = zaakType.url, archiefnominatie = null)

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
        every { ztcClientService.readZaaktype(any<URI>()) } returns zaakType
        every { ztcClientService.readInformatieobjecttype(any<URI>()) } returns informatieObjectType
        every { brcClientService.isInformatieObjectGekoppeldAanBesluit(any()) } returns false

        `when`("convert is called with the zaak supplied directly") {
            val documentZoekObject = documentZoekObjectConverter.convert(documentUUID.toString(), zaak) { true }

            then("the document zoek object still resolves its zaak fields from the supplied zaak") {
                documentZoekObject!!.zaakUuid shouldBe zaak.uuid.toString()
                documentZoekObject.isZaakspecifiekGeautoriseerd shouldBe true
            }

            then("the zaak is never read again from the ZRC API, since it was already supplied") {
                verify(exactly = 0) { zrcClientService.readZaak(any<UUID>()) }
            }
        }
    }

    given("a document with no linked zaak at all, converted via the zaak-driven combined reindex entry point") {
        val documentUUID = UUID.randomUUID()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(uuid = documentUUID)
        val zaak = createZaak()

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns emptyList()

        `when`("convert is called with a zaak supplied, but the document resolves no zaakinformatieobject") {
            val documentZoekObject = documentZoekObjectConverter.convert(documentUUID.toString(), zaak) { true }

            then("it returns null, the same as the id-only overload does for an orphan document") {
                documentZoekObject shouldBe null
            }
        }
    }
})
