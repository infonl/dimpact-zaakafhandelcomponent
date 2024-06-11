/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaken

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.`object`.model.createORObject
import net.atos.client.vrl.VrlClientService
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Medewerker
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createRolMedewerker
import net.atos.client.zgw.zrc.model.createRolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createRolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.createZaakobjectPand
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.zac.aanvraag.InboxProductaanvraagService
import net.atos.zac.aanvraag.ProductaanvraagService
import net.atos.zac.aanvraag.createProductaanvraagDimpact
import net.atos.zac.app.admin.converter.RESTZaakAfzenderConverter
import net.atos.zac.app.audit.converter.RESTHistorieRegelConverter
import net.atos.zac.app.bag.converter.RESTBAGConverter
import net.atos.zac.app.zaken.converter.RESTBesluitConverter
import net.atos.zac.app.zaken.converter.RESTBesluittypeConverter
import net.atos.zac.app.zaken.converter.RESTGeometryConverter
import net.atos.zac.app.zaken.converter.RESTResultaattypeConverter
import net.atos.zac.app.zaken.converter.RESTZaakConverter
import net.atos.zac.app.zaken.converter.RESTZaakOverzichtConverter
import net.atos.zac.app.zaken.converter.RESTZaaktypeConverter
import net.atos.zac.app.zaken.model.ZAAK_TYPE_1_OMSCHRIJVING
import net.atos.zac.app.zaken.model.createRESTZaak
import net.atos.zac.app.zaken.model.createRESTZaakAanmaakGegevens
import net.atos.zac.app.zaken.model.createRESTZaakToekennenGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.BPMNService
import net.atos.zac.flowable.CMMNService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.healthcheck.HealthCheckService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.createGroup
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createOverigeRechtenAllDeny
import net.atos.zac.policy.output.createZaakRechtenAllDeny
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringService
import net.atos.zac.zaak.ZaakService
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.createZaakafhandelParameters
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.Optional

