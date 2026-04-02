/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.SoortEnum
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.model.createEnkelvoudigInformatieObjectLock
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createTaakRechten
import java.time.LocalDate
import java.util.UUID

class EnkelvoudigInformatieObjectUpdateServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val taakVariabelenService = mockk<TaakVariabelenService>()
    val zgwApiService = mockk<ZgwApiService>()
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
    val zaakInformatieObject = createZaakInformatieobjectForCreatesAndUpdates()
    val taskId = "1234"
    val task = createTestTask()

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Creating a zaak informatie object for a zaak") {
        Given("Zaak, lock request and an open task") {
            every {
                zgwApiService.createZaakInformatieobjectForZaak(
                    zaak,
                    enkelvoudigInformatieObjectCreateLockRequest,
                    enkelvoudigInformatieObjectCreateLockRequest.titel,
                    enkelvoudigInformatieObjectCreateLockRequest.beschrijving,
                    ConfigurationService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
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
                    ConfigurationService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
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
                    ConfigurationService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
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
    }

    Context("Updating enkelvoudig informatieobjecten with lock data") {
        Given("An enkelvoudig informatie object and no existing lock") {
            val enkelvoudigInformatieObjectUUID = UUID.randomUUID()
            val enkelvoudigInformatieObjectWithLockRequest = createEnkelvoudigInformatieObjectWithLockRequest()
            val explanation = "fakeExplanation"
            val userId = "fakeUserId"
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
            every { loggedInUserInstance.get().id } returns userId
            every { enkelvoudigInformatieObjectLockService.findLock(enkelvoudigInformatieObjectUUID) } returns null
            every {
                enkelvoudigInformatieObjectLockService.createLock(enkelvoudigInformatieObjectUUID, userId)
            } returns enkelvoudigInformatieObjectLock
            every {
                drcClientService.updateEnkelvoudigInformatieobject(
                    enkelvoudigInformatieObjectUUID, enkelvoudigInformatieObjectWithLockRequest, explanation
                )
            } returns enkelvoudigInformatieObject
            every {
                enkelvoudigInformatieObjectLockService.deleteLock(enkelvoudigInformatieObjectUUID)
            } returns Unit

            When("updating the object with lock data") {
                val updatedEnkelvoudigInformatieObject =
                    enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                        enkelvoudigInformatieObjectUUID,
                        enkelvoudigInformatieObjectWithLockRequest,
                        explanation
                    )

                Then("the object is updated successfully") {
                    updatedEnkelvoudigInformatieObject shouldBe enkelvoudigInformatieObject
                    verify(exactly = 1) {
                        drcClientService.updateEnkelvoudigInformatieobject(
                            enkelvoudigInformatieObjectUUID,
                            enkelvoudigInformatieObjectWithLockRequest,
                            explanation
                        )
                    }
                }
                And("a temporary lock is created and deleted") {
                    verify(exactly = 1) {
                        enkelvoudigInformatieObjectLockService.createLock(enkelvoudigInformatieObjectUUID, userId)
                        enkelvoudigInformatieObjectLockService.deleteLock(enkelvoudigInformatieObjectUUID)
                    }
                }
            }
        }

        Given("An enkelvoudig informatie object and an existing lock") {
            val enkelvoudigInformatieObjectUUID = UUID.randomUUID()
            val enkelvoudigInformatieObjectWithLockRequest = createEnkelvoudigInformatieObjectWithLockRequest()
            val explanation = "fakeExplanation"
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
            every {
                enkelvoudigInformatieObjectLockService.findLock(enkelvoudigInformatieObjectUUID)
            } returns enkelvoudigInformatieObjectLock
            every {
                drcClientService.updateEnkelvoudigInformatieobject(
                    enkelvoudigInformatieObjectUUID, enkelvoudigInformatieObjectWithLockRequest, explanation
                )
            } returns enkelvoudigInformatieObject

            When("updating the object with lock data") {
                val updatedEnkelvoudigInformatieObject =
                    enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                        enkelvoudigInformatieObjectUUID,
                        enkelvoudigInformatieObjectWithLockRequest,
                        explanation
                    )

                Then("the object is updated successfully") {
                    updatedEnkelvoudigInformatieObject shouldBe enkelvoudigInformatieObject
                    verify(exactly = 1) {
                        drcClientService.updateEnkelvoudigInformatieobject(
                            enkelvoudigInformatieObjectUUID,
                            enkelvoudigInformatieObjectWithLockRequest,
                            explanation
                        )
                    }
                }
                And("no temporary lock is created nor deleted") {
                    verify(exactly = 0) {
                        enkelvoudigInformatieObjectLockService.createLock(any(), any())
                        enkelvoudigInformatieObjectLockService.deleteLock(any())
                    }
                }
            }
        }
    }

    Context("Sending an enkelvoudig informatie object") {
        val uuid = UUID.randomUUID()
        val verzenddatum = LocalDate.now()
        val userId = "fakeUserId"

        Given("A UUID, verzenddatum and a toelichting") {
            val toelichting = "fakeToelichting"
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
            val requestSlot = slot<EnkelvoudigInformatieObjectWithLockRequest>()
            every { loggedInUserInstance.get().id } returns userId
            every { enkelvoudigInformatieObjectLockService.findLock(uuid) } returns null
            every { enkelvoudigInformatieObjectLockService.createLock(uuid, userId) } returns enkelvoudigInformatieObjectLock
            every { drcClientService.updateEnkelvoudigInformatieobject(uuid, capture(requestSlot), any()) } returns enkelvoudigInformatieObject
            every { enkelvoudigInformatieObjectLockService.deleteLock(uuid) } returns Unit

            When("verzendEnkelvoudigInformatieObject is called") {
                enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(uuid, verzenddatum, toelichting)

                Then("the verzenddatum is set in the request") {
                    requestSlot.captured.verzenddatum shouldBe verzenddatum
                }
                And("the toelichting is prefixed with 'Per post'") {
                    verify(exactly = 1) {
                        drcClientService.updateEnkelvoudigInformatieobject(uuid, any(), "Per post: $toelichting")
                    }
                }
            }
        }

        Given("A UUID, verzenddatum and no toelichting") {
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
            every { loggedInUserInstance.get().id } returns userId
            every { enkelvoudigInformatieObjectLockService.findLock(uuid) } returns null
            every { enkelvoudigInformatieObjectLockService.createLock(uuid, userId) } returns enkelvoudigInformatieObjectLock
            every { drcClientService.updateEnkelvoudigInformatieobject(uuid, any(), any()) } returns enkelvoudigInformatieObject
            every { enkelvoudigInformatieObjectLockService.deleteLock(uuid) } returns Unit

            When("verzendEnkelvoudigInformatieObject is called with null toelichting") {
                enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(uuid, verzenddatum, null)

                Then("the toelichting is just the prefix without a colon") {
                    verify(exactly = 1) {
                        drcClientService.updateEnkelvoudigInformatieobject(uuid, any(), "Per post")
                    }
                }
            }
        }
    }

    Context("Signing an enkelvoudig informatie object") {
        val uuid = UUID.randomUUID()
        val userId = "fakeUserId"

        Given("A UUID of an unsigned document") {
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
            val requestSlot = slot<EnkelvoudigInformatieObjectWithLockRequest>()
            every { loggedInUserInstance.get().id } returns userId
            every { enkelvoudigInformatieObjectLockService.findLock(uuid) } returns null
            every { enkelvoudigInformatieObjectLockService.createLock(uuid, userId) } returns enkelvoudigInformatieObjectLock
            every { drcClientService.updateEnkelvoudigInformatieobject(uuid, capture(requestSlot), any()) } returns enkelvoudigInformatieObject
            every { enkelvoudigInformatieObjectLockService.deleteLock(uuid) } returns Unit

            When("ondertekenEnkelvoudigInformatieObject is called") {
                enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(uuid)

                Then("the ondertekening is set to DIGITAAL with today's date") {
                    requestSlot.captured.ondertekening!!.soort shouldBe SoortEnum.DIGITAAL
                    requestSlot.captured.ondertekening!!.datum shouldBe LocalDate.now()
                }
                And("the status is set to DEFINITIEF") {
                    requestSlot.captured.status shouldBe StatusEnum.DEFINITIEF
                }
                And("the toelichting is 'Door ondertekenen'") {
                    verify(exactly = 1) {
                        drcClientService.updateEnkelvoudigInformatieobject(uuid, any(), "Door ondertekenen")
                    }
                }
            }
        }
    }
})
