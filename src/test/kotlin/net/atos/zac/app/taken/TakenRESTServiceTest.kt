/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.taken

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.inspectors.forExactly
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObject
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.app.identity.model.createRESTUser
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import net.atos.zac.app.taken.converter.RESTTaakConverter
import net.atos.zac.app.taken.model.TaakStatus
import net.atos.zac.app.taken.model.createRESTTaak
import net.atos.zac.app.taken.model.createRESTTaakToekennenGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.flowable.TakenService
import net.atos.zac.flowable.util.TaskUtil.getTaakStatus
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createDocumentRechten
import net.atos.zac.policy.output.createTaakRechten
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.util.DateTimeConverterUtil
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.task.api.Task
import org.flowable.task.api.history.HistoricTaskInstance
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.URI
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@MockKExtension.CheckUnnecessaryStub
class TakenRESTServiceTest : BehaviorSpec() {
    val drcClientService = mockk<DRCClientService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val eventingService = mockk<EventingService>()
    val httpSessionInstance = mockk<Instance<HttpSession>>()
    val indexeerService = mockk<IndexeerService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val taakVariabelenService = mockk<TaakVariabelenService>()
    val restTaakConverter = mockk<RESTTaakConverter>()
    val takenService = mockk<TakenService>()
    val zrcClientService = mockk<ZRCClientService>()

    // We have to use @InjectMockKs since the class under test uses field injection instead of constructor injection.
    // This is because WildFly does not properly support constructor injection for JAX-RS REST services.
    @InjectMockKs
    lateinit var takenRESTService: TakenRESTService

    val loggedInUser = createLoggedInUser()

    override suspend fun beforeTest(testCase: TestCase) {
        MockKAnnotations.init(this)
        every { loggedInUserInstance.get() } returns loggedInUser
    }

