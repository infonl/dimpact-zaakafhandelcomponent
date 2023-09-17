package net.atos.zac.app.zaken

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.objecten.model.createObjectRegistratieObject
import net.atos.client.vrl.VRLClientService
import net.atos.client.zgw.brc.BRCClientService
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createRolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.createZaakobjectPand
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.AardVanRol
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.aanvraag.InboxProductaanvraagService
import net.atos.zac.aanvraag.ProductaanvraagService
import net.atos.zac.aanvraag.createProductaanvraagDenhaag
import net.atos.zac.app.admin.converter.RESTZaakAfzenderConverter
import net.atos.zac.app.audit.converter.RESTHistorieRegelConverter
import net.atos.zac.app.bag.converter.RESTBAGConverter
import net.atos.zac.app.zaken.converter.RESTBesluitConverter
import net.atos.zac.app.zaken.converter.RESTBesluittypeConverter
import net.atos.zac.app.zaken.converter.RESTCommunicatiekanaalConverter
import net.atos.zac.app.zaken.converter.RESTGeometryConverter
import net.atos.zac.app.zaken.converter.RESTResultaattypeConverter
import net.atos.zac.app.zaken.converter.RESTZaakBetrokkeneConverter
import net.atos.zac.app.zaken.converter.RESTZaakConverter
import net.atos.zac.app.zaken.converter.RESTZaakOverzichtConverter
import net.atos.zac.app.zaken.converter.RESTZaaktypeConverter
import net.atos.zac.app.zaken.model.createRESTZaak
import net.atos.zac.app.zaken.model.createRESTZaakAanmaakGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.BPMNService
import net.atos.zac.flowable.CMMNService
import net.atos.zac.flowable.TakenService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.healthcheck.HealthCheckService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.createGroup
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.OverigeRechten
import net.atos.zac.policy.output.createZaakRechten
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.createZaakafhandelParameters
import net.atos.zac.zoeken.IndexeerService
import javax.enterprise.inject.Instance

