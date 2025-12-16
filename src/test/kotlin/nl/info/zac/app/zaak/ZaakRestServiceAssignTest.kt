/*
 * SPDX-FileCopyrightText: 2023 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.model.createRESTZaakAssignmentData
import nl.info.zac.app.zaak.model.createRESTZakenVerdeelGegevens
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.app.zaak.model.createRestZaakAssignmentToLoggedInUserData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.history.ZaakHistoryService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.zaak.ZaakService
import java.util.UUID

@Suppress("LongParameterList")
class ZaakRestServiceAssignTest : BehaviorSpec({
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
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zaakService = mockk<ZaakService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakHistoryService = mockk<ZaakHistoryService>()
    val testDispatcher = StandardTestDispatcher()
    val zaakRestService = ZaakRestService(
        bpmnService = bpmnService,
        brcClientService = brcClientService,
        cmmnService = cmmnService,
        configuratieService = configuratieService,
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
        ztcClientService = ztcClientService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Assigning a zaak") {
        Given("zaak assignment data is provided") {
            val restZaakAssignmentData = createRESTZaakAssignmentData()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaak = createRestZaak()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentData.zaakUUID) } returns Pair(zaak, zaakType)
            every {
                zaakService.assignZaak(
                    zaak,
                    restZaakAssignmentData.groupId,
                    restZaakAssignmentData.assigneeUserName,
                    restZaakAssignmentData.reason
                )
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, any()) } returns restZaak

            When("toekennen policy is assigned to the user") {
                every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = true)
                val returnedRestZaak = zaakRestService.assignZaak(restZaakAssignmentData)

                Then("expected response is prepared") {
                    returnedRestZaak shouldBe restZaak
                }
            }

            When("toekennen policy is missing") {
                every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = false)
                shouldThrow<PolicyException> {
                    zaakRestService.assignZaak(restZaakAssignmentData)
                }

                Then("exception is thrown") {}
            }
        }
    }

    Context("Assigning a zaak to the logged-in user") {
        Given("when zaak is open and toekennen policy is assigned to the logged-in user") {
            val restZaakAssignmentToLoggedInUserData = createRestZaakAssignmentToLoggedInUserData()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaak = createRestZaak()

            val loggedInUserId = "loggedInUserId"
            val loggedInUser = createLoggedInUser(id = loggedInUserId)
            every { loggedInUserInstance.get() } returns loggedInUser

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentToLoggedInUserData.zaakUUID)
            } returns Pair(zaak, zaakType)
            every {
                zaakService.assignZaak(
                    zaak,
                    restZaakAssignmentToLoggedInUserData.groupId,
                    loggedInUserId,
                    restZaakAssignmentToLoggedInUserData.reason
                )
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, any()) } returns restZaak

            When("toekennen policy is assigned to the logged-in user") {
                every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = true)
                val returnedRestZaak = zaakRestService.assignZaakToLoggedInUser(restZaakAssignmentToLoggedInUserData)

                Then("the zaak is assigned both to the group and the user") {
                    returnedRestZaak shouldBe restZaak
                }
            }

            When("logged-in user does not have toekennen policy") {
                every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechtenAllDeny(toekennen = false)
                shouldThrow<PolicyException> {
                    zaakRestService.assignZaakToLoggedInUser(restZaakAssignmentToLoggedInUserData)
                }

                Then("exception is thrown") {}
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
                groepId = group.name,
                behandelaarGebruikersnaam = user.id,
                reden = "fakeReason"
            )
            every { policyService.readWerklijstRechten() } returns createWerklijstRechten()
            every { zaakService.assignZaken(any(), any(), any(), any(), any()) } just runs
            every { identityService.readGroup(group.name) } returns group
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
})
