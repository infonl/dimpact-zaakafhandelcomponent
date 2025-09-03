/*
 * SPDX-FileCopyrightText: 2023 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
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
import nl.info.zac.admin.model.createZaakAfzender
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
import nl.info.zac.app.zaak.exception.DueDateNotAllowed
import nl.info.zac.app.zaak.model.BetrokkeneIdentificatie
import nl.info.zac.app.zaak.model.RESTReden
import nl.info.zac.app.zaak.model.RESTZaakAfbrekenGegevens
import nl.info.zac.app.zaak.model.RESTZaakEditMetRedenGegevens
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestZaaktype
import nl.info.zac.app.zaak.model.ZAAK_TYPE_1_OMSCHRIJVING
import nl.info.zac.app.zaak.model.createBetrokkeneIdentificatie
import nl.info.zac.app.zaak.model.createRESTGeometry
import nl.info.zac.app.zaak.model.createRESTZaakAanmaakGegevens
import nl.info.zac.app.zaak.model.createRESTZaakAssignmentData
import nl.info.zac.app.zaak.model.createRESTZakenVerdeelGegevens
import nl.info.zac.app.zaak.model.createRestDocumentOntkoppelGegevens
import nl.info.zac.app.zaak.model.createRestGroup
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.app.zaak.model.createRestZaakAssignmentToLoggedInUserData
import nl.info.zac.app.zaak.model.createRestZaakCreateData
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
import nl.info.zac.test.listener.MockkClearingTestListener.Companion.NO_MOCK_CLEANUP
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
@Tags(NO_MOCK_CLEANUP)
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

    Context("Creating a zaak") {
        Given("Zaak input data is provided") {
            val group = createGroup()
            val formulierData = mapOf(Pair("fakeKey", "fakeValue"))
            val objectRegistratieObject = createORObject()
            val productaanvraagDimpact = createProductaanvraagDimpact()
            val restZaakCreateData = createRestZaakCreateData(einddatumGepland = LocalDate.now().minusDays(1))
            val restZaak = createRestZaak(einddatumGepland = LocalDate.now().minusDays(1))
            val zaakTypeUUID = UUID.randomUUID()
            val zaakType = createZaakType(
                omschrijving = ZAAK_TYPE_1_OMSCHRIJVING,
                servicenorm = "P10D",
                uri = URI("https://example.com/zaaktypes/$zaakTypeUUID")
            )
            val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
                restZaakCreateData = createRestZaakCreateData(
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
            every {
                inboxProductaanvraagService.delete(restZaakAanmaakGegevens.inboxProductaanvraag?.id)
            } just runs
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
            every { zaakService.bepaalRolGroep(group, zaak) } returns rolOrganisatorischeEenheid
            every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker
            every { zaakService.readZaakTypeByUUID(zaakTypeUUID) } returns zaakType
            every {
                zaakService.addInitiatorToZaak(
                    identificationType = restZaakCreateData.initiatorIdentificatie!!.type,
                    identification = restZaakCreateData.initiatorIdentificatie!!.bsnNummer!!,
                    zaak = zaak,
                    explanation = "Aanmaken zaak"
                )
            } just runs
            every { bpmnService.findProcessDefinitionForZaaktype(zaakTypeUUID) } returns null

            When(
                """
                a zaak is created for a zaaktype for which the user has permissions and no BPMN process definition is found
                """.trimMargin()
            ) {
                every { identityService.readGroup(restZaakAanmaakGegevens.zaak.groep!!.id) } returns group
                every { identityService.readUser(restZaakAanmaakGegevens.zaak.behandelaar!!.id) } returns user
                every {
                    policyService.readOverigeRechten(zaakType.omschrijving)
                } returns createOverigeRechtenAllDeny(startenZaak = true)
                every {
                    policyService.readZaakRechten(zaak, zaakType)
                } returns createZaakRechtenAllDeny(toevoegenInitiatorPersoon = true)
                every { policyService.isAuthorisedForZaaktype(zaakType.omschrijving) } returns true

                val restZaakReturned = zaakRestService.createZaak(restZaakAanmaakGegevens)

                Then("a zaak is created using the ZGW API and a zaak is started in the ZAC CMMN service") {
                    restZaakReturned shouldBe restZaak
                    verify(exactly = 1) {
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
                restZaakCreateData = createRestZaakCreateData(communicatiekanaal = null)
            )
            every { zaakService.readZaakTypeByUUID(any()) } returns zaakType
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
                restZaakCreateData = createRestZaakCreateData(communicatiekanaal = "      ")
            )
            every { zaakService.readZaakTypeByUUID(any<UUID>()) } returns zaakType
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

        Given("zaak input data has due date when servicenorm is not specified in OpenZaak") {
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
                restZaakCreateData = createRestZaakCreateData(einddatumGepland = LocalDate.now())
            )
            every { zaakService.readZaakTypeByUUID(any<UUID>()) } returns zaakType
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(any())
            } returns createZaakafhandelParameters()

            When("zaak creation is attempted") {
                val exception = shouldThrow<DueDateNotAllowed> {
                    zaakRestService.createZaak(restZaakAanmaakGegevens)
                }

                Then("an exception is thrown") {
                    exception.errorCode shouldNotBe null
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
                val restZaakCreateData = createRestZaakCreateData()
                val zaakAanmaakGegevens = createRESTZaakAanmaakGegevens(restZaakCreateData = restZaakCreateData)

                every { zaakService.readZaakTypeByUUID(restZaakCreateData.zaaktype.uuid) } returns zaakType
                every {
                    zaakafhandelParameterService.readZaakafhandelParameters(restZaakCreateData.zaaktype.uuid)
                } returns zaakafhandelParameters

                val exception = shouldThrow<BetrokkeneNotAllowedException> {
                    zaakRestService.createZaak(zaakAanmaakGegevens)
                }

                Then("An error should be thrown") {
                    exception.errorCode shouldBe ErrorCode.ERROR_CODE_CASE_BETROKKENE_NOT_ALLOWED
                }
            }
        }
    }

    Context("Assigning a zaak") {
        Given("a zaak exists, no user and no group are assigned and zaak assignment data is provided") {
            val restZaakToekennenGegevens = createRESTZaakAssignmentData()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val user = createLoggedInUser()
            val rolSlot = mutableListOf<Rol<*>>()
            val restZaak = createRestZaak()
            val rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR)
            val rolMedewerker = createRolMedewerker()
            val group = createGroup()
            val rolGroup = createRolOrganisatorischeEenheid()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakToekennenGegevens.zaakUUID) } returns Pair(zaak, zaakType)
            every { zrcClientService.updateRol(zaak, capture(rolSlot), restZaakToekennenGegevens.reason) } just runs
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns null
            every { identityService.readUser(restZaakToekennenGegevens.assigneeUserName!!) } returns user
            every { zgwApiService.findGroepForZaak(zaak) } returns null
            every { identityService.readGroup(restZaakToekennenGegevens.groupId) } returns group
            every { zaakService.bepaalRolGroep(group, zaak) } returns rolGroup
            every { restZaakConverter.toRestZaak(zaak, zaakType, any()) } returns restZaak
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs
            every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker
            every { bpmnService.isProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, group.name) } just runs
            every { zaakVariabelenService.setUser(zaak.uuid, "fakeDisplayName") } just runs

            When("the zaak is assigned to a user and a group") {
                every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = true)
                every {
                    identityService.validateIfUserIsInGroup(
                        restZaakToekennenGegevens.assigneeUserName!!,
                        restZaakToekennenGegevens.groupId
                    )
                } just runs

                val returnedRestZaak = zaakRestService.assignZaak(restZaakToekennenGegevens)

                Then("the zaak is assigned both to the group and the user") {
                    returnedRestZaak shouldBe restZaak
                    verify(exactly = 2) {
                        zrcClientService.updateRol(zaak, any(), restZaakToekennenGegevens.reason)
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

                And("the zaken search index is updated") {
                    verify(exactly = 1) {
                        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                    }
                }

                And("the zaak data is updated accordingly") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, group.name)
                        zaakVariabelenService.setUser(zaak.uuid, "fakeDisplayName")
                    }
                }
            }
        }

        Given("a zaak with no user and group assigned and zaak assignment data is provided") {
            val restZaakToekennenGegevensUnknownGroup = createRESTZaakAssignmentData(groepId = "unknown")
            val zaak = createZaak()
            val zaakType = createZaakType()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakToekennenGegevensUnknownGroup.zaakUUID)
            } returns Pair(zaak, zaakType)
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

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakToekennenGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
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
            every { bpmnService.isProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, group.name) } just runs
            every { zaakVariabelenService.setUser(zaak.uuid, "fakeDisplayName") } just runs

            When("the zaak is assigned to a user and a group") {
                every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = true)
                every {
                    identityService.validateIfUserIsInGroup(
                        restZaakToekennenGegevens.assigneeUserName!!,
                        restZaakToekennenGegevens.groupId
                    )
                } just runs

                val returnedRestZaak = zaakRestService.assignZaak(restZaakToekennenGegevens)

                Then("the zaak is assigned both to the group and the user") {
                    returnedRestZaak shouldBe restZaak
                    verify(exactly = 2) {
                        zrcClientService.updateRol(zaak, any(), restZaakToekennenGegevens.reason)
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

                And("the zaken search index is updated") {
                    verify(exactly = 1) {
                        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                    }
                }

                And("the zaak data is updated accordingly") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, group.name)
                        zaakVariabelenService.setUser(zaak.uuid, "fakeDisplayName")
                    }
                }
            }
        }

        Given(
            "a zaak exists, with a user and group already assigned and zaak assignment for a group only is provided"
        ) {
            val restZaakToekennenGegevens = createRESTZaakAssignmentData().apply {
                assigneeUserName = null
            }
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()
            val updateRolSlot = mutableListOf<Rol<*>>()
            val restZaak = createRestZaak()
            val rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR)
            val existingRolMedewerker = createRolMedewerker()
            val group = createGroup()
            val existingRolGroup = createRolOrganisatorischeEenheid()
            val rolGroup = createRolOrganisatorischeEenheid(
                zaakURI = zaak.url,
                organisatorischeEenheidIdentificatie = createOrganisatorischeEenheid(identificatie = "newGroup")
            )

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakToekennenGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every { zrcClientService.updateRol(zaak, capture(updateRolSlot), restZaakToekennenGegevens.reason) } just runs
            every { zrcClientService.deleteRol(zaak, BetrokkeneTypeEnum.MEDEWERKER, restZaakToekennenGegevens.reason) } just runs
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns existingRolMedewerker
            every { zgwApiService.findGroepForZaak(zaak) } returns existingRolGroup
            every { identityService.readGroup(restZaakToekennenGegevens.groupId) } returns group
            every { zaakService.bepaalRolGroep(group, zaak) } returns rolGroup
            every { restZaakConverter.toRestZaak(zaak, zaakType, any()) } returns restZaak
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs
            every { bpmnService.isProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, group.name) } just runs
            every { zaakVariabelenService.removeUser(zaak.uuid) } just runs

            When("the zaak is assigned to a user and a group") {
                every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = true)

                val returnedRestZaak = zaakRestService.assignZaak(restZaakToekennenGegevens)

                Then("the zaak is assigned both to the group and the user") {
                    returnedRestZaak shouldBe restZaak
                    verify(exactly = 1) {
                        zrcClientService.updateRol(zaak, any(), restZaakToekennenGegevens.reason)
                    }
                    with(updateRolSlot.first()) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                        with(betrokkeneIdentificatie as OrganisatorischeEenheidIdentificatie) {
                            identificatie shouldBe rolGroup.betrokkeneIdentificatie!!.identificatie
                        }
                        this.zaak shouldBe rolGroup.zaak
                        omschrijving shouldBe rolType.omschrijving
                    }
                }

                And("the zaken search index is updated") {
                    verify(exactly = 1) {
                        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                    }
                }

                And("the zaak data is updated accordingly") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, group.name)
                        zaakVariabelenService.removeUser(zaak.uuid)
                    }
                }
            }
        }
    }

    Context("Assigning a zaak to the logged-in user") {
        Given("a zaak exists, with assigned user and group that are not the logged-in user ones") {
            val restZaakAssignmentToLoggedInUserData = createRestZaakAssignmentToLoggedInUserData()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()
            val rolSlot = mutableListOf<Rol<*>>()
            val restZaak = createRestZaak()
            val rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR)
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

            val loggedInUserId = "loggedInUserId"
            val loggedInUser = createLoggedInUser(id = loggedInUserId)
            every { loggedInUserInstance.get() } returns loggedInUser
            every { identityService.readUser(loggedInUserId) } returns loggedInUser
            every { zaakService.bepaalRolMedewerker(loggedInUser, zaak) } returns rolMedewerker

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentToLoggedInUserData.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every { zrcClientService.updateRol(zaak, capture(rolSlot), restZaakAssignmentToLoggedInUserData.reason) } just runs
            every { zgwApiService.findGroepForZaak(zaak) } returns existingRolGroup
            every { identityService.readGroup(restZaakAssignmentToLoggedInUserData.groupId) } returns group
            every { zaakService.bepaalRolGroep(group, zaak) } returns rolGroup
            every { restZaakConverter.toRestZaak(zaak, zaakType, any()) } returns restZaak
            every { bpmnService.isProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, group.name) } just runs
            every { zaakVariabelenService.setUser(zaak.uuid, "fakeDisplayName") } just runs

            When("the zaak is assigned to the logged-in user") {
                every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = true)
                every {
                    identityService.validateIfUserIsInGroup(loggedInUserId, restZaakAssignmentToLoggedInUserData.groupId)
                } just runs

                val returnedRestZaak = zaakRestService.assignZaakToLoggedInUser(restZaakAssignmentToLoggedInUserData)

                Then("the zaak is assigned both to the group and the user") {
                    returnedRestZaak shouldBe restZaak
                    verify(exactly = 2) {
                        zrcClientService.updateRol(zaak, any(), restZaakAssignmentToLoggedInUserData.reason)
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

                And("the zaak data is updated accordingly") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, group.name)
                        zaakVariabelenService.setUser(zaak.uuid, "fakeDisplayName")
                    }
                }
            }
        }
    }

    Context("Assigning zaken from a list") {
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
    }

    Context("Linking a zaak") {
        Given("Two open zaken with zaak link data using a 'bijdrage' relatie and in reverse an 'onderwerp' relatie") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val teKoppelenZaak = createZaak()
            val teKoppelenZaakType = createZaakType()
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.BIJDRAGE,
                reverseRelatieType = RelatieType.ONDERWERP
            )
            val patchZaakUUIDSlot = mutableListOf<UUID>()
            val patchZaakSlot = mutableListOf<Zaak>()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { policyService.readZaakRechten(teKoppelenZaak, teKoppelenZaakType) } returns createZaakRechten()
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
                        relevanteAndereZaken shouldHaveSize (1)
                        with(relevanteAndereZaken[0]) {
                            url shouldBe teKoppelenZaak.url
                            aardRelatie shouldBe AardRelatieEnum.BIJDRAGE
                        }
                    }
                    with(patchZaakSlot[1]) {
                        relevanteAndereZaken shouldHaveSize (1)
                        with(relevanteAndereZaken[0]) {
                            url shouldBe zaak.url
                            aardRelatie shouldBe AardRelatieEnum.ONDERWERP
                        }
                    }
                }
            }
        }

        Given("Two open zaken with zaak link data using a 'hoofdzaak' relatie and no reverse relation") {
            clearAllMocks()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val teKoppelenZaak = createZaak()
            val teKoppelenZaakType = createZaakType()
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.HOOFDZAAK
            )
            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { policyService.readZaakRechten(teKoppelenZaak, teKoppelenZaakType) } returns createZaakRechten()
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
            clearAllMocks()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val teKoppelenZaak = createZaak()
            val teKoppelenZaakType = createZaakType()
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.HOOFDZAAK
            )

            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { policyService.readZaakRechten(teKoppelenZaak, teKoppelenZaakType) } returns createZaakRechten()

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
    }

    Context("Unlinking a zaak") {
        Given("Two linked zaken with the relation 'vervolg'") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val gekoppeldeZaak = createZaak()
            val gekoppeldeZaakType = createZaakType()
            val restZaakUnlinkData = createRestZaakUnlinkData(
                zaakUuid = zaak.uuid,
                gekoppeldeZaakIdentificatie = gekoppeldeZaak.identificatie,
                relationType = RelatieType.VERVOLG,
                reason = "fakeUnlinkReason"
            )
            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakID(restZaakUnlinkData.gekoppeldeZaakIdentificatie)
            } returns Pair(gekoppeldeZaak, gekoppeldeZaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { policyService.readZaakRechten(gekoppeldeZaak, gekoppeldeZaakType) } returns createZaakRechten()
            every {
                zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot), "fakeUnlinkReason")
            } returns zaak

            When("the zaken are unlinked") {
                zaakRestService.unlinkZaak(restZaakUnlinkData)

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
    }

    Context("Updating a zaak") {
        clearAllMocks()

        Given("a zaak with tasks exists and zaak and tasks have final date set") {
            val changeDescription = "change description"
            val zaak = createZaak()
            val zaakType = createZaakType(servicenorm = "P10D")
            val zaakRechten = createZaakRechten()
            val newZaakFinalDate = zaak.uiterlijkeEinddatumAfdoening.minusDays(10)
            val restZaakCreateData = createRestZaakCreateData(uiterlijkeEinddatumAfdoening = newZaakFinalDate).apply {
                einddatumGepland = startdatum
            }
            val restZaakEditMetRedenGegevens =
                RESTZaakEditMetRedenGegevens(zaak = restZaakCreateData, reden = changeDescription)
            val patchedZaak = createZaak()
            val patchedRestZaak = createRestZaak()
            val task = mockk<Task>()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every { zrcClientService.patchZaak(zaak.uuid, any(), changeDescription) } returns patchedZaak
            every { flowableTaskService.listOpenTasksForZaak(any()) } returns listOf(task, task, task)
            every { task.dueDate } returns zaak.uiterlijkeEinddatumAfdoening.toDate()
            every { task.name } returnsMany listOf("fakeTask", AANVULLENDE_INFORMATIE_TASK_NAME, "another task")
            every { task.dueDate = newZaakFinalDate.toDate() } just runs
            every { flowableTaskService.updateTask(task) } returns task
            every { task.id } returns "id"
            every { eventingService.send(any<ScreenEvent>()) } just runs
            every { restZaakConverter.toRestZaak(patchedZaak, zaakType, zaakRechten) } returns patchedRestZaak
            every {
                identityService.validateIfUserIsInGroup(restZaakCreateData.behandelaar!!.id, restZaakCreateData.groep!!.id)
            } just runs
            every { zaakafhandelParameterService.readZaakafhandelParameters(any()) } returns createZaakafhandelParameters()

            When("zaak final date is set to a later date") {
                val updatedRestZaak = zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)

                Then("zaak is updated with the new data") {
                    updatedRestZaak shouldBe patchedRestZaak
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
            val restZaakCreateData = createRestZaakCreateData()
            val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaakCreateData, changeDescription)

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
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
            val restZaakCreateData =
                createRestZaakCreateData(uiterlijkeEinddatumAfdoening = newZaakFinalDate, einddatumGepland = null)
            val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaakCreateData, "change description")

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
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
                val restZaakCreateData = restZaakCreateData.copy(
                    uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening
                )
                val restZaak = createRestZaak()
                val zaakRechten = createZaakRechten()
                every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
                every { identityService.validateIfUserIsInGroup(any(), any()) } just runs
                every { restZaakConverter.toRestZaak(any(), zaakType, zaakRechten) } returns restZaak
                every { zrcClientService.patchZaak(zaak.uuid, any(), any()) } returns zaak

                zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens.copy(zaak = restZaakCreateData))

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
            val restZaakCreateData =
                createRestZaakCreateData(uiterlijkeEinddatumAfdoening = newZaakFinalDate, einddatumGepland = null)
            val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaakCreateData, "change description")

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
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
                val restZaak = createRestZaak(
                    uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening
                )
                val zaakRechten = createZaakRechten()
                every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
                every { identityService.validateIfUserIsInGroup(any(), any()) } just runs
                every { restZaakConverter.toRestZaak(any(), zaakType, zaakRechten) } returns restZaak
                every { zrcClientService.patchZaak(zaak.uuid, any(), any()) } returns zaak

                zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens.copy(zaak = restZaakCreateData))

                Then("it succeeds") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(zaak.uuid, any(), any())
                    }
                }
            }
        }

        Given("due date change when servicenorm is not specifed in OpenZaak") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()
            val restZaakCreateData = createRestZaakCreateData(einddatumGepland = LocalDate.now())
            val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaakCreateData, "change description")

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every { zaakafhandelParameterService.readZaakafhandelParameters(any()) } returns createZaakafhandelParameters()

            When("zaak update is requested with a new final date") {
                val exception = shouldThrow<DueDateNotAllowed> {
                    zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)
                }

                Then("it fails") {
                    exception.message shouldBe null
                }
            }
        }
    }

    Context("Updating a zaak location") {
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
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
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
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
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
    }

    Context("Updating an initiator") {
        Given("A zaak with an initiator and rest zaak betrokkene gegevens") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten(toevoegenInitiatorPersoon = true)
            val indentification = "123456677"
            val restZaakInitiatorGegevens = createRestZaakInitiatorGegevens()
            val rolMedewerker = createRolMedewerker()
            val restZaak = createRestZaak()
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
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

        Given("A zaak without an initiator and an initiator of type vestiging") {
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

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
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

            When("the initiator is updated") {
                zaakRestService.updateInitiator(
                    restZaakInitiatorGegevens.apply {
                        toelichting = "test reden"
                    }
                )

                Then("the initiator should be added to the zaak") {
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

        Given("A zaak without an initiator and an initiator of type rechtspersoon") {
            val kvkNummer = "1234567"
            val restZaakInitiatorGegevens = createRestZaakInitiatorGegevens(
                betrokkeneIdentificatie = BetrokkeneIdentificatie(
                    type = IdentificatieType.RSIN,
                    kvkNummer = kvkNummer,
                )
            )
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(any()) } returns null
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every {
                zaakService.addInitiatorToZaak(
                    IdentificatieType.RSIN,
                    kvkNummer,
                    zaak,
                    any()
                )
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten) } returns createRestZaak()

            When("the initiator is updated") {
                zaakRestService.updateInitiator(
                    restZaakInitiatorGegevens.apply {
                        toelichting = "test reden"
                    }
                )

                Then("the initiator should be added to the zaak") {
                    verify(exactly = 1) {
                        zaakService.addInitiatorToZaak(
                            IdentificatieType.RSIN,
                            kvkNummer,
                            any(),
                            "test reden"
                        )
                    }
                }
            }
        }

        Given("A zaak without an initiator and an initiator of type rechtspersoon without a KVK nummer") {
            clearAllMocks()
            val restZaakInitiatorGegevens = createRestZaakInitiatorGegevens(
                betrokkeneIdentificatie = BetrokkeneIdentificatie(
                    type = IdentificatieType.RSIN,
                    kvkNummer = null,
                )
            )
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(any()) } returns null
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten

            When("the initiator is updated without a required KVK nummer") {
                val exception = shouldThrow<IllegalArgumentException> {
                    zaakRestService.updateInitiator(restZaakInitiatorGegevens)
                }

                Then("and exception should be thrown and the initiator should not be added to the zaak") {
                    exception.message shouldBe "KVK nummer is required for type RSIN"
                    verify(exactly = 0) {
                        zaakService.addInitiatorToZaak(any(), any(), any(), any())
                    }
                }
            }
        }
    }

    Context("Deleting an initiator") {
        Given("A zaak with an initiator") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten(toevoegenInitiatorPersoon = true)
            val rolMedewerker = createRolMedewerker()
            val restZaak = createRestZaak()
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
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
    }

    Context("Terminating a zaak") {
        Given("A zaak and no managed zaakbeeindigreden") {
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaakTypeUUID = zaakType.url.extractUuid()
            val zaak = createZaak(zaakTypeURI = zaakType.url)
            val zaakAfhandelParameters = createZaakafhandelParameters()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten(afbreken = true)
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
            clearAllMocks()
            val zaakUuid = UUID.randomUUID()
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaak = createZaak(
                uuid = zaakUuid,
                zaakTypeURI = zaakType.url,
                resultaat = URI("https://example.com/${UUID.randomUUID()}")
            )

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten(afbreken = true)

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
            clearAllMocks()
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

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten(afbreken = true)
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
                    zaakRestService.terminateZaak(
                        zaak.uuid,
                        RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = "not a number")
                    )
                }

                Then("it throws an error") {
                    exception.message shouldBe "For input string: \"not a number\""
                }
            }
        }
    }

    Context("Listing zaak types") {
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
            every { configuratieService.featureFlagPabcIntegration() } returns false
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
            clearAllMocks()
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
                every { configuratieService.featureFlagPabcIntegration() } returns false
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
    }

    Context("Updating zaak data") {
        Given("Rest zaak data") {
            val restZaakUpdate = createRestZaak()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakdataMap = slot<Map<String, Any>>()
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakUpdate.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
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
    }

    Context("Reading a zaak") {
        Given("A zaak with an initiator of type BSN for which signaleringen exist") {
            val zaakUUID = UUID.randomUUID()
            val zaak = createZaak(uuid = zaakUUID)
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten(lezen = true)
            val restZaak = createRestZaak(
                uuid = zaakUUID,
                rechten = zaakRechten.toRestZaakRechten(),
                initiatorBetrokkeneIdentificatie = createBetrokkeneIdentificatie(
                    type = IdentificatieType.BSN,
                    bsnNummer = "123456789"
                )
            )
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
            } returns Pair(zaak, zaakType)
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
    }

    Context("Downloading a process diagram") {
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
    }

    Context("Uncoupling an informatieobject from a zaak") {
        Given(
            "A zaak with a zaakinformatieobject where the corresponding informatieobject is only linked to this zaak"
        ) {
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
    }

    Context("Listing betrokkenen for a zaak") {
        Given(
            """
            A zaak with a betrokkene of type natuurlijk persoon, a betrokkene of type niet-natuurlijk persoon
            with a vestigingsnummer, a betrokkene of type niet-natuurlijk persoon with a RSIN (=INN NNP ID),
            and a betrokkene without a betrokkene identification.
            """
        ) {
            val zaak = createZaak()
            val zaakType = createZaakType()
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
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
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
    }

    Context("Listing afzenders for zaak and reading the default afzender for a zaak") {
        Given("Zaakafhandelparameters with zaakafzenders, one of which uses 'special mails'") {
            val zaakUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val zaak = createZaak(
                uuid = zaakUUID,
                zaakTypeURI = URI("https://example.com/zaaktypes/$zaakTypeUUID")
            )
            val zaakafhandelparameters = createZaakafhandelParameters(zaaktypeUUID = zaakTypeUUID)
            val zaakAfzenders = setOf(
                createZaakAfzender(
                    id = 1L,
                    zaakafhandelParameters = zaakafhandelparameters,
                    defaultMail = false,
                    mail = "test@example.com",
                    replyTo = null
                ),
                createZaakAfzender(
                    id = 2L,
                    zaakafhandelParameters = zaakafhandelparameters,
                    defaultMail = true,
                    mail = "GEMEENTE",
                    replyTo = "MEDEWERKER"
                ),
            )
            zaakafhandelparameters.setZaakAfzenders(zaakAfzenders)
            every { zrcClientService.readZaak(zaakUUID) } returns zaak
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID)
            } returns zaakafhandelparameters
            every { configuratieService.readGemeenteMail() } returns "fake-gemeente@example.com"
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                email = "fake-medewerker@example.com"
            )

            When("the zaakafzenders are requested") {
                val returnedRestZaakAfzenders = zaakRestService.listAfzendersVoorZaak(zaakUUID)

                Then("the zaakafzenders are returned, including special mails for GEMEENTE and MEDEWERKER") {
                    with(returnedRestZaakAfzenders) {
                        // we defined two afzender emails but expect three because
                        // we did not define an 'MEDEWERKER' afzender email so this one
                        // should be added automatically
                        size shouldBe 3
                        first().apply {
                            id shouldBe 2L
                            defaultMail shouldBe true
                            mail shouldBe "fake-gemeente@example.com"
                            replyTo shouldBe "fake-medewerker@example.com"
                            speciaal shouldBe true
                        }
                        this[1].apply {
                            id shouldBe null
                            defaultMail shouldBe false
                            mail shouldBe "fake-medewerker@example.com"
                            replyTo shouldBe null
                            speciaal shouldBe true
                        }
                        last().apply {
                            id shouldBe 1L
                            defaultMail shouldBe false
                            mail shouldBe "test@example.com"
                            replyTo shouldBe null
                            speciaal shouldBe false
                        }
                    }
                }
            }

            When("the default afzender is read") {
                val returnedDefaultRestZaakAfzender = zaakRestService.readDefaultAfzenderVoorZaak(zaakUUID)

                Then("the default afzender is returned with the email address of the special mail type") {
                    returnedDefaultRestZaakAfzender shouldNotBe null
                    with(returnedDefaultRestZaakAfzender!!) {
                        id shouldBe 2L
                        defaultMail shouldBe true
                        mail shouldBe "fake-gemeente@example.com"
                        replyTo shouldBe "fake-medewerker@example.com"
                        speciaal shouldBe true
                    }
                }
            }
        }

        Given("Zaakafhandelparameters without any zaakafzenders") {
            val zaakUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val zaak = createZaak(
                uuid = zaakUUID,
                zaakTypeURI = URI("https://example.com/zaaktypes/$zaakTypeUUID")
            )
            val zaakafhandelparameters = createZaakafhandelParameters(zaaktypeUUID = zaakTypeUUID)
            zaakafhandelparameters.setZaakAfzenders(emptyList())
            every { zrcClientService.readZaak(zaakUUID) } returns zaak
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID)
            } returns zaakafhandelparameters
            every { configuratieService.readGemeenteMail() } returns "fake-gemeente@example.com"
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                email = "fake-medewerker@example.com"
            )

            When("the zaakafzenders are requested") {
                val returnedRestZaakAfzenders = zaakRestService.listAfzendersVoorZaak(zaakUUID)

                Then("a list consisting of 'special mail' afzenders only should be returned") {
                    returnedRestZaakAfzenders shouldHaveSize 2
                    with(returnedRestZaakAfzenders) {
                        size shouldBe 2
                        first().apply {
                            id shouldBe null
                            defaultMail shouldBe false
                            mail shouldBe "fake-gemeente@example.com"
                            replyTo shouldBe null
                            speciaal shouldBe true
                        }
                        last().apply {
                            id shouldBe null
                            defaultMail shouldBe false
                            mail shouldBe "fake-medewerker@example.com"
                            replyTo shouldBe null
                            speciaal shouldBe true
                        }
                    }
                }
            }

            When("the default afzender is read") {
                val returnedDefaultRestZaakAfzender = zaakRestService.readDefaultAfzenderVoorZaak(zaakUUID)

                Then("no default afzender should be returned") {
                    returnedDefaultRestZaakAfzender shouldBe null
                }
            }
        }
    }
})