class ZakenRESTServiceTest : BehaviorSpec({
    val bpmnService = mockk<BPMNService>()
    val brcClientService = mockk<BRCClientService>()
    val cmmnService = mockk<CMMNService>()
    val communicatiekanaalConverter = mockk<RESTCommunicatiekanaalConverter>()
    val configuratieService = mockk<ConfiguratieService>()
    val drcClientService = mockk<DRCClientService>()
    val eventingService = mockk<EventingService>()
    val healthCheckService = mockk<HealthCheckService>()
    val identityService = mockk<IdentityService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val indexeerService = mockk<IndexeerService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val objectsClientService = mockk<ObjectsClientService>()
    val ontkoppeldeDocumentenService = mockk<OntkoppeldeDocumentenService>()
    val opschortenZaakHelper = mockk<OpschortenZaakHelper>()
    val policyService = mockk<PolicyService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val restBagConverter = mockk<RESTBAGConverter>()
    val restBesluitConverter = mockk<RESTBesluitConverter>()
    val restBesluittypeConverter = mockk<RESTBesluittypeConverter>()
    val restGeometryConverter = mockk<RESTGeometryConverter>()
    val restHistorieRegelConverter = mockk<RESTHistorieRegelConverter>()
    val restZaakBetrokkeneConverter = mockk<RESTZaakBetrokkeneConverter>()
    val restZaakConverter = mockk<RESTZaakConverter>()
    val restZaaktypeConverter = mockk<RESTZaaktypeConverter>()
    val resultaattypeConverter = mockk<RESTResultaattypeConverter>()
    val signaleringenService = mockk<SignaleringenService>()
    val takenService = mockk<TakenService>()
    val vrlClientService = mockk<VRLClientService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zaakAfzenderConverter = mockk<RESTZaakAfzenderConverter>()
    val zaakOverzichtConverter = mockk<RESTZaakOverzichtConverter>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZRCClientService>()
    val ztcClientService = mockk<ZTCClientService>()

    val zakenRESTService = ZakenRESTService(
        zgwApiService,
        productaanvraagService,
        brcClientService,
        drcClientService,
        ztcClientService,
        zrcClientService,
        vrlClientService,
        eventingService,
        identityService,
        signaleringenService,
        ontkoppeldeDocumentenService,
        indexeerService,
        policyService,
        cmmnService,
        bpmnService,
        takenService,
        objectsClientService,
        inboxProductaanvraagService,
        zaakVariabelenService,
        configuratieService,
        loggedInUserInstance,
        restZaakConverter,
        restZaaktypeConverter,
        restBesluitConverter,
        restBesluittypeConverter,
        resultaattypeConverter,
        zaakOverzichtConverter,
        restBagConverter,
        restHistorieRegelConverter,
        restZaakBetrokkeneConverter,
        zaakafhandelParameterService,
        communicatiekanaalConverter,
        restGeometryConverter,
        healthCheckService,
        opschortenZaakHelper,
        zaakAfzenderConverter
    )

    beforeEach {
        clearAllMocks()
    }

    given("zaak input data is provided") {
        When("createZaak is called") {
            then("a zaak is created using the ZGW API and a zaak is started in the ZAC CMMN service") {
                val group = createGroup()
                val formulierData = mapOf(Pair("dummyKey", "dummyValue"))
                val natuurlijkPersoon = createNatuurlijkPersoon()
                val objectRegistratieObject = createObjectRegistratieObject()
                val productaanvraagDenhaag = createProductaanvraagDenhaag()
                val restZaak = createRESTZaak()
                val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens()
                val restZaakType = restZaakAanmaakGegevens.zaak.zaaktype
                val rolNatuurlijkPersoon = createRolNatuurlijkPersoon(natuurlijkPersoon = natuurlijkPersoon)
                val user = createLoggedInUser()
                val zaakAfhandelParameters = createZaakafhandelParameters()
                val zaakObjectPand = createZaakobjectPand()
                val zaakObjectOpenbareRuimte = createZaakobjectOpenbareRuimte()
                val zaakType = createZaakType()
                val zaak = createZaak(zaakType.url)

                every { cmmnService.startCase(zaak, zaakType, zaakAfhandelParameters, null) } just runs
                every { identityService.readGroup(restZaakAanmaakGegevens.zaak.groep.id) } returns group
                every { identityService.readUser(restZaakAanmaakGegevens.zaak.behandelaar.id) } returns user
                every { inboxProductaanvraagService.delete(restZaakAanmaakGegevens.inboxProductaanvraag.id) } just runs
                every { loggedInUserInstance.get() } returns createLoggedInUser()
                every {
                    objectsClientService.readObject(restZaakAanmaakGegevens.inboxProductaanvraag.productaanvraagObjectUUID)
                } returns objectRegistratieObject
                every { policyService.readOverigeRechten() } returns OverigeRechten(true, false, false)
                every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
                every { productaanvraagService.getFormulierData(objectRegistratieObject) } returns formulierData
                every {
                    productaanvraagService.getProductaanvraag(objectRegistratieObject)
                } returns productaanvraagDenhaag
                every { productaanvraagService.pairAanvraagPDFWithZaak(productaanvraagDenhaag, zaak.url) } just runs
                every { productaanvraagService.pairBijlagenWithZaak(productaanvraagDenhaag.attachments, zaak.url) } just runs
                every {
                    productaanvraagService.pairProductaanvraagWithZaak(
                        objectRegistratieObject,
                        zaak.url
                    )
                } just runs
                every { restBagConverter.convertToZaakobject(restZaakAanmaakGegevens.bagObjecten[0], zaak) } returns zaakObjectPand
                every { restBagConverter.convertToZaakobject(restZaakAanmaakGegevens.bagObjecten[1], zaak) } returns zaakObjectOpenbareRuimte
                every { restZaakConverter.convert(zaak) } returns restZaak
                every {
                    zaakafhandelParameterService.readZaakafhandelParameters(zaakType.uuid)
                } returns zaakAfhandelParameters
                every { zaakVariabelenService.setZaakdata(zaak.uuid, formulierData) } just runs
                every { zgwApiService.createZaak(zaak) } returns zaak
                every {
                    zrcClientService.createRol(any(), "Toegekend door de medewerker tijdens het behandelen van de zaak")
                } returns rolNatuurlijkPersoon
                every { zrcClientService.updateRol(zaak, any(), "Aanmaken zaak") } just runs
                every { zrcClientService.createZaak(zaak) } returns zaak
                every { zrcClientService.createZaakobject(zaakObjectPand) } returns zaakObjectPand
                every { zrcClientService.createZaakobject(zaakObjectOpenbareRuimte) } returns zaakObjectOpenbareRuimte
                every { ztcClientService.readZaaktype(restZaakType.uuid) } returns zaakType
                every {
                    ztcClientService.readRoltype(AardVanRol.INITIATOR, zaak.zaaktype)
                } returns createRolType(rol = AardVanRol.INITIATOR)
                every {
                    ztcClientService.readRoltype(AardVanRol.BEHANDELAAR, zaak.zaaktype)
                } returns createRolType(rol = AardVanRol.BEHANDELAAR)
                every { restZaakConverter.convert(restZaakAanmaakGegevens.zaak, zaakType) } returns zaak

                val restZaakCreated = zakenRESTService.createZaak(restZaakAanmaakGegevens)

                with(restZaakCreated) {
                    assert(uuid != null)
                }
            }
        }
    }
})
