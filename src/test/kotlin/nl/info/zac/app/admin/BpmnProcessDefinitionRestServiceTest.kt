/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.core.Response
import nl.info.test.org.flowable.engine.repository.createProcessDefinition
import nl.info.zac.app.admin.model.RestFormioFormulierContent
import nl.info.zac.app.admin.model.RestProcessDefinitionContent
import nl.info.zac.flowable.bpmn.BpmnProcessDefinitionTaskFormService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.model.createBpmnProcessDefinitionMetadata
import nl.info.zac.flowable.bpmn.model.createBpmnProcessDefinitionTaskForm
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createOverigeRechten

class BpmnProcessDefinitionRestServiceTest : BehaviorSpec({
    val bpmnService = mockk<BpmnService>()
    val policyService = mockk<PolicyService>()
    val bpmnProcessDefinitionTaskFormService = mockk<BpmnProcessDefinitionTaskFormService>()
    val restService = BpmnProcessDefinitionRestService(
        bpmnService,
        policyService,
        bpmnProcessDefinitionTaskFormService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    Given("User has beheren rights and process definitions exist") {
        val processDefinition1 = createProcessDefinition(
            id = "pd1",
            name = "Process 1",
            version = 1,
            key = "process1"
        )
        val processDefinition2 = createProcessDefinition(
            id = "pd2",
            name = "Process 2",
            version = 2,
            key = "process2"
        )
        val processDefinitionsInUse = setOf("process1")
        val processDefinitionsInConfig = setOf("process2")
        val processDefinition1Metadata = createBpmnProcessDefinitionMetadata(formKeys = listOf("form1", "form2"))
        val processDefinition2Metadata = createBpmnProcessDefinitionMetadata(formKeys = listOf("form3"))
        val form1 = createBpmnProcessDefinitionTaskForm(
            bpmnProcessDefinitionKey = processDefinition1.key,
            bpmnProcessDefinitionVersion = processDefinition1.version,
            name = "form1"
        )
        val form2 = createBpmnProcessDefinitionTaskForm(
            bpmnProcessDefinitionKey = processDefinition1.key,
            bpmnProcessDefinitionVersion = processDefinition1.version,
            name = "form2"
        )
        val form3 = createBpmnProcessDefinitionTaskForm(
            bpmnProcessDefinitionKey = processDefinition2.key,
            bpmnProcessDefinitionVersion = processDefinition2.version,
            name = "form3"
        )

        every { policyService.readOverigeRechten() } returns createOverigeRechten(beheren = true)
        every { bpmnService.findUniqueBpmnProcessDefinitionKeysFromProcessInstances() } returns processDefinitionsInUse
        every { bpmnService.findUniqueBpmnProcessDefinitionKeysFromConfigurations() } returns processDefinitionsInConfig
        every { bpmnService.listProcessDefinitions() } returns listOf(processDefinition1, processDefinition2)
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns listOf(form1, form2, form3)
        every { bpmnService.getProcessDefinitionMetadata(processDefinition1) } returns processDefinition1Metadata
        every { bpmnService.getProcessDefinitionMetadata(processDefinition2) } returns processDefinition2Metadata

        When("listProcessDefinitions is called") {
            val result = restService.listProcessDefinitions(true)

            Then("it should return all process definitions with inUse status") {
                result.size shouldBe 2
                result[0].id shouldBe "pd1"
                result[0].name shouldBe "Process 1"
                result[0].version shouldBe 1
                result[0].key shouldBe "process1"
                result[0].details?.inUse shouldBe true
                result[1].id shouldBe "pd2"
                result[1].name shouldBe "Process 2"
                result[1].version shouldBe 2
                result[1].key shouldBe "process2"
                result[1].details?.inUse shouldBe true
            }
        }
    }

    Given("User has beheren rights and a process definition not in use exists") {
        val processDefinition = createProcessDefinition(
            id = "pd3",
            name = "Process 3",
            version = 1,
            key = "process3"
        )
        val processDefinitionMetadata = createBpmnProcessDefinitionMetadata(formKeys = listOf("form1"))
        val form1 = createBpmnProcessDefinitionTaskForm(
            bpmnProcessDefinitionKey = processDefinition.key,
            bpmnProcessDefinitionVersion = processDefinition.version,
            name = "form1"
        )
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns listOf(form1)

        every { policyService.readOverigeRechten() } returns createOverigeRechten(beheren = true)
        every { bpmnService.findUniqueBpmnProcessDefinitionKeysFromProcessInstances() } returns emptySet()
        every { bpmnService.findUniqueBpmnProcessDefinitionKeysFromConfigurations() } returns emptySet()
        every { bpmnService.listProcessDefinitions() } returns listOf(processDefinition)
        every { bpmnService.getProcessDefinitionMetadata(processDefinition) } returns processDefinitionMetadata

        When("listProcessDefinitions is called") {
            val result = restService.listProcessDefinitions(true)

            Then("it should return the process definition with inUse as false") {
                result.size shouldBe 1
                result[0].details?.inUse shouldBe false
            }
        }
    }

    Given("User has beheren rights and wants to create a process definition") {
        val filename = "testProcess.bpmn"
        val content = "<bpmn:definitions>...</bpmn:definitions>"
        val processDefinitionContent = RestProcessDefinitionContent(filename, content)
        val deployment = mockk<org.flowable.engine.repository.Deployment>()

        every { policyService.readOverigeRechten() } returns createOverigeRechten(beheren = true)
        every { bpmnService.addProcessDefinition(filename, content) } returns deployment

        When("createProcessDefinition is called") {
            val response = restService.createProcessDefinition(processDefinitionContent)

            Then("it should create the process definition and return 201 Created") {
                response.status shouldBe Response.Status.CREATED.statusCode
                verify(exactly = 1) { bpmnService.addProcessDefinition(filename, content) }
            }
        }
    }

    Given("User has beheren rights and a process definition not in use needs deletion") {
        val processDefinitionKey = "unusedProcess"

        every { policyService.readOverigeRechten() } returns createOverigeRechten(beheren = true)
        every { bpmnService.isProcessDefinitionInUse(processDefinitionKey) } returns false
        every { bpmnService.deleteProcessDefinition(processDefinitionKey) } just Runs

        When("deleteProcessDefinition is called") {
            val response = restService.deleteProcessDefinition(processDefinitionKey)

            Then("it should delete the process definition and return 204 No Content") {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
                verify(exactly = 1) { bpmnService.deleteProcessDefinition(processDefinitionKey) }
            }
        }
    }

    Given("User has beheren rights and a process definition in use needs deletion") {
        val processDefinitionKey = "usedProcess"

        every { policyService.readOverigeRechten() } returns createOverigeRechten(beheren = true)
        every { bpmnService.isProcessDefinitionInUse(processDefinitionKey) } returns true

        When("deleteProcessDefinition is called") {
            val response = restService.deleteProcessDefinition(processDefinitionKey)

            Then("it should return 400 Bad Request with error message") {
                response.status shouldBe Response.Status.BAD_REQUEST.statusCode
                val entity = response.entity as Map<*, *>
                entity["message"] shouldBe "BPMN process definition 'usedProcess' cannot be deleted as it is in use"
                verify(exactly = 0) { bpmnService.deleteProcessDefinition(any()) }
            }
        }
    }

    Given("User has beheren rights and wants to create a form for a process definition") {
        val processDefinitionKey = "processKey"
        val filename = "testForm.json"
        val content = """{"name": "Test Form", "title": "Test Title"}"""
        val restFormioFormulierContent = RestFormioFormulierContent(filename, content)

        every { policyService.readOverigeRechten() } returns createOverigeRechten(beheren = true)
        every {
            bpmnProcessDefinitionTaskFormService.addForm(processDefinitionKey, filename, content)
        } just Runs

        When("createForm is called") {
            val response = restService.createForm(processDefinitionKey, restFormioFormulierContent)

            Then("it should create the form and return 201 Created") {
                response.status shouldBe Response.Status.CREATED.statusCode
                verify(exactly = 1) {
                    bpmnProcessDefinitionTaskFormService.addForm(processDefinitionKey, filename, content)
                }
            }
        }
    }

    Given("User has beheren rights and wants to delete a form from a process definition which is not in use") {
        val processDefinitionKey = "processKey"
        val formName = "testForm"

        every { policyService.readOverigeRechten() } returns createOverigeRechten(beheren = true)
        every { bpmnService.isProcessDefinitionInUse(processDefinitionKey) } returns false
        every {
            bpmnProcessDefinitionTaskFormService.deleteForm(processDefinitionKey, formName)
        } just Runs

        When("deleteForm is called") {
            val response = restService.deleteForm(processDefinitionKey, formName)

            Then("it should delete the form and return 204 No Content") {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
                verify(exactly = 1) {
                    bpmnProcessDefinitionTaskFormService.deleteForm(processDefinitionKey, formName)
                }
            }
        }
    }

    Given(
        "User has beheren rights and wants to delete a form from a process definition which is in use, but the form is orphaned"
    ) {
        val processDefinitionKey = "processKey"
        val formName = "testForm"
        val processDefinition = createProcessDefinition(
            id = "pd3",
            name = "Process 3",
            version = 1,
            key = processDefinitionKey,
        )
        val metadata = createBpmnProcessDefinitionMetadata()

        every { policyService.readOverigeRechten() } returns createOverigeRechten(beheren = true)
        every { bpmnService.isProcessDefinitionInUse(processDefinitionKey) } returns true
        every {
            bpmnProcessDefinitionTaskFormService.deleteForm(processDefinitionKey, formName)
        } just Runs
        every { bpmnService.findProcessDefinitionByProcessDefinitionKey(processDefinitionKey) } returns processDefinition
        every { bpmnService.getProcessDefinitionMetadata(processDefinition) } returns metadata

        When("deleteForm is called") {
            val response = restService.deleteForm(processDefinitionKey, formName)

            Then("it should delete the form and return 204 No Content") {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
                verify(exactly = 1) {
                    bpmnProcessDefinitionTaskFormService.deleteForm(processDefinitionKey, formName)
                }
            }
        }
    }

    Given("User has beheren rights and wants to delete a form from a process definition which is in use") {
        val processDefinitionKey = "processKey"
        val formName = "testForm"
        val processDefinition = createProcessDefinition(
            id = "pd3",
            name = "Process 3",
            version = 1,
            key = processDefinitionKey,
        )
        val metadata = createBpmnProcessDefinitionMetadata(formKeys = listOf(formName))

        every { policyService.readOverigeRechten() } returns createOverigeRechten(beheren = true)
        every { bpmnService.isProcessDefinitionInUse(processDefinitionKey) } returns true
        every { bpmnService.findProcessDefinitionByProcessDefinitionKey(processDefinitionKey) } returns processDefinition
        every { bpmnService.getProcessDefinitionMetadata(processDefinition) } returns metadata

        When("deleteForm is called") {
            val response = restService.deleteForm(processDefinitionKey, formName)

            Then("it should return 400 Bad Request with error message") {
                response.status shouldBe Response.Status.BAD_REQUEST.statusCode
                val entity = response.entity as Map<*, *>
                entity["message"] shouldBe "BPMN process definition form 'testForm' cannot be deleted as it is in use" +
                    " by process definition 'processKey'"
                verify(exactly = 0) { bpmnService.deleteProcessDefinition(any()) }
            }
        }
    }
})
