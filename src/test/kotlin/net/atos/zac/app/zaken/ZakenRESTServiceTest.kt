package net.atos.zac.app.zaken

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.vrl.VRLClientService
import net.atos.client.zgw.brc.BRCClientService
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.NatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createRolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.AardVanRol
import net.atos.client.zgw.ztc.model.Roltype
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.aanvraag.InboxProductaanvraagService
import net.atos.zac.aanvraag.ProductaanvraagService
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
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.OverigeRechten
import net.atos.zac.policy.output.createZaakRechten
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zoeken.IndexeerService
import java.net.URI
import javax.enterprise.inject.Instance

class ZakenRESTServiceTest : BehaviorSpec({
    val zgwApiService = mockk<ZGWApiService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val brcClientService = mockk<BRCClientService>()
    val drcClientService = mockk<DRCClientService>()
    val ztcClientService = mockk<ZTCClientService>()
    val zrcClientService = mockk<ZRCClientService>()
    val vrlClientService = mockk<VRLClientService>()
    val eventingService = mockk<EventingService>()
    val identityService = mockk<IdentityService>()
    val signaleringenService = mockk<SignaleringenService>()
    val ontkoppeldeDocumentenService = mockk<OntkoppeldeDocumentenService>()
    val indexeerService = mockk<IndexeerService>()
    val policyService = mockk<PolicyService>()
    val cmmnService = mockk<CMMNService>()
    val bpmnService = mockk<BPMNService>()
    val takenService = mockk<TakenService>()
    val objectsClientService = mockk<ObjectsClientService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val configuratieService = mockk<ConfiguratieService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val restZaakConverter = mockk<RESTZaakConverter>()
    val restZaaktypeConverter = mockk<RESTZaaktypeConverter>()
    val restBesluitConverter = mockk<RESTBesluitConverter>()
    val restBesluittypeConverter = mockk<RESTBesluittypeConverter>()
    val resultaattypeConverter = mockk<RESTResultaattypeConverter>()
    val zaakOverzichtConverter = mockk<RESTZaakOverzichtConverter>()
    val bagConverter = mockk<RESTBAGConverter>()
    val auditTrailConverter = mockk<RESTHistorieRegelConverter>()
    val zaakBetrokkeneConverter = mockk<RESTZaakBetrokkeneConverter>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val communicatiekanaalConverter = mockk<RESTCommunicatiekanaalConverter>()
    val restGeometryConverter = mockk<RESTGeometryConverter>()
    val healthCheckService = mockk<HealthCheckService>()
    val opschortenZaakHelper = mockk<OpschortenZaakHelper>()
    val zaakAfzenderConverter = mockk<RESTZaakAfzenderConverter>()

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
        bagConverter,
        auditTrailConverter,
        zaakBetrokkeneConverter,
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
                val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens()
                val restZaakType = restZaakAanmaakGegevens.zaak.zaaktype
                val zaakType = createZaakType()
                val zaakTypeURI = URI("http://example.com/${zaakType.uuid}")
                val zaak = createZaak(zaakTypeURI)
                val natuurlijkPersoon = createNatuurlijkPersoon()
                val rolNatuurlijkPersoon = createRolNatuurlijkPersoon(zaakTypeURI, natuurlijkPersoon)

                every { loggedInUserInstance.get() } returns createLoggedInUser()
                every { policyService.readOverigeRechten() } returns OverigeRechten(true, false, false)
                every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
                every { ztcClientService.readZaaktype(restZaakType.uuid) } returns zaakType
                every { ztcClientService.readRoltype(AardVanRol.INITIATOR, zaak.zaaktype) } returns createRolType(zaakType.url)
                every { restZaakConverter.convert(restZaakAanmaakGegevens.zaak, zaakType) } returns zaak
                every { zgwApiService.createZaak(zaak) } returns zaak
                every { zrcClientService.createRol(any(), any())} returns rolNatuurlijkPersoon
                // TODO
                //every { identityService.readGroup(any()) } returns null

                val restZaak = zakenRESTService.createZaak(restZaakAanmaakGegevens)

                with(restZaak) {
                    assert(uuid != null)
                }
            }
        }
    }
})
