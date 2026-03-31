/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.json.Json
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.createOndertekening
import nl.info.client.zgw.model.createOpschorting
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.app.task.model.createRestTask
import nl.info.zac.identity.IdentityService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import java.net.URI
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

class BpmnTaskFormRuntimeServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val identityService = mockk<IdentityService>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val taakVariabelenService = mockk<TaakVariabelenService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zgwApiService = mockk<ZgwApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val service = BpmnTaskFormRuntimeService(
        zgwApiService = zgwApiService,
        zrcClientService = zrcClientService,
        zaakVariabelenService = zaakVariabelenService,
        identityService = identityService,
        suspensionZaakHelper = suspensionZaakHelper,
        drcClientService = drcClientService,
        enkelvoudigInformatieObjectUpdateService = enkelvoudigInformatieObjectUpdateService,
        taakVariabelenService = taakVariabelenService,
        flowableTaskService = flowableTaskService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A formio task with empty taakdata") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val task = createTestTask()
        val restTask = createRestTask(zaakUuid = zaakUuid, taakData = mutableMapOf())

        every { taakVariabelenService.setTaskinformation(any(), any()) } just runs
        every { taakVariabelenService.setTaskData(any(), any()) } just runs
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()
        every { zaakVariabelenService.setZaakdata(zaakUuid, any()) } just runs

        When("submit is called") {
            val result = service.submit(restTask, task, zaak)

            Then("sets task info and data, updates zaak variables, and returns the original task") {
                result shouldBe task
                verify(exactly = 1) { taakVariabelenService.setTaskinformation(task, restTask.taakinformatie) }
                verify(exactly = 1) { taakVariabelenService.setTaskData(task, restTask.taakdata) }
                verify(exactly = 1) { zaakVariabelenService.setZaakdata(zaakUuid, any()) }
                verify(exactly = 0) { flowableTaskService.updateTask(any()) }
                verify(exactly = 0) { suspensionZaakHelper.suspendZaak(any(), any(), any()) }
                verify(exactly = 0) { suspensionZaakHelper.resumeZaak(any(), any()) }
            }
        }
    }

    Given("A formio task with toelichting in taakdata") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val task = createTestTask()
        val updatedTask = createTestTask(description = "updated description")
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            taakData = mutableMapOf("toelichting" to "updated description")
        )

        every { taakVariabelenService.setTaskinformation(any(), any()) } just runs
        every { taakVariabelenService.setTaskData(any(), any()) } just runs
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()
        every { zaakVariabelenService.setZaakdata(zaakUuid, any()) } just runs
        every { flowableTaskService.updateTask(any()) } returns updatedTask

        When("submit is called") {
            val result = service.submit(restTask, task, zaak)

            Then("updates the task description via flowableTaskService and returns the updated task") {
                result shouldBe updatedTask
                verify(exactly = 1) { flowableTaskService.updateTask(any()) }
            }
        }
    }

    Given("A formio task with zaak-opschorten=true and a non-suspended zaak") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val dueDate = Date.from(LocalDate.now().plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant())
        val task = createTestTask(dueDate = dueDate)
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            taakData = mutableMapOf("zaak-opschorten" to "true")
        )

        every { taakVariabelenService.setTaskinformation(any(), any()) } just runs
        every { taakVariabelenService.setTaskData(any(), any()) } just runs
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()
        every { zaakVariabelenService.setZaakdata(zaakUuid, any()) } just runs
        every { suspensionZaakHelper.suspendZaak(any(), any(), any()) } returns zaak

        When("submit is called") {
            service.submit(restTask, task, zaak)

            Then("suspends the zaak") {
                verify(exactly = 1) { suspensionZaakHelper.suspendZaak(zaak, any(), any()) }
            }
        }
    }

    Given("A formio task with zaak-opschorten=true and an already suspended zaak") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid, opschorting = createOpschorting(indicatie = true))
        val dueDate = Date.from(LocalDate.now().plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant())
        val task = createTestTask(dueDate = dueDate)
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            taakData = mutableMapOf("zaak-opschorten" to "true")
        )

        every { taakVariabelenService.setTaskinformation(any(), any()) } just runs
        every { taakVariabelenService.setTaskData(any(), any()) } just runs
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()
        every { zaakVariabelenService.setZaakdata(zaakUuid, any()) } just runs

        When("submit is called") {
            service.submit(restTask, task, zaak)

            Then("does not suspend the zaak again") {
                verify(exactly = 0) { suspensionZaakHelper.suspendZaak(any(), any(), any()) }
            }
        }
    }

    Given("A formio task with zaak-hervatten=true and a suspended zaak") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid, opschorting = createOpschorting(indicatie = true))
        val task = createTestTask()
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            taakData = mutableMapOf("zaak-hervatten" to "true")
        )

        every { taakVariabelenService.setTaskinformation(any(), any()) } just runs
        every { taakVariabelenService.setTaskData(any(), any()) } just runs
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()
        every { zaakVariabelenService.setZaakdata(zaakUuid, any()) } just runs
        every { suspensionZaakHelper.resumeZaak(any(), any(), any()) } returns zaak

        When("submit is called") {
            service.submit(restTask, task, zaak)

            Then("resumes the zaak") {
                verify(exactly = 1) { suspensionZaakHelper.resumeZaak(zaak, "Zaak hervat vanuit proces", any()) }
            }
        }
    }

    Given("A formio task with zaak-hervatten=true and a non-suspended zaak") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val task = createTestTask()
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            taakData = mutableMapOf("zaak-hervatten" to "true")
        )

        every { taakVariabelenService.setTaskinformation(any(), any()) } just runs
        every { taakVariabelenService.setTaskData(any(), any()) } just runs
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()
        every { zaakVariabelenService.setZaakdata(zaakUuid, any()) } just runs

        When("submit is called") {
            service.submit(restTask, task, zaak)

            Then("does not resume the zaak") {
                verify(exactly = 0) { suspensionZaakHelper.resumeZaak(any(), any()) }
            }
        }
    }

    Given("A formio task with documenten-verzenden referencing one document") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val task = createTestTask()
        val documentUuid = UUID.randomUUID()
        val document = createEnkelvoudigInformatieObject(
            uuid = documentUuid,
            url = URI("http://example.com/$documentUuid")
        )
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            taakData = mutableMapOf("documenten-verzenden" to documentUuid.toString())
        )

        every { taakVariabelenService.setTaskinformation(any(), any()) } just runs
        every { taakVariabelenService.setTaskData(any(), any()) } just runs
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()
        every { zaakVariabelenService.setZaakdata(zaakUuid, any()) } just runs
        every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns document
        every {
            enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(any(), any(), any())
        } just runs

        When("submit is called") {
            service.submit(restTask, task, zaak)

            Then("marks the document as sent") {
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(
                        documentUuid,
                        any(),
                        any()
                    )
                }
            }
        }
    }

    Given("A formio task with documenten-onderteken referencing an unsigned document") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val task = createTestTask()
        val documentUuid = UUID.randomUUID()
        val unsignedDocument = createEnkelvoudigInformatieObject(
            uuid = documentUuid,
            url = URI("http://example.com/$documentUuid"),
            ondertekening = null
        )
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            taakData = mutableMapOf("documenten-onderteken" to documentUuid.toString())
        )

        every { taakVariabelenService.setTaskinformation(any(), any()) } just runs
        every { taakVariabelenService.setTaskData(any(), any()) } just runs
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()
        every { zaakVariabelenService.setZaakdata(zaakUuid, any()) } just runs
        every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns unsignedDocument
        every { enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(any()) } just runs

        When("submit is called") {
            service.submit(restTask, task, zaak)

            Then("signs the document") {
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(documentUuid)
                }
            }
        }
    }

    Given("A formio task with documenten-onderteken referencing an already signed document") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val task = createTestTask()
        val documentUuid = UUID.randomUUID()
        val signedDocument = createEnkelvoudigInformatieObject(
            uuid = documentUuid,
            url = URI("http://example.com/$documentUuid"),
            ondertekening = createOndertekening()
        )
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            taakData = mutableMapOf("documenten-onderteken" to documentUuid.toString())
        )

        every { taakVariabelenService.setTaskinformation(any(), any()) } just runs
        every { taakVariabelenService.setTaskData(any(), any()) } just runs
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()
        every { zaakVariabelenService.setZaakdata(zaakUuid, any()) } just runs
        every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns signedDocument

        When("submit is called") {
            service.submit(restTask, task, zaak)

            Then("skips signing the already-signed document") {
                verify(exactly = 0) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(any())
                }
            }
        }
    }

    Given("A task with no formio formulier") {
        val restTask = createRestTask(formioFormulier = null)

        When("renderFormioFormulier is called") {
            val result = service.renderFormioFormulier(restTask)

            Then("returns null") {
                result shouldBe null
            }
        }
    }

    Given("A formio formulier with a TAAK:STARTDATUM defaultValue") {
        val zaakUuid = UUID.randomUUID()
        val creationDateTime = ZonedDateTime.now()
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            formioFormulier = Json.createObjectBuilder()
                .add(
                    "components",
                    Json.createArrayBuilder().add(
                        Json.createObjectBuilder()
                            .add("key", "startdatum")
                            .add("defaultValue", "TAAK:STARTDATUM")
                    )
                )
                .build()
        ).apply { this.creatiedatumTijd = creationDateTime }

        every { zrcClientService.readZaak(zaakUuid) } returns createZaak(uuid = zaakUuid)
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()

        When("renderFormioFormulier is called") {
            val result = service.renderFormioFormulier(restTask)

            Then("resolves the defaultValue to the formatted task creation date") {
                val resolvedDefault = result
                    ?.getJsonArray("components")
                    ?.getJsonObject(0)
                    ?.getString("defaultValue")
                resolvedDefault shouldBe creationDateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            }
        }
    }

    Given("A formio formulier with a zaakdata-prefixed defaultValue") {
        val zaakUuid = UUID.randomUUID()
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            formioFormulier = Json.createObjectBuilder()
                .add("key", "customField")
                .add("defaultValue", ":myZaakKey")
                .build()
        )

        every { zrcClientService.readZaak(zaakUuid) } returns createZaak(uuid = zaakUuid)
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns mapOf("myZaakKey" to "myZaakValue")

        When("renderFormioFormulier is called") {
            val result = service.renderFormioFormulier(restTask)

            Then("resolves the defaultValue from the zaakdata map") {
                result?.getString("defaultValue") shouldBe "myZaakValue"
            }
        }
    }

    Given("A formio formulier with an unrecognised defaultValue") {
        val zaakUuid = UUID.randomUUID()
        val restTask = createRestTask(
            zaakUuid = zaakUuid,
            formioFormulier = Json.createObjectBuilder()
                .add("key", "someField")
                .add("defaultValue", "some-literal-value")
                .build()
        )

        every { zrcClientService.readZaak(zaakUuid) } returns createZaak(uuid = zaakUuid)
        every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns emptyMap()

        When("renderFormioFormulier is called") {
            val result = service.renderFormioFormulier(restTask)

            Then("returns the literal value unchanged") {
                result?.getString("defaultValue") shouldBe "some-literal-value"
            }
        }
    }
})