@Suppress("LongParameterList")
class ZakenRESTServiceTest : BehaviorSpec({
    val bpmnService: BPMNService = mockk<BPMNService>()
    val brcClientService: BrcClientService = mockk<BrcClientService>()
    val configuratieService: ConfiguratieService = mockk<ConfiguratieService>()
    val cmmnService: CMMNService = mockk<CMMNService>()
    val drcClientService: DrcClientService = mockk<DrcClientService>()
    val eventingService: EventingService = mockk<EventingService>()
    val healthCheckService: HealthCheckService = mockk<HealthCheckService>()
    val identityService: IdentityService = mockk<IdentityService>()
    val inboxProductaanvraagService: InboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val indexeerService: IndexeerService = mockk<IndexeerService>()
    val loggedInUserInstance: Instance<LoggedInUser> = mockk<Instance<LoggedInUser>>()
    val objectsClientService: ObjectsClientService = mockk<ObjectsClientService>()
    val ontkoppeldeDocumentenService: OntkoppeldeDocumentenService = mockk<OntkoppeldeDocumentenService>()
    val opschortenZaakHelper: OpschortenZaakHelper = mockk<OpschortenZaakHelper>()
    val policyService: PolicyService = mockk<PolicyService>()
    val productaanvraagService: ProductaanvraagService = mockk<ProductaanvraagService>()
    val restBAGConverter: RESTBAGConverter = mockk<RESTBAGConverter>()
    val restBesluitConverter: RESTBesluitConverter = mockk<RESTBesluitConverter>()
    val restBesluittypeConverter: RESTBesluittypeConverter = mockk<RESTBesluittypeConverter>()
    val restGeometryConverter: RESTGeometryConverter = mockk<RESTGeometryConverter>()
    val restResultaattypeConverter: RESTResultaattypeConverter = mockk<RESTResultaattypeConverter>()
    val restZaakConverter: RESTZaakConverter = mockk<RESTZaakConverter>()
    val restZaakAfzenderConverter: RESTZaakAfzenderConverter = mockk<RESTZaakAfzenderConverter>()
    val restZaakOverzichtConverter: RESTZaakOverzichtConverter = mockk<RESTZaakOverzichtConverter>()
    val restZaaktypeConverter: RESTZaaktypeConverter = mockk<RESTZaaktypeConverter>()
    val restHistorieRegelConverter: RESTHistorieRegelConverter = mockk<RESTHistorieRegelConverter>()
    val signaleringService: SignaleringService = mockk<SignaleringService>()
    val flowableTaskService: FlowableTaskService = mockk<FlowableTaskService>()
    val vrlClientService: VrlClientService = mockk<VrlClientService>()
    val zaakafhandelParameterService: ZaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zaakVariabelenService: ZaakVariabelenService = mockk<ZaakVariabelenService>()
    val zaakService: ZaakService = mockk<ZaakService>()
    val zgwApiService: ZGWApiService = mockk<ZGWApiService>()
    val zrcClientService: ZRCClientService = mockk<ZRCClientService>()
    val ztcClientService: ZTCClientService = mockk<ZTCClientService>()

    val zakenRESTService = ZakenRESTService(
        zgwApiService = zgwApiService,
        cmmnService = cmmnService,
        identityService = identityService,
        inboxProductaanvraagService = inboxProductaanvraagService,
        loggedInUserInstance = loggedInUserInstance,
        objectsClientService = objectsClientService,
        policyService = policyService,
        productaanvraagService = productaanvraagService,
        restBAGConverter = restBAGConverter,
        restZaakConverter = restZaakConverter,
        zaakafhandelParameterService = zaakafhandelParameterService,
        zaakVariabelenService = zaakVariabelenService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        zaakService = zaakService,
        indexeerService = indexeerService,
        restHistorieRegelConverter = restHistorieRegelConverter,
        restBesluitConverter = restBesluitConverter,
        bpmnService = bpmnService,
        brcClientService = brcClientService,
        configuratieService = configuratieService,
        drcClientService = drcClientService,
        eventingService = eventingService,
        healthCheckService = healthCheckService,
        ontkoppeldeDocumentenService = ontkoppeldeDocumentenService,
        opschortenZaakHelper = opschortenZaakHelper,
        restBesluittypeConverter = restBesluittypeConverter,
        restGeometryConverter = restGeometryConverter,
        restResultaattypeConverter = restResultaattypeConverter,
        restZaakOverzichtConverter = restZaakOverzichtConverter,
        signaleringService = signaleringService,
        flowableTaskService = flowableTaskService,
        vrlClientService = vrlClientService,
        restZaakAfzenderConverter = restZaakAfzenderConverter,
        restZaaktypeConverter = restZaaktypeConverter
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("zaak input data is provided") {
        val group = createGroup()
        val formulierData = mapOf(Pair("dummyKey", "dummyValue"))
        val natuurlijkPersoon = createNatuurlijkPersoon()
        val objectRegistratieObject = createORObject()
        val productaanvraagDimpact = createProductaanvraagDimpact()
        val restZaak = createRESTZaak()
        val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
        val zaakTypeUUID = URIUtil.parseUUIDFromResourceURI(zaakType.url)
        val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(zaakTypeUUID = zaakTypeUUID)
        val rolMedewerker = createRolMedewerker()
        val rolNatuurlijkPersoon = createRolNatuurlijkPersoon(natuurlijkPersoon = natuurlijkPersoon)
        val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid()
        val user = createLoggedInUser()
        val zaakAfhandelParameters = createZaakafhandelParameters()
        val zaakObjectPand = createZaakobjectPand()
        val zaakObjectOpenbareRuimte = createZaakobjectOpenbareRuimte()
        val zaak = createZaak(zaakType.url)

        every { cmmnService.startCase(zaak, zaakType, zaakAfhandelParameters, null) } just runs
        every { identityService.readGroup(restZaakAanmaakGegevens.zaak.groep?.id) } returns group
        every { identityService.readUser(restZaakAanmaakGegevens.zaak.behandelaar?.id) } returns user
        every {
            inboxProductaanvraagService.delete(restZaakAanmaakGegevens.inboxProductaanvraag?.id)
        } just runs
        every { loggedInUserInstance.get() } returns createLoggedInUser()
        every {
            objectsClientService
                .readObject(restZaakAanmaakGegevens.inboxProductaanvraag?.productaanvraagObjectUUID)
        } returns objectRegistratieObject
        every { productaanvraagService.getFormulierData(objectRegistratieObject) } returns formulierData
        every {
            productaanvraagService.getProductaanvraag(objectRegistratieObject)
        } returns productaanvraagDimpact
        every { productaanvraagService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url) } just runs
        every {
            productaanvraagService.pairBijlagenWithZaak(productaanvraagDimpact.bijlagen, zaak.url)
        } just runs
        every {
            productaanvraagService.pairProductaanvraagWithZaak(
                objectRegistratieObject,
                zaak.url
            )
        } just runs
        every {
            restBAGConverter.convertToZaakobject(restZaakAanmaakGegevens.bagObjecten?.get(0), zaak)
        } returns zaakObjectPand
        every {
            restBAGConverter.convertToZaakobject(
                restZaakAanmaakGegevens.bagObjecten?.get(1),
                zaak
            )
        } returns zaakObjectOpenbareRuimte
        every { restZaakConverter.convert(zaak) } returns restZaak
        every { restZaakConverter.convert(restZaakAanmaakGegevens.zaak, zaakType) } returns zaak
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID)
        } returns zaakAfhandelParameters
        every { zaakVariabelenService.setZaakdata(zaak.uuid, formulierData) } just runs
        every { zgwApiService.createZaak(zaak) } returns zaak
        every { zrcClientService.createRol(any(), any()) } returns rolNatuurlijkPersoon
        every { zrcClientService.updateRol(zaak, any(), any()) } just runs
        every { zrcClientService.createZaakobject(zaakObjectPand) } returns zaakObjectPand
        every { zrcClientService.createZaakobject(zaakObjectOpenbareRuimte) } returns zaakObjectOpenbareRuimte
        every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
        every {
            ztcClientService.readRoltype(RolType.OmschrijvingGeneriekEnum.INITIATOR, zaak.zaaktype)
        } returns createRolType(omschrijvingGeneriek = RolType.OmschrijvingGeneriekEnum.INITIATOR)
        every { zaakService.bepaalRolGroep(group, zaak) } returns rolOrganisatorischeEenheid
        every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker

        When("createZaak is called for a zaaktype for which the logged in user has permissions") {
            every { policyService.readOverigeRechten() } returns createOverigeRechtenAllDeny(startenZaak = true)
            every {
                policyService.readZaakRechten(zaak)
            } returns createZaakRechtenAllDeny(toevoegenInitiatorPersoon = true)

            val restZaakReturned = zakenRESTService.createZaak(restZaakAanmaakGegevens)

            Then("a zaak is created using the ZGW API and a zaak is started in the ZAC CMMN service") {
                with(restZaakReturned) {
                    assertEquals(this, restZaak)
                }
                val zaakCreatedSlot = slot<Zaak>()
                val rolNatuurlijkPersoonSlot = slot<RolNatuurlijkPersoon>()
                val rolGroupSlotOrganisatorischeEenheidSlot = slot<RolOrganisatorischeEenheid>()
                verify(exactly = 1) {
                    ztcClientService.readZaaktype(restZaakAanmaakGegevens.zaak.zaaktype.uuid)
                    zgwApiService.createZaak(capture(zaakCreatedSlot))
                    zrcClientService.createRol(
                        capture(rolNatuurlijkPersoonSlot),
                        "Toegekend door de medewerker tijdens het behandelen van de zaak"
                    )
                    zrcClientService.updateRol(
                        zaak,
                        capture(rolGroupSlotOrganisatorischeEenheidSlot),
                        "Aanmaken zaak"
                    )
                    cmmnService.startCase(zaak, zaakType, zaakAfhandelParameters, null)
                    zrcClientService.createZaakobject(zaakObjectPand)
                    zrcClientService.createZaakobject(zaakObjectOpenbareRuimte)
                }
                with(zaakCreatedSlot.captured) {
                    assertEquals(this, zaak)
                }
                with(rolNatuurlijkPersoonSlot.captured) {
                    assertEquals(this.zaak, zaak.url)
                    assertEquals(this.betrokkeneType, BetrokkeneType.NATUURLIJK_PERSOON)
                }
                with(rolGroupSlotOrganisatorischeEenheidSlot.captured) {
                    assertEquals(this.zaak, rolOrganisatorischeEenheid.zaak)
                    assertEquals(this.betrokkeneType, BetrokkeneType.ORGANISATORISCHE_EENHEID)
                }
            }
        }
    }
    Given("a zaak exists, no one is assigned and zaak toekennen gegevens are provided") {
        clearAllMocks()
        val restZaakToekennenGegevens = createRESTZaakToekennenGegevens()
        val zaak = createZaak()
        val user = createLoggedInUser()
        val rolSlot = slot<Rol<*>>()
        val restZaak = createRESTZaak()
        val rolType = createRolType(omschrijvingGeneriek = RolType.OmschrijvingGeneriekEnum.BEHANDELAAR)
        val rolMedewerker = createRolMedewerker()

        every { zrcClientService.readZaak(restZaakToekennenGegevens.zaakUUID) } returns zaak
        every { zrcClientService.updateRol(zaak, capture(rolSlot), restZaakToekennenGegevens.reden) } just runs
        every { zgwApiService.findBehandelaarForZaak(zaak) } returns Optional.empty()
        every { identityService.readUser(restZaakToekennenGegevens.behandelaarGebruikersnaam) } returns user
        every { zgwApiService.findGroepForZaak(zaak) } returns Optional.empty()
        every { restZaakConverter.convert(zaak) } returns restZaak
        every { indexeerService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs
        every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker

        When("toekennen is called from user with access") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(toekennen = true)

            val returnedRestZaak = zakenRESTService.toekennen(restZaakToekennenGegevens)

            Then("the zaak is updated, and the zaken search index is updated") {
                assertEquals(returnedRestZaak, restZaak)
                verify(exactly = 1) {
                    zrcClientService.updateRol(zaak, any(), restZaakToekennenGegevens.reden)
                    indexeerService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                }
                with(rolSlot.captured) {
                    assertEquals(this.betrokkeneType, BetrokkeneType.MEDEWERKER)
                    assertEquals(
                        (this.betrokkeneIdentificatie as Medewerker).identificatie,
                        rolMedewerker.betrokkeneIdentificatie.identificatie
                    )
                    assertEquals(this.zaak, rolMedewerker.zaak)
                    assertEquals(this.omschrijving, rolType.omschrijving)
                }
            }
        }
    }
})
