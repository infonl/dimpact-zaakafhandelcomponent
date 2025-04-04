/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten

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
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.flowable.createTestTask
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createTaakRechten
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.model.createZaakInformatieobject
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import java.util.UUID

class EnkelvoudigInformatieObjectUpdateServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val taakVariabelenService = mockk<TaakVariabelenService>()
    val zgwApiService = mockk<ZGWApiService>()

    val enkelvoudigInformatieObjectUpdateService = EnkelvoudigInformatieObjectUpdateService(
        drcClientService,
        enkelvoudigInformatieObjectLockService,
        flowableTaskService,
        loggedInUserInstance,
        policyService,
        taakVariabelenService,
        zgwApiService
    )

    val zaak = Zaak()
    val enkelvoudigInformatieObjectCreateLockRequest = createEnkelvoudigInformatieObjectCreateLockRequest()
    val zaakInformatieObject = createZaakInformatieobject()
    val taskId = "1234"
    val task = createTestTask()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Zaak, lock request and an open task") {
        every {
            zgwApiService.createZaakInformatieobjectForZaak(
                zaak,
                enkelvoudigInformatieObjectCreateLockRequest,
                enkelvoudigInformatieObjectCreateLockRequest.titel,
                enkelvoudigInformatieObjectCreateLockRequest.beschrijving,
                ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
            )
        } returns zaakInformatieObject
        every { flowableTaskService.findOpenTask(taskId) } returns task
        every { taakVariabelenService.setTaakdocumenten(task, any<List<UUID>>()) } just runs

        When("creating information object for a task is called") {
            every { policyService.readTaakRechten(task) } returns createTaakRechten()

            val zaakInfo = enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                zaak = zaak,
                enkelvoudigInformatieObjectCreateLockRequest = enkelvoudigInformatieObjectCreateLockRequest,
                taskId = taskId
            )

            Then("correct zaak info object is returned") {
                zaakInfo shouldBe zaakInformatieObject
            }

            And("task document is set") {
                verify(exactly = 1) {
                    taakVariabelenService.setTaakdocumenten(task, any<List<UUID>>())
                }
            }
        }
    }

    Given("Zaak, lock request and non-eligible task") {
        every {
            zgwApiService.createZaakInformatieobjectForZaak(
                zaak,
                enkelvoudigInformatieObjectCreateLockRequest,
                enkelvoudigInformatieObjectCreateLockRequest.titel,
                enkelvoudigInformatieObjectCreateLockRequest.beschrijving,
                ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
            )
        } returns zaakInformatieObject
        every { flowableTaskService.findOpenTask(taskId) } returns null

        When("creating information object for a non-open task") {
            val exception = shouldThrow<TaskNotFoundException> {
                enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                    zaak = zaak,
                    enkelvoudigInformatieObjectCreateLockRequest = enkelvoudigInformatieObjectCreateLockRequest,
                    taskId = taskId,
                )
            }

            Then("thrown exception mentions the task id") {
                exception.message shouldBe "No open task found with task id: '$taskId'"
            }
        }
    }

    Given("Zaak, lock request and internal (pre-authenticated) call") {
        every {
            zgwApiService.createZaakInformatieobjectForZaak(
                zaak,
                enkelvoudigInformatieObjectCreateLockRequest,
                enkelvoudigInformatieObjectCreateLockRequest.titel,
                enkelvoudigInformatieObjectCreateLockRequest.beschrijving,
                ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
            )
        } returns zaakInformatieObject
        every { flowableTaskService.findOpenTask(taskId) } returns task
        every { taakVariabelenService.setTaakdocumenten(task, any<List<UUID>>()) } just runs

        When("creating information object for a non-open task") {
            enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                zaak = zaak,
                enkelvoudigInformatieObjectCreateLockRequest = enkelvoudigInformatieObjectCreateLockRequest,
                taskId = taskId,
                skipPolicyCheck = true
            )

            Then("policy check is skipped") {
                verify(exactly = 0) {
                    policyService.readTaakRechten(task)
                }
            }
        }
    }
})