    init {
        Given("a task is not yet assigned") {
            val restTaakToekennenGegevens = createRESTTaakToekennenGegevens()
            val task = mockk<Task>()
            val identityLinkInfo = mockk<IdentityLinkInfo>()
            val identityLinks = listOf(identityLinkInfo)
            val signaleringEventSlot = slot<SignaleringEvent<*>>()
            val screenEventSlots = mutableListOf<ScreenEvent>()

            every { takenService.readOpenTask(restTaakToekennenGegevens.taakId) } returns task
            every { getTaakStatus(task) } returns TaakStatus.NIET_TOEGEKEND
            every {
                takenService.assignTaskToUser(
                    restTaakToekennenGegevens.taakId,
                    restTaakToekennenGegevens.behandelaarId,
                    restTaakToekennenGegevens.reden
                )
            } returns task
            every {
                takenService.assignTaskToGroup(
                    task,
                    restTaakToekennenGegevens.groepId,
                    restTaakToekennenGegevens.reden
                )
            } returns task
            every { policyService.readTaakRechten(task) } returns createTaakRechten()
            every { task.assignee } returns ""
            every { task.identityLinks } returns identityLinks
            every { task.id } returns restTaakToekennenGegevens.taakId
            every { restTaakConverter.extractGroupId(identityLinks) } returns "dummyGroupId"
            every { eventingService.send(capture(signaleringEventSlot)) } just runs
            every { eventingService.send(capture(screenEventSlots)) } just runs
            every {
                indexeerService.indexeerDirect(
                    restTaakToekennenGegevens.taakId,
                    ZoekObjectType.TAAK
                )
            } just runs

            When("'toekennen' is called") {
                takenRESTService.toekennen(restTaakToekennenGegevens)

                Then(
                    "the task is assigned to the provided user and group, " +
                        "a signalling event and two screen events are sent,  " +
                        "and the indexed task data is updated"
                ) {
                    verify(exactly = 1) {
                        takenService.assignTaskToUser(
                            restTaakToekennenGegevens.taakId,
                            restTaakToekennenGegevens.behandelaarId,
                            restTaakToekennenGegevens.reden
                        )
                        takenService.assignTaskToGroup(
                            task,
                            restTaakToekennenGegevens.groepId,
                            restTaakToekennenGegevens.reden
                        )
                        eventingService.send(any<SignaleringEvent<*>>())
                        indexeerService.indexeerDirect(
                            restTaakToekennenGegevens.taakId,
                            ZoekObjectType.TAAK
                        )
                    }
                    // we expect two screen events to be sent
                    verify(exactly = 2) {
                        eventingService.send(any<ScreenEvent>())
                    }
                    with(signaleringEventSlot.captured) {
                        assertEquals(this.objectType, SignaleringType.Type.TAAK_OP_NAAM)
                        assertEquals(this.actor, loggedInUser.id)
                        assertEquals(this.objectId.resource, restTaakToekennenGegevens.taakId)
                    }
                    // we expect both a taak screen event and a zaak_taken screen event to be sent
                    screenEventSlots.forExactly(1) { screenEvent ->
                        assertEquals(screenEvent.opcode, Opcode.UPDATED)
                        assertEquals(screenEvent.objectType, ScreenEventType.TAAK)
                        assertEquals(
                            screenEvent.objectId.resource,
                            restTaakToekennenGegevens.taakId
                        )
                    }
                    screenEventSlots.forExactly(1) { screenEvent ->
                        assertEquals(screenEvent.opcode, Opcode.UPDATED)
                        assertEquals(screenEvent.objectType, ScreenEventType.ZAAK_TAKEN)
                        assertEquals(
                            screenEvent.objectId.resource,
                            restTaakToekennenGegevens.zaakUuid.toString()
                        )
                    }
                }
            }
        }

        Given("a task is assigned to the current user") {
            val task = mockk<Task>()
            val zaak = mockk<Zaak>()
            val httpSession = mockk<HttpSession>()
            val historicTaskInstance = mockk<HistoricTaskInstance>()
            val restUser = createRESTUser(
                id = loggedInUser.id,
                name = loggedInUser.fullName
            )
            val restTaak = createRESTTaak(
                behandelaar = restUser
            )
            val restTaakConverted = createRESTTaak(
                behandelaar = restUser
            )

            every { task.assignee } returns "dummyAssignee"
            every { task.description = restTaak.toelichting } just runs
            every { task.dueDate = any() } just runs
            every { takenService.readOpenTask(restTaak.id) } returns task
            every { takenService.updateTask(task) } returns task
            every { zrcClientService.readZaak(restTaak.zaakUuid) } returns zaak
            every { policyService.readTaakRechten(task) } returns createTaakRechten()
            every { httpSessionInstance.get() } returns httpSession
            every { taakVariabelenService.isZaakHervatten(restTaak.taakdata) } returns false
            every { taakVariabelenService.readOndertekeningen(restTaak.taakdata) } returns Optional.empty()
            every { taakVariabelenService.setTaakdata(task, restTaak.taakdata) } just runs
            every { taakVariabelenService.setTaakinformatie(task, null) } just runs
            every { takenService.completeTask(task) } returns historicTaskInstance
            every { indexeerService.addOrUpdateZaak(restTaak.zaakUuid, false) } just runs
            every { historicTaskInstance.id } returns restTaak.id
            every { restTaakConverter.convert(historicTaskInstance) } returns restTaakConverted

            When("'complete' is called") {
                val restTaakReturned = takenRESTService.completeTaak(restTaak)

                Then(
                    "the task is completed and the search index service is invoked"
                ) {
                    restTaakReturned shouldBe restTaakConverted
                    verify(exactly = 1) {
                        takenService.completeTask(task)
                    }
                }
            }
        }

        Given("a task is assigned to the current user with a document that is signed") {
            val task = mockk<Task>()
            val zaak = mockk<Zaak>()
            val httpSession = mockk<HttpSession>()
            val historicTaskInstance = mockk<HistoricTaskInstance>()

            val restUser = createRESTUser(
                id = loggedInUser.id,
                name = loggedInUser.fullName
            )
            val restTaak = createRESTTaak(
                behandelaar = restUser
            )
            val restTaakConverted = createRESTTaak(
                behandelaar = restUser
            )
            val enkelvoudigInformatieObjectUUID = UUID.randomUUID()
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(
                url = URI("http://example.com/$enkelvoudigInformatieObjectUUID")
            )
            val documentenRechten = createDocumentRechten()

            every { task.assignee } returns "dummyAssignee"
            every { takenService.readOpenTask(restTaak.id) } returns task
            every { takenService.updateTask(task) } returns task
            every { zrcClientService.readZaak(restTaak.zaakUuid) } returns zaak
            every { policyService.readTaakRechten(task) } returns createTaakRechten()
            every { httpSessionInstance.get() } returns httpSession
            every { taakVariabelenService.isZaakHervatten(restTaak.taakdata) } returns false
            every { taakVariabelenService.readOndertekeningen(restTaak.taakdata) } returns Optional.of(
                enkelvoudigInformatieObjectUUID.toString()
            )
            every { taakVariabelenService.setTaakdata(task, restTaak.taakdata) } just runs
            every { taakVariabelenService.setTaakinformatie(task, null) } just runs
            every { takenService.completeTask(task) } returns historicTaskInstance
            every { indexeerService.addOrUpdateZaak(restTaak.zaakUuid, false) } just runs
            every { historicTaskInstance.id } returns restTaak.id
            every { restTaakConverter.convert(historicTaskInstance) } returns restTaakConverted
            every {
                drcClientService.readEnkelvoudigInformatieobject(enkelvoudigInformatieObjectUUID)
            } returns enkelvoudigInformatieObject
            every {
                policyService.readDocumentRechten(
                    enkelvoudigInformatieObject,
                    zaak
                )
            } returns documentenRechten
            every {
                enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                    enkelvoudigInformatieObjectUUID
                )
            } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs

            When("'updateTaakdata' is called with changed description and due date") {
                restTaak.apply {
                    toelichting = "changed"
                    fataledatum = LocalDate.parse("2024-03-19")
                }

                every { task.description = restTaak.toelichting } just runs
                every { task.dueDate = DateTimeConverterUtil.convertToDate(restTaak.fataledatum) } just runs
                every { task.id } returns restTaak.id

                val restTaakReturned = takenRESTService.updateTaakdata(restTaak)

                Then("the changes are stored") {
                    restTaakReturned shouldBe restTaak
                    verify(exactly = 1) {
                        takenService.updateTask(task)
                    }
                }
            }

            When("'complete' is called") {
                every { task.description = restTaak.toelichting } just runs
                every { task.dueDate = any() } just runs

                val restTaakReturned = takenRESTService.completeTaak(restTaak)

                Then(
                    "the document is signed, the task is completed and the search index service is invoked"
                ) {
                    restTaakReturned shouldBe restTaakConverted
                    verify(exactly = 1) {
                        enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                            enkelvoudigInformatieObjectUUID
                        )
                        takenService.completeTask(task)
                    }
                }
            }
        }
    }
}
