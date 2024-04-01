package net.atos.zac.aanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.`object`.model.createORObject
import net.atos.client.or.`object`.model.createObjectRecord
import net.atos.client.vrl.VRLClientService
import net.atos.client.vrl.model.generated.CommunicatieKanaal
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakInformatieobject
import net.atos.client.zgw.zrc.model.createZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.configuratie.ConfiguratieService
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
    val drcClientService = mockk<DRCClientService>()
    val ztcClientService = mockk<ZTCClientService>()
    val vrlClientService = mockk<VRLClientService>()
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

    Given("an object registration object") {
        val bron = createBron()
        val orObject = createORObject(
            record = createObjectRecord(
                data = mapOf(
                    "bron" to bron,
                    "type" to "productaanvraag"
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
                    type shouldBe "productaanvraag"
                }
            }
        }
    }
    Given("a productaanvraag to start a zaak") {
        val productAanvraagObjectUUID = UUID.randomUUID()
        val zaakTypeUUID = UUID.randomUUID()
        val productAanvraagType = "productaanvraag"
        val zaakType = ZaakType()
        val communicatieKanaal = CommunicatieKanaal()
        val createdZaak = createZaak()
        val createdZaakobjectProductAanvraag = createZaakobjectProductaanvraag()
        val createdZaakInformatieobject = createZaakInformatieobject()
        val zaakafhandelParameters = createZaakafhandelParameters()
        val productAanvraagORObject = createORObject(
            record = createObjectRecord(
                data = mapOf(
                    "bron" to createBron(),
                    "type" to productAanvraagType,
                    // aanvraaggegevens must contain at least one key with a map value
                    "aanvraaggegevens" to mapOf("dummyKey" to mapOf("dummySubKey" to "dummyValue"))
                )
            )
        )
        val productAanvraagURI = URI("http://example.com/dummyProductaanvraag/$productAanvraagObjectUUID")
        every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
        every {
            zaakafhandelParameterBeheerService.findZaaktypeUUIDByProductaanvraagType(productAanvraagType)
        } returns Optional.of(zaakTypeUUID)
        every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
        every { vrlClientService.findCommunicatiekanaal("E-formulier") } returns Optional.of(communicatieKanaal)
        every { zgwApiService.createZaak(any()) } returns createdZaak
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID) } returns zaakafhandelParameters
        every { zrcClientService.createZaakobject(any()) } returns createdZaakobjectProductAanvraag
        every {
            zrcClientService.createZaakInformatieobject(
                any(),
                "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
            )
        } returns createdZaakInformatieobject
        every { cmmnService.startCase(createdZaak, zaakType, zaakafhandelParameters, any()) } just Runs

        When("a zaak, a zaakobject and a zaakinformatieobject are created and a CMMN process is started") {
            productaanvraagService.verwerkProductaanvraag(productAanvraagURI)

            Then("a zaak should be created and a CMMN process should be started") {
                verify(exactly = 1) {
                    zgwApiService.createZaak(any())
                    zrcClientService.createZaakobject(any())
                    cmmnService.startCase(createdZaak, zaakType, zaakafhandelParameters, any())
                }
            }
        }
    }
})
