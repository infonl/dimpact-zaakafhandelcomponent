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
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.test.StandardTestDispatcher
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.`object`.model.createORObject
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakobjectOpenbareRuimte
import nl.info.client.zgw.model.createZaakobjectPand
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.exception.BetrokkeneNotAllowedException
import nl.info.zac.app.zaak.exception.CommunicationChannelNotFound
import nl.info.zac.app.zaak.exception.DueDateNotAllowed
import nl.info.zac.app.zaak.model.RestZaaktype
import nl.info.zac.app.zaak.model.ZAAK_TYPE_1_OMSCHRIJVING
import nl.info.zac.app.zaak.model.createRESTZaakAanmaakGegevens
import nl.info.zac.app.zaak.model.createRestGroup
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.app.zaak.model.createRestZaakCreateData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.exception.ErrorCode
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.history.ZaakHistoryService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.identification.IdentificationService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createOverigeRechtenAllDeny
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.productaanvraag.createProductaanvraagDimpact
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.zaak.ZaakService
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
class ZaakRestServiceCreateTest : BehaviorSpec({
    val decisionService = mockk<DecisionService>()
    val bpmnService = mockk<BpmnService>()
    val brcClientService = mockk<BrcClientService>()
    val configurationService = mockk<ConfigurationService>()
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
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zaakService = mockk<ZaakService>()
    val zgwApiService = mockk<ZgwApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakHistoryService = mockk<ZaakHistoryService>()
    val identificationService = mockk<IdentificationService>()
    val testDispatcher = StandardTestDispatcher()
    val zaakRestService = ZaakRestService(
        bpmnService = bpmnService,
        brcClientService = brcClientService,
        cmmnService = cmmnService,
        configurationService = configurationService,
        decisionService = decisionService,
        dispatcher = testDispatcher,
        drcClientService = drcClientService,
        eventingService = eventingService,
        healthCheckService = healthCheckService,
        identityService = identityService,
        inboxProductaanvraagService = inboxProductaanvraagService,
        indexingService = indexingService,
        loggedInUserInstance = loggedInUserInstance,
        objectsClientService = objectsClientService,
        ontkoppeldeDocumentenService = ontkoppeldeDocumentenService,
        opschortenZaakHelper = opschortenZaakHelper,
        policyService = policyService,
        productaanvraagService = productaanvraagService,
        restDecisionConverter = restDecisionConverter,
        restZaakConverter = restZaakConverter,
        restZaakOverzichtConverter = restZaakOverzichtConverter,
        restZaaktypeConverter = restZaaktypeConverter,
        signaleringService = signaleringService,
        zaakHistoryLineConverter = zaakHistoryLineConverter,
        zaakHistoryService = zaakHistoryService,
        zaakService = zaakService,
        zaakVariabelenService = zaakVariabelenService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        zgwApiService = zgwApiService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        identificationService = identificationService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("CMMN zaak input data is provided") {
        val group = createGroup()
        val bsn = "12345678"
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
                    id = group.name
                ),

            ),
            zaakTypeUUID = zaakTypeUUID
        )
        val rolMedewerker = createRolMedewerker()
        val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid()
        val user = createLoggedInUser()
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val zaakObjectPand = createZaakobjectPand()
        val zaakObjectOpenbareRuimte = createZaakobjectOpenbareRuimte()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val bronOrganisatie = "fakeBronOrganisatie"
        val verantwoordelijkeOrganisatie = "fakeVerantwoordelijkeOrganisatie"
        val zaakCreatedSlot = slot<Zaak>()
        val updatedRolesSlot = mutableListOf<Rol<*>>()
        val loggedInUser = createLoggedInUser()

        every { configurationService.readBronOrganisatie() } returns bronOrganisatie
        every { configurationService.readVerantwoordelijkeOrganisatie() } returns verantwoordelijkeOrganisatie
        every { cmmnService.startCase(zaak, zaakType, zaaktypeCmmnConfiguration, null) } just runs
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
        every { zaaktypeConfigurationService.readZaaktypeConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID)
        } returns zaaktypeCmmnConfiguration
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
                identification = bsn,
                zaak = zaak,
                explanation = "Aanmaken zaak"
            )
        } just runs
        every { loggedInUserInstance.get() } returns loggedInUser

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
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechtenAllDeny(toevoegenInitiatorPersoon = true)
            every { policyService.isAuthorisedForZaaktype(zaakType.omschrijving) } returns true
            every {
                identificationService.replaceKeyWithBsn(restZaakAanmaakGegevens.zaak.initiatorIdentificatie!!.temporaryPersonId!!)
            } returns bsn

            val restZaakReturned = zaakRestService.createZaak(restZaakAanmaakGegevens)

            Then("a zaak is created using the ZGW API and a zaak is started in the ZAC CMMN service") {
                restZaakReturned shouldBe restZaak
                verify(exactly = 1) {
                    zgwApiService.createZaak(any())
                    cmmnService.startCase(zaak, zaakType, zaaktypeCmmnConfiguration, null)
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

    Given("BPMN zaak input data is provided") {
        val group = createGroup()
        val bsn = "12345678"
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
                    id = group.name
                ),

            ),
            zaakTypeUUID = zaakTypeUUID
        )
        val rolMedewerker = createRolMedewerker()
        val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid()
        val user = createLoggedInUser()
        val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
        val zaakData = mapOf(
            "zaakGroep" to restZaak.groep!!.naam,
            "zaakBehandelaar" to restZaak.behandelaar!!.naam,
            "zaakCommunicatiekanaal" to restZaak.communicatiekanaal!!
        )
        val zaakObjectPand = createZaakobjectPand()
        val zaakObjectOpenbareRuimte = createZaakobjectOpenbareRuimte()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val bronOrganisatie = "fakeBronOrganisatie"
        val verantwoordelijkeOrganisatie = "fakeVerantwoordelijkeOrganisatie"
        val zaakCreatedSlot = slot<Zaak>()
        val updatedRolesSlot = mutableListOf<Rol<*>>()
        val loggedInUser = createLoggedInUser()

        every { configurationService.readBronOrganisatie() } returns bronOrganisatie
        every { configurationService.readVerantwoordelijkeOrganisatie() } returns verantwoordelijkeOrganisatie
        every {
            bpmnService.startProcess(zaak, zaakType, zaaktypeBpmnConfiguration.bpmnProcessDefinitionKey, zaakData)
        } just runs
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
        every { zaaktypeConfigurationService.readZaaktypeConfiguration(zaakTypeUUID) } returns zaaktypeBpmnConfiguration
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
                identification = bsn,
                zaak = zaak,
                explanation = "Aanmaken zaak"
            )
        } just runs
        every { bpmnService.findProcessDefinitionForZaaktype(zaakTypeUUID) } returns zaaktypeBpmnConfiguration
        every { loggedInUserInstance.get() } returns loggedInUser

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
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechtenAllDeny(toevoegenInitiatorPersoon = true)
            every { policyService.isAuthorisedForZaaktype(zaakType.omschrijving) } returns true
            every {
                identificationService.replaceKeyWithBsn(restZaakAanmaakGegevens.zaak.initiatorIdentificatie!!.temporaryPersonId!!)
            } returns bsn

            val restZaakReturned = zaakRestService.createZaak(restZaakAanmaakGegevens)

            Then("a zaak is created using the ZGW API and a zaak is started in the ZAC CMMN service") {
                restZaakReturned shouldBe restZaak
                verify(exactly = 1) {
                    zgwApiService.createZaak(any())
                    bpmnService.startProcess(
                        zaak,
                        zaakType,
                        zaaktypeBpmnConfiguration.bpmnProcessDefinitionKey,
                        zaakData
                    )
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
        every {
            zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
        } returns createZaaktypeBpmnConfiguration()
        every { policyService.readOverigeRechten(zaakType.omschrijving) } returns createOverigeRechten()
        every { policyService.isAuthorisedForZaaktype(zaakType.omschrijving) } returns true

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
        every {
            zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
        } returns createZaaktypeBpmnConfiguration()
        every { policyService.readOverigeRechten(zaakType.omschrijving) } returns createOverigeRechten()
        every { policyService.isAuthorisedForZaaktype(zaakType.omschrijving) } returns true

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
            zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
        } returns createZaaktypeBpmnConfiguration()
        every { policyService.readOverigeRechten(zaakType.omschrijving) } returns createOverigeRechten()
        every { policyService.isAuthorisedForZaaktype(zaakType.omschrijving) } returns true

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
        When("This is not allowed in the zaak afhandel parameters") {
            val betrokkeneKoppelingen = createBetrokkeneKoppelingen(
                brpKoppelen = false,
                zaaktypeConfiguration = createZaaktypeCmmnConfiguration()
            )
            val zaaktypeCmmnConfiguration =
                createZaaktypeCmmnConfiguration(zaaktypeBetrokkeneParameters = betrokkeneKoppelingen)
            val zaakType = createZaakType()
            val restZaakCreateData = createRestZaakCreateData()
            val zaakAanmaakGegevens = createRESTZaakAanmaakGegevens(restZaakCreateData = restZaakCreateData)

            every { zaakService.readZaakTypeByUUID(restZaakCreateData.zaaktype.uuid) } returns zaakType
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
            } returns zaaktypeCmmnConfiguration

            val exception = shouldThrow<BetrokkeneNotAllowedException> {
                zaakRestService.createZaak(zaakAanmaakGegevens)
            }

            Then("An error should be thrown") {
                exception.errorCode shouldBe ErrorCode.ERROR_CODE_CASE_BETROKKENE_NOT_ALLOWED
            }
        }
    }
})
