/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.taken

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObject
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.zac.app.identity.model.createRESTUser
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter
import net.atos.zac.app.taken.converter.RESTTaakConverter
import net.atos.zac.app.taken.converter.RESTTaakHistorieConverter
import net.atos.zac.app.taken.model.TaakStatus
import net.atos.zac.app.taken.model.createRESTTaak
import net.atos.zac.app.taken.model.createRESTTaakToekennenGegevens
import net.atos.zac.app.taken.model.createRESTTaakVerdelenGegevens
import net.atos.zac.app.taken.model.createRESTTaakVerdelenTaak
import net.atos.zac.app.taken.model.createRESTTaakVrijgevenGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.flowable.util.TaskUtil.getTaakStatus
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.exception.PolicyException
import net.atos.zac.policy.output.createDocumentRechtenAllDeny
import net.atos.zac.policy.output.createTaakRechtenAllDeny
import net.atos.zac.policy.output.createWerklijstRechten
import net.atos.zac.policy.output.createWerklijstRechtenAllDeny
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.task.TaskService
import net.atos.zac.util.DateTimeConverterUtil
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.zoeken.IndexeerService
import org.flowable.task.api.Task
import org.flowable.task.api.history.HistoricTaskInstance
import org.flowable.task.api.history.createHistoricTaskInstanceEntityImpl
import java.net.URI
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class TakenRESTServiceTest : BehaviorSpec({
    val drcClientService = mockk<DRCClientService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val eventingService = mockk<EventingService>()
    val httpSessionInstance = mockk<Instance<HttpSession>>()
    val indexeerService = mockk<IndexeerService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val taakVariabelenService = mockk<TaakVariabelenService>()
    val restTaakConverter = mockk<RESTTaakConverter>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val zrcClientService = mockk<ZRCClientService>()
    val opschortenZaakHelper = mockk<OpschortenZaakHelper>()
    val restInformatieobjectConverter = mockk<RESTInformatieobjectConverter>()
    val signaleringenService = mockk<SignaleringenService>()
    val taakHistorieConverter = mockk<RESTTaakHistorieConverter>()
    val zgwApiService = mockk<ZGWApiService>()
    val taskService = mockk<TaskService>()

    val takenRESTService = TakenRESTService(
        drcClientService = drcClientService,
        enkelvoudigInformatieObjectUpdateService = enkelvoudigInformatieObjectUpdateService,
        eventingService = eventingService,
        httpSession = httpSessionInstance,
        indexeerService = indexeerService,
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        taakVariabelenService = taakVariabelenService,
        restTaakConverter = restTaakConverter,
        flowableTaskService = flowableTaskService,
        zrcClientService = zrcClientService,
        opschortenZaakHelper = opschortenZaakHelper,
        restInformatieobjectConverter = restInformatieobjectConverter,
        signaleringenService = signaleringenService,
        taakHistorieConverter = taakHistorieConverter,
        zgwApiService = zgwApiService,
        taskService = taskService
    )
    val loggedInUser = createLoggedInUser()

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("a task is not yet assigned") {
        val restTaakToekennenGegevens = createRESTTaakToekennenGegevens()
        val task = mockk<Task>()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { flowableTaskService.readOpenTask(restTaakToekennenGegevens.taakId) } returns task
        every { getTaakStatus(task) } returns TaakStatus.NIET_TOEGEKEND
        every { task.assignee } returns ""
        every {
            taskService.assignTask(
                restTaakToekennenGegevens,
                task,
                loggedInUser
            )
        } just runs

        When("'toekennen' is called from user with access") {
            every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny(toekennen = true)

            takenRESTService.toekennen(restTaakToekennenGegevens)

            Then(
                "the task is assigned"
            ) {
                verify(exactly = 1) {
                    taskService.assignTask(
                        restTaakToekennenGegevens,
                        task,
                        loggedInUser
                    )
                }
            }
        }

        When("'toekennen' is called from user with no access") {
            every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny()

            val exception = shouldThrow<PolicyException> {
                takenRESTService.toekennen(restTaakToekennenGegevens)
            }

            Then("it throws exception with no message") { exception.message shouldBe null }
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

        every { loggedInUserInstance.get() } returns loggedInUser
        every { task.assignee } returns "dummyAssignee"
        every { task.description = restTaak.toelichting } just runs
        every { task.dueDate = any() } just runs
        every { flowableTaskService.readOpenTask(restTaak.id) } returns task
        every { flowableTaskService.updateTask(task) } returns task
        every { zrcClientService.readZaak(restTaak.zaakUuid) } returns zaak
        every { httpSessionInstance.get() } returns httpSession
        every { taakVariabelenService.isZaakHervatten(restTaak.taakdata) } returns false
        every { taakVariabelenService.readOndertekeningen(restTaak.taakdata) } returns Optional.empty()
        every { taakVariabelenService.setTaakdata(task, restTaak.taakdata) } just runs
        every { taakVariabelenService.setTaakinformatie(task, null) } just runs
        every { flowableTaskService.completeTask(task) } returns historicTaskInstance
        every { indexeerService.addOrUpdateZaak(restTaak.zaakUuid, false) } just runs
        every { historicTaskInstance.id } returns restTaak.id
        every { restTaakConverter.convert(historicTaskInstance) } returns restTaakConverted
        every { eventingService.send(any<ScreenEvent>()) } just runs

        When("'complete' is called from user with access") {
            every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny(wijzigen = true)

            val restTaakReturned = takenRESTService.completeTaak(restTaak)

            Then(
                "the task is completed and the search index service is invoked"
            ) {
                restTaakReturned shouldBe restTaakConverted
                verify(exactly = 1) {
                    flowableTaskService.completeTask(task)
                }
            }
        }

        When("'complete' is called from user with access") {
            every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny()

            val exception = shouldThrow<PolicyException> {
                takenRESTService.completeTaak(restTaak)
            }

            Then("it throws exception with no message") { exception.message shouldBe null }
        }
    }

    Given("a task with signature task data is assigned to the current user with a document that is signed") {
        val task = mockk<Task>()
        val restUser = createRESTUser(
            id = loggedInUser.id,
            name = loggedInUser.fullName
        )
        val restTaakDataKey = "dummyKey"
        val restTaakDataValue = "dummyValue"
        val restTaakData = mutableMapOf(
            restTaakDataKey to restTaakDataValue
        )
        val restTaak = createRESTTaak(
            behandelaar = restUser,
            taakData = restTaakData
        )
        val restTaakConverted = createRESTTaak(
            behandelaar = restUser
        )
        every { task.assignee } returns "dummyAssignee"
        every { flowableTaskService.readOpenTask(restTaak.id) } returns task
        every { flowableTaskService.updateTask(task) } returns task
        every { taakVariabelenService.setTaakdata(task, restTaak.taakdata) } just runs
        every { taakVariabelenService.setTaakinformatie(task, null) } just runs
        every { eventingService.send(any<ScreenEvent>()) } just runs

        When("'updateTaakdata' is called with changed description and due date from user with access") {
            every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny(wijzigen = true)

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
                    flowableTaskService.updateTask(task)
                }
            }
        }

        When("'complete' is called") {
            val zaak = createZaak()
            val historicTaskInstance = createHistoricTaskInstanceEntityImpl()
            val httpSession = mockk<HttpSession>()
            val signatureUUID = UUID.randomUUID()
            val enkelvoudigInformatieObjectUUID = UUID.randomUUID()
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(
                url = URI("http://example.com/$enkelvoudigInformatieObjectUUID")
            )
            every { zrcClientService.readZaak(restTaak.zaakUuid) } returns zaak
            every { flowableTaskService.completeTask(task) } returns historicTaskInstance
            every { indexeerService.addOrUpdateZaak(restTaak.zaakUuid, false) } just runs
            every { restTaakConverter.convert(historicTaskInstance) } returns restTaakConverted
            every { httpSessionInstance.get() } returns httpSession
            // in this test we assume there was no document uploaded to the http session beforehand
            every { httpSession.getAttribute("_FILE__${restTaak.id}__$restTaakDataKey") } returns null
            every { taakVariabelenService.isZaakHervatten(restTaakData) } returns false
            every {
                taakVariabelenService.readOndertekeningen(restTaakData)
            } returns Optional.of(signatureUUID.toString())
            every {
                drcClientService.readEnkelvoudigInformatieobject(signatureUUID)
            } returns enkelvoudigInformatieObject
            every {
                policyService.readDocumentRechten(enkelvoudigInformatieObject, zaak)
            } returns createDocumentRechtenAllDeny(wijzigen = true, ondertekenen = true)
            every {
                enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                    enkelvoudigInformatieObjectUUID
                )
            } just runs

            val restTaakReturned = takenRESTService.completeTaak(restTaak)

            Then(
                "the document is signed, the task is completed and the search index service is invoked"
            ) {
                restTaakReturned shouldBe restTaakConverted
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                        enkelvoudigInformatieObjectUUID
                    )
                    flowableTaskService.completeTask(task)
                }
            }
        }
    }
    Given("REST taak verdeelgegevens to assign two tasks asynchronously") {
        val screenEventResourceId = "dummyScreenEventResourceId"
        val restTaakVerdelenGegevens = createRESTTaakVerdelenGegevens(
            taken = listOf(
                createRESTTaakVerdelenTaak(),
                createRESTTaakVerdelenTaak()
            ),
            screenEventResourceId = screenEventResourceId
        )
        coEvery {
            taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser, screenEventResourceId)
        } just Runs

        When("the 'verdelen vanuit lijst' function is called from user with access") {
            every {
                policyService.readWerklijstRechten()
            } returns createWerklijstRechtenAllDeny(zakenTakenVerdelen = true)

            takenRESTService.verdelenVanuitLijst(restTaakVerdelenGegevens)

            Then("the tasks are assigned to the group and user") {
                coVerify(exactly = 1) {
                    taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser, screenEventResourceId)
                }
            }
        }

        When("the 'verdelen vanuit lijst' function is called from user with no access") {
            every { policyService.readWerklijstRechten() } returns createWerklijstRechtenAllDeny()

            val exception = shouldThrow<PolicyException> {
                takenRESTService.verdelenVanuitLijst(restTaakVerdelenGegevens)
            }

            Then("it throws exception with no message") { exception.message shouldBe null }
        }
    }
    Given("REST taak vrijgeven gegevens to release two tasks asynchronously") {
        val screenEventResourceId = "dummyScreenEventResourceId"
        val restTaakVrijgevenGegevens = createRESTTaakVrijgevenGegevens(
            taken = listOf(
                createRESTTaakVerdelenTaak(),
                createRESTTaakVerdelenTaak()
            ),
            screenEventResourceId = screenEventResourceId
        )
        val werklijstRechten = createWerklijstRechten()
        every { policyService.readWerklijstRechten() } returns werklijstRechten
        coEvery {
            taskService.releaseTasks(restTaakVrijgevenGegevens, loggedInUser, screenEventResourceId)
        } just Runs

        When("the 'verdelen vanuit lijst' function is called") {
            takenRESTService.vrijgevenVanuitLijst(restTaakVrijgevenGegevens)

            Then("the tasks are assigned to the group and user") {
                coVerify(exactly = 1) {
                    taskService.releaseTasks(restTaakVrijgevenGegevens, loggedInUser, screenEventResourceId)
                }
            }
        }
    }
})
