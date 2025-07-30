/*
 * SPDX-FileCopyrightText: 2023 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.`object`.model.createORObject
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.ZaakInformatieobjectListParameters
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.ZaakafhandelParameterService.INADMISSIBLE_TERMINATION_ID
import net.atos.zac.admin.model.ZaakbeeindigParameter
import net.atos.zac.admin.model.ZaakbeeindigReden
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.documenten.model.OntkoppeldDocument
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createMedewerkerIdentificatie
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createOrganisatorischeEenheid
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoonForReads
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoonForReads
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.model.createZaakobjectOpenbareRuimte
import nl.info.client.zgw.model.createZaakobjectPand
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.DeleteGeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.AardRelatieEnum
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.policy.model.toRestZaakRechten
import nl.info.zac.app.zaak.ZaakRestService.Companion.AANVULLENDE_INFORMATIE_TASK_NAME
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.exception.BetrokkeneNotAllowedException
import nl.info.zac.app.zaak.exception.CommunicationChannelNotFound
import nl.info.zac.app.zaak.model.BetrokkeneIdentificatie
import nl.info.zac.app.zaak.model.RESTReden
import nl.info.zac.app.zaak.model.RESTZaakAfbrekenGegevens
import nl.info.zac.app.zaak.model.RESTZaakEditMetRedenGegevens
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestZaaktype
import nl.info.zac.app.zaak.model.ZAAK_TYPE_1_OMSCHRIJVING
import nl.info.zac.app.zaak.model.createRESTGeometry
import nl.info.zac.app.zaak.model.createRESTZaakAanmaakGegevens
import nl.info.zac.app.zaak.model.createRESTZaakAssignmentData
import nl.info.zac.app.zaak.model.createRESTZakenVerdeelGegevens
import nl.info.zac.app.zaak.model.createRestDocumentOntkoppelGegevens
import nl.info.zac.app.zaak.model.createRestGroup
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.app.zaak.model.createRestZaakInitiatorGegevens
import nl.info.zac.app.zaak.model.createRestZaakLinkData
import nl.info.zac.app.zaak.model.createRestZaakLocatieGegevens
import nl.info.zac.app.zaak.model.createRestZaakUnlinkData
import nl.info.zac.app.zaak.model.createRestZaaktype
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnProcessDefinition
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck
import nl.info.zac.history.ZaakHistoryService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.exception.UserNotInGroupException
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createOverigeRechtenAllDeny
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.productaanvraag.createProductaanvraagDimpact
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.test.date.toDate
import nl.info.zac.zaak.ZaakService
import nl.info.zac.zaak.exception.ZaakWithADecisionCannotBeTerminatedException
import org.apache.http.HttpStatus
import org.flowable.task.api.Task
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList", "LargeClass")
class ZaakRestServiceTest : BehaviorSpec({
    val decisionService = mockk<DecisionService>()
    val bpmnService = mockk<BpmnService>()
    val brcClientService = mockk<BrcClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val cmmnService = mockk<CMMNService>()
    val drcClientService = mockk<DrcClientService>()
    val eventingService = mockk<EventingService>()
    val healthCheckService = mockk<HealthCheckService>()
    val identityService = mockk<IdentityService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val indexingService = mockk<IndexingService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val objectsClientService = mockk<ObjectsClientService>()
    val ontkoppeldeDocumentenService = mockk<OntkoppeldeDocumentenService>()
    val opschortenZaakHelper = mockk<SuspensionZaakHelper>()
    val policyService = mockk<PolicyService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val restDecisionConverter = mockk<RestDecisionConverter>()
    val restZaakConverter = mockk<RestZaakConverter>()
    val restZaakOverzichtConverter = mockk<RestZaakOverzichtConverter>()
    val restZaaktypeConverter = mockk<RestZaaktypeConverter>()
    val zaakHistoryLineConverter = mockk<ZaakHistoryLineConverter>()
    val signaleringService = mockk<SignaleringService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zaakService = mockk<ZaakService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakHistoryService = mockk<ZaakHistoryService>()
    val testDispatcher = StandardTestDispatcher()
    val zaakRestService = ZaakRestService(
        decisionService = decisionService,
        cmmnService = cmmnService,
        identityService = identityService,
        inboxProductaanvraagService = inboxProductaanvraagService,
        loggedInUserInstance = loggedInUserInstance,
        objectsClientService = objectsClientService,
        policyService = policyService,
        productaanvraagService = productaanvraagService,
        restZaakConverter = restZaakConverter,
        zaakafhandelParameterService = zaakafhandelParameterService,
        zaakVariabelenService = zaakVariabelenService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        zaakService = zaakService,
        indexingService = indexingService,
        zaakHistoryLineConverter = zaakHistoryLineConverter,
        restDecisionConverter = restDecisionConverter,
        bpmnService = bpmnService,
        brcClientService = brcClientService,
        configuratieService = configuratieService,
        drcClientService = drcClientService,
        eventingService = eventingService,
        healthCheckService = healthCheckService,
        ontkoppeldeDocumentenService = ontkoppeldeDocumentenService,
        opschortenZaakHelper = opschortenZaakHelper,
        restZaakOverzichtConverter = restZaakOverzichtConverter,
        signaleringService = signaleringService,
        flowableTaskService = flowableTaskService,
        restZaaktypeConverter = restZaaktypeConverter,
        zaakHistoryService = zaakHistoryService,
        zgwApiService = zgwApiService,
        dispatcher = testDispatcher
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Zaak input data is provided") {
        val group = createGroup()
        val formulierData = mapOf(Pair("fakeKey", "fakeValue"))
        val objectRegistratieObject = createORObject()
        val productaanvraagDimpact = createProductaanvraagDimpact()
        val zaakRechten = createZaakRechten()
        val restZaak = createRestZaak()
        val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
        val zaakTypeUUID = zaakType.url.extractUuid()
        val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
            zaak = createRestZaak(
                restZaakType = RestZaaktype(
                    uuid = zaakTypeUUID
                ),
                restGroup = createRestGroup(
                    id = group.id
                ),

            ),
            zaakTypeUUID = zaakTypeUUID
        )
        val rolMedewerker = createRolMedewerker()
        val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid()
        val user = createLoggedInUser()
        val zaakAfhandelParameters = createZaakafhandelParameters()
        val zaakObjectPand = createZaakobjectPand()
        val zaakObjectOpenbareRuimte = createZaakobjectOpenbareRuimte()
        val zaak = createZaak(zaakTypeURI = zaakType.url)
        val bronOrganisatie = "fakeBronOrganisatie"
        val verantwoordelijkeOrganisatie = "fakeVerantwoordelijkeOrganisatie"
        val zaakCreatedSlot = slot<Zaak>()
        val updatedRolesSlot = mutableListOf<Rol<*>>()

        every { configuratieService.featureFlagBpmnSupport() } returns false
        every { configuratieService.readBronOrganisatie() } returns bronOrganisatie
        every { configuratieService.readVerantwoordelijkeOrganisatie() } returns verantwoordelijkeOrganisatie
        every { cmmnService.startCase(zaak, zaakType, zaakAfhandelParameters, null) } just runs
        every { identityService.readGroup(restZaakAanmaakGegevens.zaak.groep!!.id) } returns group
        every { identityService.readUser(restZaakAanmaakGegevens.zaak.behandelaar!!.id) } returns user
        every {
            inboxProductaanvraagService.delete(restZaakAanmaakGegevens.inboxProductaanvraag?.id)
        } just runs
        every { loggedInUserInstance.get() } returns createLoggedInUser()
        every {
            objectsClientService
                .readObject(restZaakAanmaakGegevens.inboxProductaanvraag?.productaanvraagObjectUUID)
        } returns objectRegistratieObject
        every { productaanvraagService.getAanvraaggegevens(objectRegistratieObject) } returns formulierData
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
        every { restZaakConverter.toRestZaak(zaak, zaakType, any()) } returns restZaak
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID)
        } returns zaakAfhandelParameters
        every { zaakVariabelenService.setZaakdata(zaak.uuid, formulierData) } just runs
        every { zgwApiService.createZaak(capture(zaakCreatedSlot)) } returns zaak
        every {
            zrcClientService.updateRol(
                any(),
                capture(updatedRolesSlot),
                any()
            )
        } just runs
        every { zrcClientService.createZaakobject(any<ZaakobjectPand>()) } returns zaakObjectPand
        every { zrcClientService.createZaakobject(any<ZaakobjectOpenbareRuimte>()) } returns zaakObjectOpenbareRuimte
        every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
        every { zaakService.bepaalRolGroep(group, zaak) } returns rolOrganisatorischeEenheid
        every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker
        every {
            zaakService.addInitiatorToZaak(
                identificationType = restZaak.initiatorIdentificatieType!!,
                identification = restZaak.initiatorIdentificatie!!,
                zaak = zaak,
                explanation = "Aanmaken zaak"
            )
        } just runs
        every { bpmnService.findProcessDefinitionForZaaktype(zaakTypeUUID) } returns null

        When("a zaaktype is created for which the user has permissions and no BPMN process definition is found") {
            every { policyService.readOverigeRechten() } returns createOverigeRechtenAllDeny(startenZaak = true)
            every {
                policyService.readZaakRechten(zaak, zaakType)
            } returns createZaakRechtenAllDeny(toevoegenInitiatorPersoon = true)

            val restZaakReturned = zaakRestService.createZaak(restZaakAanmaakGegevens)

            Then("a zaak is created using the ZGW API and a zaak is started in the ZAC CMMN service") {
                restZaakReturned shouldBe restZaak
                verify(exactly = 1) {
                    ztcClientService.readZaaktype(restZaakAanmaakGegevens.zaak.zaaktype.uuid)
                    zgwApiService.createZaak(any())
                    cmmnService.startCase(zaak, zaakType, zaakAfhandelParameters, null)
                }
                verify(exactly = 2) {
                    zrcClientService.createZaakobject(any())
                    zrcClientService.updateRol(
                        zaak,
                        any<Rol<*>>(),
                        "Aanmaken zaak"
                    )
                }
                updatedRolesSlot shouldHaveSize 2
                with(updatedRolesSlot[0]) {
                    betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                    omschrijving shouldBe rolOrganisatorischeEenheid.omschrijving
                    omschrijvingGeneriek shouldBe rolOrganisatorischeEenheid.omschrijvingGeneriek
                    roltoelichting shouldBe rolOrganisatorischeEenheid.roltoelichting
                }
                with(updatedRolesSlot[1]) {
                    betrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER
                    omschrijving shouldBe rolMedewerker.omschrijving
                    omschrijvingGeneriek shouldBe rolMedewerker.omschrijvingGeneriek
                    roltoelichting shouldBe rolMedewerker.roltoelichting
                }
            }
        }
    }

    Given("zaak input data has no communication channel") {
        val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
        val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
            zaak = createRestZaak(communicatiekanaal = null)
        )
        every { ztcClientService.readZaaktype(any<UUID>()) } returns zaakType
        every { policyService.readOverigeRechten() } returns createOverigeRechtenAllDeny(startenZaak = true)
        every { loggedInUserInstance.get() } returns createLoggedInUser()
        every { zaakafhandelParameterService.readZaakafhandelParameters(any()) } returns createZaakafhandelParameters()

        When("zaak creation is attempted") {
            val exception = shouldThrow<CommunicationChannelNotFound> {
                zaakRestService.createZaak(restZaakAanmaakGegevens)
            }

            Then("an exception is thrown") {
                exception.errorCode shouldNotBe null
            }
        }
    }

    Given("zaak input data has blank communication channel") {
        val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
        val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
            zaak = createRestZaak(communicatiekanaal = "      ")
        )
        every { ztcClientService.readZaaktype(any<UUID>()) } returns zaakType
        every { policyService.readOverigeRechten() } returns createOverigeRechtenAllDeny(startenZaak = true)
        every { loggedInUserInstance.get() } returns createLoggedInUser()
        every { zaakafhandelParameterService.readZaakafhandelParameters(any()) } returns createZaakafhandelParameters()

        When("zaak creation is attempted") {
            val exception = shouldThrow<CommunicationChannelNotFound> {
                zaakRestService.createZaak(restZaakAanmaakGegevens)
            }

            Then("an exception is thrown") {
                exception.errorCode shouldNotBe null
            }
        }
    }

    Given("a zaak exists, no user and no group are assigned and zaak assignment data is provided") {
        val restZaakToekennenGegevens = createRESTZaakAssignmentData()
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten()
        val user = createLoggedInUser()
        val rolSlot = mutableListOf<Rol<*>>()
        val restZaak = createRestZaak()
        val rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR)
        val rolMedewerker = createRolMedewerker()
        val group = createGroup()
        val rolGroup = createRolOrganisatorischeEenheid()

        every { zrcClientService.readZaak(restZaakToekennenGegevens.zaakUUID) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.updateRol(zaak, capture(rolSlot), restZaakToekennenGegevens.reason) } just runs
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns null
        every { identityService.readUser(restZaakToekennenGegevens.assigneeUserName!!) } returns user
        every { zgwApiService.findGroepForZaak(zaak) } returns null
        every { identityService.readGroup(restZaakToekennenGegevens.groupId) } returns group
        every { zaakService.bepaalRolGroep(group, zaak) } returns rolGroup
        every { restZaakConverter.toRestZaak(zaak, zaakType, any()) } returns restZaak
        every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs
        every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker

        When("the zaak is assigned to a user and a group") {
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = true)
            every {
                identityService.validateIfUserIsInGroup(
                    restZaakToekennenGegevens.assigneeUserName!!,
                    restZaakToekennenGegevens.groupId
                )
            } just runs

            val returnedRestZaak = zaakRestService.assignZaak(restZaakToekennenGegevens)

            Then("the zaak is assigned both to the group and the user, and the zaken search index is updated") {
                returnedRestZaak shouldBe restZaak
                verify(exactly = 2) {
                    zrcClientService.updateRol(zaak, any(), restZaakToekennenGegevens.reason)
                }
                verify(exactly = 1) {
                    indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                }
                with(rolSlot[0]) {
                    betrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER
                    with(betrokkeneIdentificatie as MedewerkerIdentificatie) {
                        identificatie shouldBe rolMedewerker.betrokkeneIdentificatie!!.identificatie
                    }
                    this.zaak shouldBe rolMedewerker.zaak
                    omschrijving shouldBe rolType.omschrijving
                }
                with(rolSlot[1]) {
                    betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                    with(betrokkeneIdentificatie as OrganisatorischeEenheidIdentificatie) {
                        identificatie shouldBe rolGroup.betrokkeneIdentificatie!!.identificatie
                    }
                    this.zaak shouldBe rolGroup.zaak
                    omschrijving shouldBe rolType.omschrijving
                }
            }
        }
    }

    Given("a zaak with no user and group assigned and zaak assignment data is provided") {
        val restZaakToekennenGegevensUnknownGroup = createRESTZaakAssignmentData(groepId = "unknown")
        val zaak = createZaak()
        val zaakType = createZaakType()

        every { zrcClientService.readZaak(restZaakToekennenGegevensUnknownGroup.zaakUUID) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = true)
        every {
            identityService.validateIfUserIsInGroup(
                restZaakToekennenGegevensUnknownGroup.assigneeUserName!!,
                restZaakToekennenGegevensUnknownGroup.groupId
            )
        } throws UserNotInGroupException()

        When("the zaak is assigned to an unknown group") {
            shouldThrow<UserNotInGroupException> {
                zaakRestService.assignZaak(restZaakToekennenGegevensUnknownGroup)
            }

            Then("an exception is thrown") {}
        }
    }

    Given("a zaak exists, with a user and group already assigned and zaak assignment data is provided") {
        val restZaakToekennenGegevens = createRESTZaakAssignmentData()
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten()
        val user = createLoggedInUser()
        val rolSlot = mutableListOf<Rol<*>>()
        val restZaak = createRestZaak()
        val rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR)
        val existingRolMedewerker = createRolMedewerker()
        val rolMedewerker = createRolMedewerker(
            zaakURI = zaak.url,
            medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "newUser")
        )
        val group = createGroup()
        val existingRolGroup = createRolOrganisatorischeEenheid()
        val rolGroup = createRolOrganisatorischeEenheid(
            zaakURI = zaak.url,
            organisatorischeEenheidIdentificatie = createOrganisatorischeEenheid(identificatie = "newGroup")
        )

        every { zrcClientService.readZaak(restZaakToekennenGegevens.zaakUUID) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every { zrcClientService.updateRol(zaak, capture(rolSlot), restZaakToekennenGegevens.reason) } just runs
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns existingRolMedewerker
        every { identityService.readUser(restZaakToekennenGegevens.assigneeUserName!!) } returns user
        every { zgwApiService.findGroepForZaak(zaak) } returns existingRolGroup
        every { identityService.readGroup(restZaakToekennenGegevens.groupId) } returns group
        every { zaakService.bepaalRolGroep(group, zaak) } returns rolGroup
        every { restZaakConverter.toRestZaak(zaak, zaakType, any()) } returns restZaak
        every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs
        every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker

        When("the zaak is assigned to a user and a group") {
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = true)
            every {
                identityService.validateIfUserIsInGroup(
                    restZaakToekennenGegevens.assigneeUserName!!,
                    restZaakToekennenGegevens.groupId
                )
            } just runs

            val returnedRestZaak = zaakRestService.assignZaak(restZaakToekennenGegevens)

            Then("the zaak is assigned both to the group and the user, and the zaken search index is updated") {
                returnedRestZaak shouldBe restZaak
                verify(exactly = 2) {
                    zrcClientService.updateRol(zaak, any(), restZaakToekennenGegevens.reason)
                }
                verify(exactly = 1) {
                    indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                }
                with(rolSlot[0]) {
                    betrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER
                    with(betrokkeneIdentificatie as MedewerkerIdentificatie) {
                        identificatie shouldBe rolMedewerker.betrokkeneIdentificatie!!.identificatie
                    }
                    this.zaak shouldBe rolMedewerker.zaak
                    omschrijving shouldBe rolType.omschrijving
                }
                with(rolSlot[1]) {
                    betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                    with(betrokkeneIdentificatie as OrganisatorischeEenheidIdentificatie) {
                        identificatie shouldBe rolGroup.betrokkeneIdentificatie!!.identificatie
                    }
                    this.zaak shouldBe rolGroup.zaak
                    omschrijving shouldBe rolType.omschrijving
                }
            }
        }
    }

    Given("REST zaken verdeel gegevens with a group and a user") {
        val zaakUUIDs = listOf(UUID.randomUUID(), UUID.randomUUID())
        val group = createGroup()
        val user = createUser()
        val restZakenVerdeelGegevens = createRESTZakenVerdeelGegevens(
            uuids = zaakUUIDs,
            groepId = group.id,
            behandelaarGebruikersnaam = user.id,
            reden = "fakeReason"
        )
        every { policyService.readWerklijstRechten() } returns createWerklijstRechten()
        every { zaakService.assignZaken(any(), any(), any(), any(), any()) } just runs
        every { identityService.readGroup(group.id) } returns group
        every { identityService.readUser(restZakenVerdeelGegevens.behandelaarGebruikersnaam!!) } returns user

        When("the assign zaken from a list function is called") {
            runTest(testDispatcher) {
                zaakRestService.assignFromList(restZakenVerdeelGegevens)
            }

            Then("the zaken are assigned to the group and user") {
                verify(exactly = 1) {
                    zaakService.assignZaken(
                        zaakUUIDs,
                        group,
                        user,
                        restZakenVerdeelGegevens.reden,
                        restZakenVerdeelGegevens.screenEventResourceId
                    )
                }
            }
        }
    }

    Given("Two open zaken with zaak link data using a 'bijdrage' relatie and in reverse an 'onderwerp' relatie") {
        val zaak = createZaak()
        val teKoppelenZaak = createZaak()
        val restZaakLinkData = createRestZaakLinkData(
            zaakUuid = zaak.uuid,
            teKoppelenZaakUuid = teKoppelenZaak.uuid,
            relatieType = RelatieType.BIJDRAGE,
            reverseRelatieType = RelatieType.ONDERWERP
        )
        val patchZaakUUIDSlot = mutableListOf<UUID>()
        val patchZaakSlot = mutableListOf<Zaak>()
        every { zrcClientService.readZaak(restZaakLinkData.zaakUuid) } returns zaak
        every { zrcClientService.readZaak(restZaakLinkData.teKoppelenZaakUuid) } returns teKoppelenZaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
        every { policyService.readZaakRechten(teKoppelenZaak) } returns createZaakRechten()
        every { zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot)) } returns zaak

        When("the zaken are linked") {
            zaakRestService.linkZaak(restZaakLinkData)

            Then("the two zaken are successfully linked") {
                verify(exactly = 2) {
                    zrcClientService.patchZaak(any(), any())
                }
                patchZaakUUIDSlot[0] shouldBe zaak.uuid
                patchZaakUUIDSlot[1] shouldBe teKoppelenZaak.uuid
                with(patchZaakSlot[0]) {
                    relevanteAndereZaken shouldHaveSize(1)
                    with(relevanteAndereZaken[0]) {
                        url shouldBe teKoppelenZaak.url
                        aardRelatie shouldBe AardRelatieEnum.BIJDRAGE
                    }
                }
                with(patchZaakSlot[1]) {
                    relevanteAndereZaken shouldHaveSize(1)
                    with(relevanteAndereZaken[0]) {
                        url shouldBe zaak.url
                        aardRelatie shouldBe AardRelatieEnum.ONDERWERP
                    }
                }
            }
        }
    }

    Given("Two open zaken with zaak link data using a 'hoofdzaak' relatie and no reverse relation") {
        val zaak = createZaak()
        val teKoppelenZaak = createZaak()
        val restZaakLinkData = createRestZaakLinkData(
            zaakUuid = zaak.uuid,
            teKoppelenZaakUuid = teKoppelenZaak.uuid,
            relatieType = RelatieType.HOOFDZAAK
        )
        val patchZaakUUIDSlot = slot<UUID>()
        val patchZaakSlot = slot<Zaak>()
        every { zrcClientService.readZaak(restZaakLinkData.zaakUuid) } returns zaak
        every { zrcClientService.readZaak(restZaakLinkData.teKoppelenZaakUuid) } returns teKoppelenZaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
        every { policyService.readZaakRechten(teKoppelenZaak) } returns createZaakRechten()
        every { zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot)) } returns zaak
        every { indexingService.addOrUpdateZaak(teKoppelenZaak.uuid, false) } just runs
        every { eventingService.send(any<ScreenEvent>()) } just runs

        When("the zaken are linked") {
            zaakRestService.linkZaak(restZaakLinkData)

            Then("the two zaken are successfully linked, the index is updated and a screen event is sent") {
                verify(exactly = 1) {
                    zrcClientService.patchZaak(any(), any())
                }
                patchZaakUUIDSlot.captured shouldBe zaak.uuid
                with(patchZaakSlot.captured) {
                    hoofdzaak shouldBe teKoppelenZaak.url
                }
            }
        }
    }

    Given("An open zaak and a closed zaak") {
        val zaak = createZaak()
        val teKoppelenZaak = createZaak()
        val restZaakLinkData = createRestZaakLinkData(
            zaakUuid = zaak.uuid,
            teKoppelenZaakUuid = teKoppelenZaak.uuid,
            relatieType = RelatieType.HOOFDZAAK
        )

        every { zrcClientService.readZaak(restZaakLinkData.zaakUuid) } returns zaak
        every { zrcClientService.readZaak(restZaakLinkData.teKoppelenZaakUuid) } returns teKoppelenZaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
        every { policyService.readZaakRechten(teKoppelenZaak) } returns createZaakRechten()

        val patchZaakUUIDSlot = slot<UUID>()
        val patchZaakSlot = slot<Zaak>()
        every {
            zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot))
        } returns zaak
        every { indexingService.addOrUpdateZaak(teKoppelenZaak.uuid, false) } just runs
        every { eventingService.send(any<ScreenEvent>()) } just runs

        When("the zaken are linked") {
            zaakRestService.linkZaak(restZaakLinkData)

            Then("the two zaken are successfully linked, the index is updated and a screen event is sent") {
                verify(exactly = 1) {
                    zrcClientService.patchZaak(any(), any())
                }
                patchZaakUUIDSlot.captured shouldBe zaak.uuid
                with(patchZaakSlot.captured) {
                    hoofdzaak shouldBe teKoppelenZaak.url
                }
            }
        }
    }

    Given("Two linked zaken with the relation 'vervolg'") {
        val zaak = createZaak()
        val gekoppeldeZaak = createZaak()
        val restZaakLinkData = createRestZaakUnlinkData(
            zaakUuid = zaak.uuid,
            gekoppeldeZaakIdentificatie = gekoppeldeZaak.identificatie,
            relationType = RelatieType.VERVOLG,
            reason = "fakeUnlinkReason"
        )
        val patchZaakUUIDSlot = slot<UUID>()
        val patchZaakSlot = slot<Zaak>()
        every { zrcClientService.readZaak(restZaakLinkData.zaakUuid) } returns zaak
        every { zrcClientService.readZaakByID(restZaakLinkData.gekoppeldeZaakIdentificatie) } returns gekoppeldeZaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
        every { policyService.readZaakRechten(gekoppeldeZaak) } returns createZaakRechten()
        every {
            zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot), "fakeUnlinkReason")
        } returns zaak

        When("the zaken are unlinked") {
            zaakRestService.unlinkZaak(restZaakLinkData)

            Then("the two zaken are successfully unlinked") {
                verify(exactly = 1) {
                    zrcClientService.patchZaak(any(), any(), any())
                }
                patchZaakUUIDSlot.captured shouldBe zaak.uuid
                with(patchZaakSlot.captured) {
                    relevanteAndereZaken shouldBe null
                }
            }
        }
    }

    Given("a zaak with tasks exists and zaak and tasks have final date set") {
        val changeDescription = "change description"
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten()
        val newZaakFinalDate = zaak.uiterlijkeEinddatumAfdoening.minusDays(10)
        val restZaak = createRestZaak(uiterlijkeEinddatumAfdoening = newZaakFinalDate)
        val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaak, changeDescription)
        val patchedZaak = createZaak(uiterlijkeEinddatumAfdoening = newZaakFinalDate)
        val task = mockk<Task>()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every { zrcClientService.patchZaak(zaak.uuid, any(), changeDescription) } returns patchedZaak
        every { flowableTaskService.listOpenTasksForZaak(zaak.uuid) } returns listOf(task, task, task)
        every { task.dueDate } returns zaak.uiterlijkeEinddatumAfdoening.toDate()
        every { task.name } returnsMany listOf("fakeTask", AANVULLENDE_INFORMATIE_TASK_NAME, "another task")
        every { task.dueDate = newZaakFinalDate.toDate() } just runs
        every { flowableTaskService.updateTask(task) } returns task
        every { task.id } returns "id"
        every { eventingService.send(any<ScreenEvent>()) } just runs
        every { restZaakConverter.toRestZaak(patchedZaak, zaakType, zaakRechten) } returns restZaak
        every { identityService.validateIfUserIsInGroup(restZaak.behandelaar!!.id, restZaak.groep!!.id) } just runs
        every { zaakafhandelParameterService.readZaakafhandelParameters(any()) } returns createZaakafhandelParameters()

        When("zaak final date is set to a later date") {
            val updatedRestZaak = zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)

            Then("zaak is updated with the new data") {
                with(updatedRestZaak) {
                    uiterlijkeEinddatumAfdoening shouldBe newZaakFinalDate
                }
            }

            And("tasks final date are shifted accordingly") {
                verify(exactly = 2) {
                    task.dueDate = newZaakFinalDate.toDate()
                }
            }

            And("screen event signals are sent") {
                verify(exactly = 3) {
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }

    Given("a zaak and user not part of any group") {
        val changeDescription = "change description"
        val zaak = createZaak()
        val zaakType = createZaakType()
        val restZaak = createRestZaak()
        val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaak, changeDescription)

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
        every { identityService.validateIfUserIsInGroup(any(), any()) } throws InputValidationFailedException()
        every { zaakafhandelParameterService.readZaakafhandelParameters(any()) } returns createZaakafhandelParameters()

        When("zaak update is requested") {
            shouldThrow<InputValidationFailedException> {
                zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)
            }

            Then("exception is thrown") {}
        }
    }

    Given("no verlengenDoorlooptijd policy") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten(verlengenDoorlooptijd = false)
        val newZaakFinalDate = zaak.uiterlijkeEinddatumAfdoening.minusDays(10)
        val restZaak = createRestZaak(uiterlijkeEinddatumAfdoening = newZaakFinalDate, einddatumGepland = null)
        val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaak, "change description")

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every { zaakafhandelParameterService.readZaakafhandelParameters(any()) } returns createZaakafhandelParameters()

        When("zaak update is requested with a new final date") {
            val exception = shouldThrow<PolicyException> {
                zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)
            }

            Then("it fails") {
                exception.message shouldBe null
            }
        }

        When("zaak update is requested without a new final date") {
            val zaakWithoutDateChange = restZaak.copy(uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening)
            val zaakRechten = createZaakRechten()
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every { identityService.validateIfUserIsInGroup(any(), any()) } just runs
            every { restZaakConverter.toRestZaak(any(), zaakType, zaakRechten) } returns zaakWithoutDateChange
            every { zrcClientService.patchZaak(zaak.uuid, any(), any()) } returns zaak

            zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens.copy(zaak = zaakWithoutDateChange))

            Then("it succeeds") {
                verify(exactly = 1) {
                    zrcClientService.patchZaak(zaak.uuid, any(), any())
                }
            }
        }
    }

    Given("no wijzigenDoorlooptijd policy") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten(wijzigenDoorlooptijd = false)
        val newZaakFinalDate = zaak.uiterlijkeEinddatumAfdoening.minusDays(10)
        val restZaak = createRestZaak(uiterlijkeEinddatumAfdoening = newZaakFinalDate, einddatumGepland = null)
        val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaak, "change description")

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every { zaakafhandelParameterService.readZaakafhandelParameters(any()) } returns createZaakafhandelParameters()

        When("zaak update is requested with a new final date") {
            val exception = shouldThrow<PolicyException> {
                zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)
            }

            Then("it fails") {
                exception.message shouldBe null
            }
        }

        When("zaak update is requested without a new final date") {
            val zaakWithoutDateChange = restZaak.copy(uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening)
            val zaakRechten = createZaakRechten()
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every { identityService.validateIfUserIsInGroup(any(), any()) } just runs
            every { restZaakConverter.toRestZaak(any(), zaakType, zaakRechten) } returns zaakWithoutDateChange
            every { zrcClientService.patchZaak(zaak.uuid, any(), any()) } returns zaak

            zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens.copy(zaak = zaakWithoutDateChange))

            Then("it succeeds") {
                verify(exactly = 1) {
                    zrcClientService.patchZaak(zaak.uuid, any(), any())
                }
            }
        }
    }

    Given("An existing zaak") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten()
        val restGeometry = createRESTGeometry()
        val reason = "fakeReason"
        val restZaakLocatieGegevens = createRestZaakLocatieGegevens(
            restGeometry = restGeometry,
            reason = reason
        )
        val updatedZaak = createZaak()
        val updatedRestZaak = createRestZaak()
        val patchZaakSlot = slot<Zaak>()
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.patchZaak(zaak.uuid, capture(patchZaakSlot), reason) } returns updatedZaak
        every { restZaakConverter.toRestZaak(updatedZaak, zaakType, zaakRechten) } returns updatedRestZaak

        When("a zaak location is added to the zaak") {
            val restZaak = zaakRestService.updateZaakLocatie(zaak.uuid, restZaakLocatieGegevens)

            Then("the zaak is updated correctly") {
                verify(exactly = 1) {
                    zrcClientService.patchZaak(zaak.uuid, any(), reason)
                }
                restZaak shouldBe updatedRestZaak
                with(patchZaakSlot.captured) {
                    zaakgeometrie.shouldBeInstanceOf<GeoJSONGeometry>()
                    with(zaakgeometrie as GeoJSONGeometry) {
                        coordinates[0].toDouble() shouldBe restGeometry.point!!.longitude
                        coordinates[1].toDouble() shouldBe restGeometry.point!!.latitude
                    }
                }
            }
        }
    }

    Given("An existing zaak with a zaak location") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten()
        val reason = "fakeReasonForDeletion"
        val restZaakLocatieGegevens = createRestZaakLocatieGegevens(
            restGeometry = null,
            reason = reason
        )
        val updatedZaak = createZaak()
        val updatedRestZaak = createRestZaak()
        val patchZaakSlot = slot<Zaak>()
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.patchZaak(zaak.uuid, capture(patchZaakSlot), reason) } returns updatedZaak
        every { restZaakConverter.toRestZaak(updatedZaak, zaakType, zaakRechten) } returns updatedRestZaak

        When("the zaak location is deleted") {
            val restZaak = zaakRestService.updateZaakLocatie(zaak.uuid, restZaakLocatieGegevens)

            Then("the zaak is updated correctly") {
                verify(exactly = 1) {
                    zrcClientService.patchZaak(zaak.uuid, any(), reason)
                }
                restZaak shouldBe updatedRestZaak
                with(patchZaakSlot.captured) {
                    zaakgeometrie.shouldBeInstanceOf<DeleteGeoJSONGeometry>()
                }
            }
        }
    }

    Given("A zaak with an initiator and rest zaak betrokkene gegevens") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten(toevoegenInitiatorPersoon = true)
        val indentification = "123456677"
        val restZaakInitiatorGegevens = createRestZaakInitiatorGegevens()
        val rolMedewerker = createRolMedewerker()
        val restZaak = createRestZaak()
        every { zrcClientService.readZaak(restZaakInitiatorGegevens.zaakUUID) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolMedewerker
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every { zrcClientService.deleteRol(any(), any()) } just runs
        every { zaakService.addInitiatorToZaak(any(), any(), any(), any()) } just runs
        every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten) } returns restZaak

        When("an initiator is updated") {
            val updatedRestZaak = zaakRestService.updateInitiator(restZaakInitiatorGegevens)

            Then("the old initiator should be removed and the new one should be added to the zaak") {
                updatedRestZaak shouldBe restZaak
                verify(exactly = 1) {
                    zrcClientService.deleteRol(
                        rolMedewerker,
                        "Verwijderd door de medewerker tijdens het behandelen van de zaak"
                    )
                    zaakService.addInitiatorToZaak(
                        IdentificatieType.BSN,
                        indentification,
                        zaak,
                        restZaakInitiatorGegevens.toelichting!!
                    )
                }
            }
        }
    }

    Given("A zaak with an initiator") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten(toevoegenInitiatorPersoon = true)
        val rolMedewerker = createRolMedewerker()
        val restZaak = createRestZaak()
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolMedewerker
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every { zrcClientService.deleteRol(any(), any()) } just runs
        every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten) } returns restZaak

        When("the initiator is deleted") {
            val updatedRestZaak = zaakRestService.deleteInitiator(zaak.uuid, RESTReden("fake reason"))

            Then("the initiator should be removed from the zaak") {
                updatedRestZaak shouldBe restZaak
                verify(exactly = 1) {
                    zrcClientService.deleteRol(rolMedewerker, "fake reason")
                }
            }
        }
    }

    Given("A zaak and no managed zaakbeeindigreden") {
        val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
        val zaakTypeUUID = zaakType.url.extractUuid()
        val zaak = createZaak(zaakTypeURI = zaakType.url)
        val zaakAfhandelParameters = createZaakafhandelParameters()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten(afbreken = true)
        every { zaakService.checkZaakAfsluitbaar(zaak) } just runs
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID)
        } returns zaakAfhandelParameters
        every {
            zgwApiService.createResultaatForZaak(
                zaak,
                zaakAfhandelParameters.nietOntvankelijkResultaattype,
                "Zaak is niet ontvankelijk"
            )
        } just runs
        every { zgwApiService.endZaak(zaak, "Zaak is niet ontvankelijk") } just runs
        every { cmmnService.terminateCase(zaak.uuid) } returns Unit

        When("aborted with the hardcoded 'niet ontvankelijk' zaakbeeindigreden") {
            zaakRestService.terminateZaak(
                zaak.uuid,
                RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = INADMISSIBLE_TERMINATION_ID)
            )

            Then("it is ended with result") {
                verify(exactly = 1) {
                    zgwApiService.createResultaatForZaak(
                        zaak,
                        zaakAfhandelParameters.nietOntvankelijkResultaattype,
                        "Zaak is niet ontvankelijk"
                    )
                    zgwApiService.endZaak(zaak, "Zaak is niet ontvankelijk")
                    cmmnService.terminateCase(zaak.uuid)
                }
            }
        }
    }

    Given("A zaak with a decision cannot be terminated. A bad request is returned") {
        val zaakUuid = UUID.randomUUID()
        val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
        val zaak = createZaak(
            uuid = zaakUuid,
            zaakTypeURI = zaakType.url,
            resultaat = URI("https://example.com/${UUID.randomUUID()}")
        )

        every { zrcClientService.readZaak(zaakUuid) } returns zaak

        shouldThrow<ZaakWithADecisionCannotBeTerminatedException> {
            zaakRestService.terminateZaak(
                zaakUuid,
                RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = INADMISSIBLE_TERMINATION_ID)
            )
        }

        verify(exactly = 0) {
            zgwApiService.createResultaatForZaak(any(), any<UUID>(), any())
            zgwApiService.endZaak(any<Zaak>(), any())
            cmmnService.terminateCase(any())
        }
    }

    Given("A zaak and managed zaakbeeindigreden") {
        val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
        val zaakTypeUUID = zaakType.url.extractUuid()
        val zaak = createZaak(zaakTypeURI = zaakType.url)
        val resultTypeUUID = UUID.randomUUID()
        val zaakAfhandelParameters = createZaakafhandelParameters(
            zaakbeeindigParameters = setOf(
                ZaakbeeindigParameter().apply {
                    id = 123
                    resultaattype = resultTypeUUID
                    zaakbeeindigReden = ZaakbeeindigReden().apply {
                        id = -2
                        naam = "-2 name"
                    }
                }
            )
        )

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten(afbreken = true)
        every { zaakService.checkZaakAfsluitbaar(zaak) } just runs
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID)
        } returns zaakAfhandelParameters
        every { zgwApiService.createResultaatForZaak(zaak, resultTypeUUID, "-2 name") } just runs
        every { zgwApiService.endZaak(zaak, "-2 name") } just runs
        every { cmmnService.terminateCase(zaak.uuid) } returns Unit

        When("aborted with managed zaakbeeindigreden") {
            zaakRestService.terminateZaak(zaak.uuid, RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = "-2"))

            Then("it is ended with result") {
                verify(exactly = 1) {
                    zgwApiService.createResultaatForZaak(zaak, resultTypeUUID, "-2 name")
                    zgwApiService.endZaak(zaak, "-2 name")
                    cmmnService.terminateCase(zaak.uuid)
                }
            }
        }

        When("aborted with invalid zaakbeeindigreden id") {
            val exception = shouldThrow<IllegalArgumentException> {
                zaakRestService.terminateZaak(zaak.uuid, RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = "not a number"))
            }

            Then("it throws an error") {
                exception.message shouldBe "For input string: \"not a number\""
            }
        }
    }

    Given(
        """
        Two existing zaaktypes in the configured catalogue for which the logged in user is authorised
        and which are valid on the current date
        """
    ) {
        val defaultCatalogueURI = URI("http://example.com/fakeCatalogue")
        val now = LocalDate.now()
        val zaaktypes = listOf(
            createZaakType(
                omschrijving = "Zaaktype 1",
                identification = "ZAAKTYPE1",
                beginGeldigheid = now.minusDays(1)
            ),
            createZaakType(
                omschrijving = "Zaaktype 2",
                identification = "ZAAKTYPE2",
                beginGeldigheid = now.minusDays(2),
                eindeGeldigheid = now.plusDays(1)
            )
        )
        val restZaaktypes = listOf(createRestZaaktype(), createRestZaaktype())
        val loggedInUser = createLoggedInUser(
            zaakTypes = zaaktypes.map { it.omschrijving }.toSet()
        )
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck()
        every { configuratieService.readDefaultCatalogusURI() } returns defaultCatalogueURI
        every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns zaaktypes
        every { loggedInUserInstance.get() } returns loggedInUser
        zaaktypes.forEach {
            every { healthCheckService.controleerZaaktype(it.url) } returns zaaktypeInrichtingscheck
            every { restZaaktypeConverter.convert(it) } returns restZaaktypes[zaaktypes.indexOf(it)]
        }
        And("BPMN support is disabled") {
            every { configuratieService.featureFlagBpmnSupport() } returns false

            When("the zaaktypes are requested") {
                val returnedRestZaaktypes = zaakRestService.listZaaktypes()

                Then("the zaaktypes are returned for which the user is authorised") {
                    verify(exactly = 1) {
                        ztcClientService.listZaaktypen(defaultCatalogueURI)
                    }
                    returnedRestZaaktypes shouldHaveSize 2
                    returnedRestZaaktypes shouldBe restZaaktypes
                }
            }
        }
    }

    Given(
        """
        Two existing zaaktypes in the configured catalogue for which the logged in user is authorised
        and which are valid on the current date and of which the first zaaktype has valid zaakafhandelparameters
        """
    ) {
        val defaultCatalogueURI = URI("http://example.com/fakeCatalogue")
        val now = LocalDate.now()
        val zaakType1UUID = UUID.randomUUID()
        val zaakType2UUID = UUID.randomUUID()
        val zaaktypes = listOf(
            createZaakType(
                omschrijving = "Zaaktype 1",
                identification = "ZAAKTYPE1",
                uri = URI("https://example.com/zaaktypes/$zaakType1UUID"),
                beginGeldigheid = now.minusDays(1)
            ),
            createZaakType(
                omschrijving = "Zaaktype 2",
                identification = "ZAAKTYPE2",
                uri = URI("https://example.com/zaaktypes/$zaakType2UUID"),
                beginGeldigheid = now.minusDays(1)
            )
        )
        val restZaaktypes = listOf(createRestZaaktype(), createRestZaaktype())
        val loggedInUser = createLoggedInUser(
            zaakTypes = zaaktypes.map { it.omschrijving }.toSet()
        )
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck()
        every { configuratieService.readDefaultCatalogusURI() } returns defaultCatalogueURI
        every { configuratieService.featureFlagBpmnSupport() } returns true
        every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns zaaktypes
        every { loggedInUserInstance.get() } returns loggedInUser
        every { healthCheckService.controleerZaaktype(zaaktypes[0].url) } returns zaaktypeInrichtingscheck
        zaaktypes.forEach {
            every { restZaaktypeConverter.convert(it) } returns restZaaktypes[zaaktypes.indexOf(it)]
        }

        And(
            "BPMN support is enabled and a BPMN process definition exists for the second but not for the first zaaktype"
        ) {
            every { configuratieService.featureFlagBpmnSupport() } returns true
            every { bpmnService.findProcessDefinitionForZaaktype(zaakType1UUID) } returns null
            every { bpmnService.findProcessDefinitionForZaaktype(zaakType2UUID) } returns createZaaktypeBpmnProcessDefinition()

            When("the zaaktypes are requested") {
                val returnedRestZaaktypes = zaakRestService.listZaaktypes()

                Then("the zaaktypes are returned for which the user is authorised") {
                    verify(exactly = 1) {
                        ztcClientService.listZaaktypen(defaultCatalogueURI)
                    }
                    verify(exactly = 2) {
                        bpmnService.findProcessDefinitionForZaaktype(any())
                    }
                    returnedRestZaaktypes shouldHaveSize 2
                    returnedRestZaaktypes shouldBe restZaaktypes
                }
            }
        }
    }

    Given("Rest zaak data") {
        val restZaakUpdate = createRestZaak()
        val zaak = createZaak()
        val zaakdataMap = slot<Map<String, Any>>()
        every { zrcClientService.readZaak(restZaakUpdate.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
        every { zaakVariabelenService.setZaakdata(restZaakUpdate.uuid, capture(zaakdataMap)) } just runs

        When("the zaakdata is requested to be updated") {
            val updatedRestZaak = zaakRestService.updateZaakdata(restZaakUpdate)

            Then("the zaakdata is correctly updated") {
                verify(exactly = 1) {
                    zaakVariabelenService.setZaakdata(restZaakUpdate.uuid, any())
                }
                updatedRestZaak shouldBe restZaakUpdate
                zaakdataMap.captured shouldBe restZaakUpdate.zaakdata
            }
        }
    }

    Given("A zaak for which signaleringen exist") {
        val zaakUUID = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUUID)
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten(lezen = true)
        val restZaak = createRestZaak(
            uuid = zaakUUID,
            rechten = zaakRechten.toRestZaakRechten()
        )
        every { zrcClientService.readZaak(zaakUUID) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten) } returns restZaak
        every { signaleringService.deleteSignaleringenForZaak(zaak) } returns 1

        When("the zaak is read") {
            val returnedRestZaak = zaakRestService.readZaak(zaakUUID)

            Then("the zaak is returned and any zaak signaleringen are deleted") {
                returnedRestZaak shouldBe restZaak
                verify(exactly = 1) {
                    signaleringService.deleteSignaleringenForZaak(zaak)
                }
            }
        }
    }

    Given("An existing BPMN process diagram for a given zaak UUID") {
        val uuid = UUID.randomUUID()
        every { bpmnService.getProcessDiagram(uuid) } returns ByteArrayInputStream("fakeDiagram".toByteArray())

        When("the process diagram is requested") {
            val response = zaakRestService.downloadProcessDiagram(uuid)

            Then(
                "a HTTP OK response is returned with a 'Content-Disposition' HTTP header and the diagram as input stream"
            ) {
                with(response) {
                    status shouldBe HttpStatus.SC_OK
                    headers["Content-Disposition"]!![0] shouldBe """attachment; filename="procesdiagram.gif"""".trimIndent()
                    (entity as InputStream).bufferedReader().use { it.readText() } shouldBe "fakeDiagram"
                }
            }
        }
    }

    Given("A initiator is posted on a zaak create with an initiator") {
        val zaakUUID = UUID.randomUUID()

        When("This is not allowed in the zaak afhandel parameters") {
            val betrokkeneKoppelingen = createBetrokkeneKoppelingen(
                brpKoppelen = false,
                zaakafhandelParameters = createZaakafhandelParameters()
            )
            val zaakafhandelParameters = createZaakafhandelParameters(betrokkeneKoppelingen = betrokkeneKoppelingen)
            val zaakType = createZaakType()

            val zaak = createRestZaak(uuid = zaakUUID, initiatorIdentificatieType = IdentificatieType.BSN)
            val zaakAanmaakGegevens = createRESTZaakAanmaakGegevens(zaak = zaak)

            every { ztcClientService.readZaaktype(zaak.zaaktype.uuid) } returns zaakType
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(zaak.zaaktype.uuid)
            } returns zaakafhandelParameters

            val exception = shouldThrow<BetrokkeneNotAllowedException> {
                zaakRestService.createZaak(zaakAanmaakGegevens)
            }

            Then("An error should be thrown") {
                exception.errorCode shouldBe ErrorCode.ERROR_CODE_CASE_BETROKKENE_NOT_ALLOWED
            }
        }
    }

    Given("A zaak with a zaakinformatieobject where the corresponding informatieobject is only linked to this zaak") {
        val zaakUUID = UUID.randomUUID()
        val informatieobjectUUID = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUUID)
        val enkelvoudiginformatieobject = createEnkelvoudigInformatieObject(uuid = informatieobjectUUID)
        val zaakinformatiebject = createZaakInformatieobjectForReads(
            uuid = informatieobjectUUID
        )
        val restOntkoppelGegevens = createRestDocumentOntkoppelGegevens(
            zaakUUID = zaakUUID,
            documentUUID = informatieobjectUUID,
            reden = "veryFakeReason"
        )
        every { zrcClientService.readZaak(zaakUUID) } returns zaak
        every { drcClientService.readEnkelvoudigInformatieobject(informatieobjectUUID) } returns enkelvoudiginformatieobject
        every { policyService.readDocumentRechten(enkelvoudiginformatieobject, zaak).ontkoppelen } returns true
        every {
            zrcClientService.listZaakinformatieobjecten(any<ZaakInformatieobjectListParameters>())
        } returns listOf(zaakinformatiebject)
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudiginformatieobject) } returns emptyList()
        every {
            zrcClientService.deleteZaakInformatieobject(zaakinformatiebject.uuid, "veryFakeReason", "Ontkoppeld")
        } just Runs
        every { indexingService.removeInformatieobject(informatieobjectUUID) } just Runs
        every {
            ontkoppeldeDocumentenService.create(enkelvoudiginformatieobject, zaak, "veryFakeReason")
        } returns mockk<OntkoppeldDocument>()

        When("a request is done to unlink the zaakinformatieobject from the zaak") {
            zaakRestService.ontkoppelInformatieObject(restOntkoppelGegevens)

            Then(
                """
                    the zaakinformatieobject is unlinked from the zaak and the related informatieobject is removed from the search index 
                    and is added as an inboxdocument
                """.trimIndent()
            ) {
                verify(exactly = 1) {
                    zrcClientService.deleteZaakInformatieobject(
                        zaakinformatiebject.uuid,
                        "veryFakeReason",
                        "Ontkoppeld"
                    )
                    indexingService.removeInformatieobject(informatieobjectUUID)
                    ontkoppeldeDocumentenService.create(enkelvoudiginformatieobject, zaak, "veryFakeReason")
                }
            }
        }
    }

    Given("A zaak without an initiator") {
        val kvkNummer = "1234567"
        val vestigingsnummer = "00012352546"
        val restZaakInitiatorGegevens = createRestZaakInitiatorGegevens(
            betrokkeneIdentificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.VN,
                kvkNummer = kvkNummer,
                vestigingsnummer = vestigingsnummer
            )
        )
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten()

        every { zrcClientService.readZaak(restZaakInitiatorGegevens.zaakUUID) } returns zaak
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zgwApiService.findInitiatorRoleForZaak(any()) } returns null
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every {
            zaakService.addInitiatorToZaak(
                IdentificatieType.VN,
                "$kvkNummer|$vestigingsnummer",
                zaak,
                any()
            )
        } just runs
        every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten) } returns createRestZaak()

        When("the initiator is updated with an explanation") {
            zaakRestService.updateInitiator(
                restZaakInitiatorGegevens.apply {
                    toelichting = "test reden"
                }
            )

            Then("the explanation should get saved") {
                verify(exactly = 1) {
                    zaakService.addInitiatorToZaak(
                        IdentificatieType.VN,
                        "$kvkNummer|$vestigingsnummer",
                        any(),
                        "test reden"
                    )
                }
            }
        }

        When("the initiator is updated without an explanation") {
            zaakRestService.updateInitiator(
                restZaakInitiatorGegevens = restZaakInitiatorGegevens.apply {
                    toelichting = null
                }
            )

            Then("the reason should be set to the default") {
                verify(exactly = 1) {
                    zaakService.addInitiatorToZaak(
                        IdentificatieType.VN,
                        "$kvkNummer|$vestigingsnummer",
                        any(),
                        "Toegekend door de medewerker tijdens het behandelen van de zaak"
                    )
                }
            }
        }
    }

    Given(
        """
            A zaak with a betrokkene of type natuurlijk persoon, a betrokkene of type niet-natuurlijk persoon
            with a vestigingsnummer, a betrokkene of type niet-natuurlijk persoon with a RSIN (=INN NNP ID),
            and a betrokkene without a betrokkene identification.
            """
    ) {
        val zaak = createZaak()
        val rolNatuurlijkPersoon = createRolNatuurlijkPersoonForReads()
        val rolNietNatuurlijkPersoonWithVestigingsnummer = createRolNietNatuurlijkPersoonForReads(
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                vestigingsnummer = "fakeVestigingsNummer"
            )
        )
        val rolNietNatuurlijkPersoonWithRSIN = createRolNietNatuurlijkPersoonForReads(
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                innNnpId = "fakeInnNnpId"
            )
        )
        val rolNatuurlijkPersoonWithoutIdentificatie = createRolNatuurlijkPersoonForReads(
            natuurlijkPersoonIdentificatie = null
        )
        val betrokkeneRoles = listOf(
            rolNatuurlijkPersoon,
            rolNietNatuurlijkPersoonWithVestigingsnummer,
            rolNietNatuurlijkPersoonWithRSIN,
            rolNatuurlijkPersoonWithoutIdentificatie
        )
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
        every { zaakService.listBetrokkenenforZaak(zaak) } returns betrokkeneRoles

        When("the betrokkenen are retrieved") {
            val returnedBetrokkenen = zaakRestService.listBetrokkenenVoorZaak(zaak.uuid)

            Then("the betrokkenen are correctly returned except the betrokkene without identification") {
                with(returnedBetrokkenen) {
                    size shouldBe 3
                    with(first()) {
                        rolid shouldBe rolNatuurlijkPersoon.uuid.toString()
                        roltype shouldBe rolNatuurlijkPersoon.omschrijving
                        roltoelichting shouldBe rolNatuurlijkPersoon.roltoelichting
                        type shouldBe "NATUURLIJK_PERSOON"
                        identificatie shouldBe rolNatuurlijkPersoon.identificatienummer
                        identificatieType shouldBe IdentificatieType.BSN
                    }
                    with(this[1]) {
                        rolid shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.uuid.toString()
                        roltype shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.omschrijving
                        roltoelichting shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.roltoelichting
                        type shouldBe "NIET_NATUURLIJK_PERSOON"
                        identificatie shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.identificatienummer
                        identificatieType shouldBe IdentificatieType.VN
                    }
                    with(last()) {
                        rolid shouldBe rolNietNatuurlijkPersoonWithRSIN.uuid.toString()
                        roltype shouldBe rolNietNatuurlijkPersoonWithRSIN.omschrijving
                        roltoelichting shouldBe rolNietNatuurlijkPersoonWithRSIN.roltoelichting
                        type shouldBe "NIET_NATUURLIJK_PERSOON"
                        identificatie shouldBe rolNietNatuurlijkPersoonWithRSIN.identificatienummer
                        identificatieType shouldBe IdentificatieType.RSIN
                    }
                }
            }
        }
    }
})
