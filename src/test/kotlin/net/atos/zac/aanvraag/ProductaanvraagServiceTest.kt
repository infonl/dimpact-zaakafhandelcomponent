package net.atos.zac.aanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.`object`.model.createORObject
import net.atos.client.or.`object`.model.createObjectRecord
import net.atos.client.vrl.VrlClientService
import net.atos.client.vrl.model.generated.CommunicatieKanaal
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Point
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakInformatieobject
import net.atos.client.zgw.zrc.model.createZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.aanvraag.model.InboxProductaanvraag
import net.atos.zac.aanvraag.model.generated.Geometry
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.configuratie.ConfiguratieService.BRON_ORGANISATIE
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.flowable.BPMNService
import net.atos.zac.flowable.CMMNService
import net.atos.zac.identity.IdentityService
import net.atos.zac.zaaksturing.ZaakafhandelParameterBeheerService
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.createZaakafhandelParameters
import java.net.URI
import java.util.Optional
import java.util.UUID

class ProductaanvraagServiceTest : BehaviorSpec({
    val objectsClientService = mockk<ObjectsClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZRCClientService>()
    val drcClientService = mockk<DrcClientService>()
    val ztcClientService = mockk<ZTCClientService>()
    val vrlClientService = mockk<VrlClientService>()
    val identityService = mockk<IdentityService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zaakafhandelParameterBeheerService = mockk<ZaakafhandelParameterBeheerService>()
    val inboxDocumentenService = mockk<InboxDocumentenService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val cmmnService = mockk<CMMNService>()
    val bpmnService = mockk<BPMNService>()
    val configuratieService = mockk<ConfiguratieService>()
    val productaanvraagService = ProductaanvraagService(
        objectsClientService,
        zgwApiService,
        zrcClientService,
        drcClientService,
        ztcClientService,
        vrlClientService,
        identityService,
        zaakafhandelParameterService,
        zaakafhandelParameterBeheerService,
        inboxDocumentenService,
        inboxProductaanvraagService,
        cmmnService,
        bpmnService,
        configuratieService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("a productaanvraag-dimpact object registration object") {
        val bron = createBron()
        val zaakIdentificatie = "dummyZaakIdentificatie"
        val coordinates = listOf(52.08968250760225, 5.114358701512936)
        val orObject = createORObject(
            record = createObjectRecord(
                data = mapOf(
                    "bron" to bron,
                    "type" to "productaanvraag",
                    "zaakgegevens" to mapOf(
                        "identificatie" to zaakIdentificatie,
                        "geometry" to mapOf(
                            "type" to "Point",
                            "coordinates" to coordinates
                        )
                    )
                )
            )
        )
        When("the productaanvraag is requested from the product aanvraag service") {
            val productAanVraagDimpact = productaanvraagService.getProductaanvraag(orObject)

            Then("the productaanvraag of type 'productaanvraag Dimpact' is returned and contains the expected data") {
                with(productAanVraagDimpact) {
                    with(this.bron) {
                        naam shouldBe bron.naam
                        kenmerk shouldBe bron.kenmerk
                    }
                    taal shouldBe "nld"
                    type shouldBe "productaanvraag"
                    with(zaakgegevens) {
                        identificatie shouldBe zaakIdentificatie
                        with(geometry) {
                            type shouldBe Geometry.Type.POINT
                            coordinates shouldBe coordinates
                        }
                    }
                }
            }
        }
    }
    Given("a productaanvraag-dimpact object registration object containing required data") {
        clearAllMocks()
        val productAanvraagObjectUUID = UUID.randomUUID()
        val zaakTypeUUID = UUID.randomUUID()
        val productAanvraagType = "productaanvraag"
        val zaakType = createZaakType()
        val communicatieKanaal = CommunicatieKanaal()
        val createdZaak = createZaak()
        val createdZaakobjectProductAanvraag = createZaakobjectProductaanvraag()
        val createdZaakInformatieobject = createZaakInformatieobject()
        val zaakafhandelParameters = createZaakafhandelParameters()
        val formulierBron = createBron()
        val coordinates = listOf(52.08968250760225, 5.114358701512936)
        val productAanvraagORObject = createORObject(
            record = createObjectRecord(
                data = mapOf(
                    "bron" to formulierBron,
                    "type" to productAanvraagType,
                    // aanvraaggegevens must contain at least one key with a map value
                    "aanvraaggegevens" to mapOf("dummyKey" to mapOf("dummySubKey" to "dummyValue")),
                    "zaakgegevens" to mapOf(
                        "geometry" to mapOf(
                            "type" to "Point",
                            "coordinates" to coordinates
                        )
                    )
                )
            )
        )
        val zaakToBeCreated = slot<Zaak>()
        every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
        every {
            zaakafhandelParameterBeheerService.findZaaktypeUUIDByProductaanvraagType(productAanvraagType)
        } returns Optional.of(zaakTypeUUID)
        every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
        every { vrlClientService.findCommunicatiekanaal("E-formulier") } returns Optional.of(communicatieKanaal)
        every { zgwApiService.createZaak(capture(zaakToBeCreated)) } returns createdZaak
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID) } returns zaakafhandelParameters
        every { zrcClientService.createZaakobject(any()) } returns createdZaakobjectProductAanvraag
        every {
            zrcClientService.createZaakInformatieobject(
                any(),
                "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
            )
        } returns createdZaakInformatieobject
        every { cmmnService.startCase(createdZaak, zaakType, zaakafhandelParameters, any()) } just Runs

        When("the productaanvraag is handled") {
            productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

            Then("a zaak should be created and a CMMN case process should be started") {
                verify(exactly = 1) {
                    zgwApiService.createZaak(any())
                    zrcClientService.createZaakobject(any())
                    cmmnService.startCase(createdZaak, zaakType, zaakafhandelParameters, any())
                }
                with(zaakToBeCreated.captured) {
                    zaaktype shouldBe zaakType.url
                    communicatiekanaal shouldBe communicatieKanaal.url
                    bronorganisatie shouldBe BRON_ORGANISATIE
                    omschrijving shouldBe "Aangemaakt vanuit ${formulierBron.naam} met kenmerk '${formulierBron.kenmerk}'"
                    toelichting shouldBe null
                    with(zaakgeometrie) {
                        type.toValue() shouldBe Geometry.Type.POINT.value()
                        with((this as Point).coordinates) {
                            latitude.toDouble() shouldBe coordinates[0]
                            longitude.toDouble() shouldBe coordinates[1]
                        }
                    }
                }
            }
        }
    }
    Given("a productaanvraag-dimpact object registration object missing required aanvraaggegevens") {
        clearAllMocks()
        val productAanvraagObjectUUID = UUID.randomUUID()
        val productAanvraagType = "productaanvraag"
        val formulierBron = createBron()
        val productAanvraagORObjectWithMissingAanvraaggegevens = createORObject(
            record = createObjectRecord(
                data = mapOf(
                    "bron" to formulierBron,
                    "type" to productAanvraagType
                )
            )
        )
        every {
            objectsClientService.readObject(productAanvraagObjectUUID)
        } returns productAanvraagORObjectWithMissingAanvraaggegevens

        When("the productaanvraag is handled") {
            productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

            Then(
                """
                an inbox productaanvraag should be created and a zaak should not be created, 
                and a CMMN case process should not be started
                """
            ) {
                verify(exactly = 0) {
                    zgwApiService.createZaak(any())
                    zrcClientService.createZaakobject(any())
                    cmmnService.startCase(any(), any(), any(), any())
                }
            }
        }
    }
    Given(
        """
        a productaanvraag-dimpact object registration object containing required data but
        no betrokkenen and which contains a zaaktype for which no zaakafhandel parameters are configured 
        and for which no zaaktype exists in the ZTC catalogus
        """
    ) {
        clearAllMocks()
        val productAanvraagObjectUUID = UUID.randomUUID()
        val catalogusURI = URI("dummyCatalogusURI")
        val productAanvraagType = "productaanvraag"
        val zaakType = createZaakType()
        val createdZaak = createZaak()
        val zaakafhandelParameters = createZaakafhandelParameters()
        val formulierBron = createBron()
        val productAanvraagORObject = createORObject(
            record = createObjectRecord(
                data = mapOf(
                    "bron" to formulierBron,
                    "type" to productAanvraagType,
                    // aanvraaggegevens must contain at least one key with a map value
                    "aanvraaggegevens" to mapOf("dummyKey" to mapOf("dummySubKey" to "dummyValue"))
                )
            ),
            uuid = productAanvraagObjectUUID
        )
        val inboxProductaanvraagSlot = slot<InboxProductaanvraag>()
        every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
        // no zaakafhandelparameters are configured for the zaaktype
        every {
            zaakafhandelParameterBeheerService.findZaaktypeUUIDByProductaanvraagType(productAanvraagType)
        } returns Optional.empty()
        every { configuratieService.readDefaultCatalogusURI() } returns catalogusURI
        every { ztcClientService.listZaaktypen(catalogusURI) } returns listOf(zaakType)
        every { inboxProductaanvraagService.create(capture(inboxProductaanvraagSlot)) } just runs

        When("the productaanvraag is handled") {
            productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

            Then(
                """
                an inbox productaanvraag should be created, no zaak should be created and no CMMN case process should be started
                """
            ) {
                verify(exactly = 1) {
                    inboxProductaanvraagService.create(any())
                }
                verify(exactly = 0) {
                    zgwApiService.createZaak(any())
                    zrcClientService.createZaakobject(any())
                    cmmnService.startCase(createdZaak, zaakType, zaakafhandelParameters, any())
                }
                inboxProductaanvraagSlot.captured.run {
                    productaanvraagObjectUUID shouldBe productAanvraagObjectUUID
                    aanvraagdocumentUUID shouldBe null
                    ontvangstdatum shouldBe null
                    type shouldBe productAanvraagType
                    initiatorID shouldBe null
                    aantalBijlagen shouldBe 0
                }
            }
        }
    }
})
