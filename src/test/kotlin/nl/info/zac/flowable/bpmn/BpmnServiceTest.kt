/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAKTYPE_OMSCHRIJVING
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAKTYPE_UUID
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_IDENTIFICATIE
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_UUID
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.model.createReferentieProcess
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.test.org.flowable.engine.repository.createHistoricProcessInstance
import nl.info.test.org.flowable.engine.repository.createProcessDefinition
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.flowable.bpmn.exception.BpmnProcessDefinitionNotFoundException
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import org.flowable.bpmn.model.BpmnModel
import org.flowable.bpmn.model.ExtensionElement
import org.flowable.bpmn.model.Process
import org.flowable.bpmn.model.UserTask
import org.flowable.engine.HistoryService
import org.flowable.engine.ProcessEngine
import org.flowable.engine.RepositoryService
import org.flowable.engine.RuntimeService
import org.flowable.engine.repository.Deployment
import org.flowable.engine.repository.DeploymentQuery
import org.flowable.engine.runtime.ProcessInstance
import org.flowable.engine.runtime.ProcessInstanceBuilder
import java.net.URI
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID

class BpmnServiceTest : BehaviorSpec({
    val repositoryService = mockk<RepositoryService>()
    val runtimeService = mockk<RuntimeService>()
    val historyService = mockk<HistoryService>()
    val processEngine = mockk<ProcessEngine>()
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val bpmnProcessDefinitionTaskFormService = mockk<BpmnProcessDefinitionTaskFormService>()
    val bpmnService = BpmnService(
        repositoryService,
        runtimeService,
        historyService,
        processEngine,
        zaaktypeBpmnConfigurationBeheerService,
        bpmnProcessDefinitionTaskFormService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    Given("A UUID for which a BPMN process instance exists") {
        val uuid = UUID.randomUUID()
        val processInstance = mockk<ProcessInstance>()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(uuid.toString())
                .singleResult()
        } returns processInstance

        When("a check is done to see if the zaak is process driven") {
            val isProcessDriven = bpmnService.isZaakProcessDriven(uuid)

            Then("'true is returned") {
                isProcessDriven shouldBe true
            }
        }
    }

    Given("A UUID for which no BPMN process instance exists") {
        val uuid = UUID.randomUUID()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(uuid.toString())
                .singleResult()
        } returns null

        When("a check is done to see if the zaak is process driven") {
            val isProcessDriven = bpmnService.isZaakProcessDriven(uuid)

            Then("'false is returned") {
                isProcessDriven shouldBe false
            }
        }
    }

    Given("A zaak and zaakdata and a zaaktype with a 'referentieproces'") {
        val referentieProcesName = "fakeReferentieProces"
        val zaakTypeUUID = UUID.randomUUID()
        val zaakUUID = UUID.randomUUID()
        val zaakType = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID"),
            referentieProces = createReferentieProcess(name = referentieProcesName)
        )
        val zaak = createZaak(
            zaaktypeUri = zaakType.url,
            uuid = zaakUUID
        )
        val zaakData = mapOf<String, Any>("fakeKey" to "fakeValue")
        val processInstanceBuilder = mockk<ProcessInstanceBuilder>()
        val processInstance = mockk<ProcessInstance>()
        every {
            runtimeService.createProcessInstanceBuilder()
        } returns processInstanceBuilder
        every {
            processInstanceBuilder
                .processDefinitionKey(referentieProcesName)
                .businessKey(zaakUUID.toString())
                .variable(VAR_ZAAK_UUID, zaakUUID)
                .variable(VAR_ZAAK_IDENTIFICATIE, zaak.identificatie)
                .variable(VAR_ZAAKTYPE_UUID, zaakTypeUUID)
                .variable(VAR_ZAAKTYPE_OMSCHRIJVING, zaakType.omschrijving)
        } returns processInstanceBuilder
        every { processInstanceBuilder.variables(zaakData) } returns processInstanceBuilder
        every { processInstanceBuilder.start() } returns processInstance

        When("the zaak is started using a BPMN process definition") {
            bpmnService.startProcess(zaak, zaakType, referentieProcesName, zaakData)

            Then("a Flowable BPMN process instance should be started") {
                verify(exactly = 1) {
                    processInstanceBuilder.start()
                }
            }
        }
    }
    Given("A valid zaaktype UUID with a process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
        every {
            zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUUID)
        } returns zaaktypeBpmnProcessDefinition

        When("finding the process definition for the zaaktype") {
            val result = bpmnService.findProcessDefinitionForZaaktype(zaaktypeUUID)

            Then("the correct process definition is returned") {
                result shouldBe zaaktypeBpmnProcessDefinition
            }
        }
    }

    Given("A valid zaaktype UUID without a process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        every { zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUUID) } returns null

        When("finding the process definition for the zaaktype") {
            val exception = shouldThrow<BpmnProcessDefinitionNotFoundException> {
                bpmnService.findProcessDefinitionForZaaktype(zaaktypeUUID)
            }

            Then("null is returned") {
                exception.message shouldContain "$zaaktypeUUID"
            }
        }
    }

    Given("A valid process definition key with an existing process definition") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        val processDefinition = createProcessDefinition()
        every { bpmnService.findProcessDefinitionByProcessDefinitionKey(processDefinitionKey) } returns processDefinition

        When("reading the process definition by process definition key") {
            val result = bpmnService.readProcessDefinitionByProcessDefinitionKey(processDefinitionKey)

            Then("the correct process definition is returned") {
                result shouldBe processDefinition
            }
        }
    }

    Given("An invalid process definition key with no existing process definition") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every { bpmnService.findProcessDefinitionByProcessDefinitionKey(processDefinitionKey) } returns null

        When("reading the process definition by process definition key") {
            val exception = shouldThrow<BpmnProcessDefinitionNotFoundException> {
                bpmnService.readProcessDefinitionByProcessDefinitionKey(processDefinitionKey)
            }

            Then("a 'process definition not found exception' is thrown") {
                exception.message shouldBe "No BPMN process definition found for process definition key: '$processDefinitionKey'"
            }
        }
    }

    Given("A valid zaaktype UUID with an existing process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        val processInstance = mockk<ProcessInstance>()
        val processInstanceId = "fakeProcessInstanceId"
        every {
            processInstance.id
        } returns processInstanceId
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaaktypeUUID.toString())
                .singleResult()
        } returns processInstance
        every {
            runtimeService.deleteProcessInstance(processInstanceId, null)
        } returns Unit

        When("Terminating the process instance by zaak UUID") {
            bpmnService.terminateCase(zaaktypeUUID)

            Then("the process instance is terminated") {
                verify(exactly = 1) {
                    runtimeService.deleteProcessInstance(processInstanceId, null)
                }
            }
        }
    }

    Given("valid zaaktype UUID without an existing process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaaktypeUUID.toString())
                .singleResult()
        } returns null

        When("Terminating the process instance by zaak UUID") {
            bpmnService.terminateCase(zaaktypeUUID)

            Then("the process instance is not found") {
                verify(exactly = 0) {
                    runtimeService.deleteProcessInstance(any(), null)
                }
            }
        }
    }

    Given("Process definitions") {
        val historyProcessInstance1 = createHistoricProcessInstance(processDefinitionKey = "fakeKey1")
        val historyProcessInstance2 = createHistoricProcessInstance(processDefinitionKey = "fakeKey2")
        val historyProcessInstance3 = createHistoricProcessInstance(processDefinitionKey = "fakeKey1")
        every {
            historyService.createHistoricProcessInstanceQuery().list()
        } returns listOf(historyProcessInstance1, historyProcessInstance2, historyProcessInstance3)

        When("Returning a list of unique BPMN process definition keys used in process instances") {
            val result = bpmnService.findUniqueBpmnProcessDefinitionKeysFromProcessInstances()

            Then("the unique BPMN process definition keys are returned") {
                result shouldBe setOf("fakeKey1", "fakeKey2")
            }
        }
    }

    Given("process definition key with current or historic process instances") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every {
            historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count()
        } returns 2

        When("checking it has process instances by process definition key") {
            val result = bpmnService.hasProcessInstances(processDefinitionKey)

            Then("true is returned") {
                result shouldBe true
            }
        }
    }

    Given("process definition key without current or historic process instances") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every {
            historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count()
        } returns 0

        When("checking it has process instances by process definition key") {
            val result = bpmnService.hasProcessInstances(processDefinitionKey)

            Then("false is returned") {
                result shouldBe false
            }
        }
    }

    Given("process definition key with linked configurations") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        val linkedProcessDefinitionKeys = listOf(processDefinitionKey, "otherProcessDefinitionKey")
        every {
            zaaktypeBpmnConfigurationBeheerService.findUniqueBpmnProcessDefinitionKeysFromZaaktypeConfigurations()
        } returns linkedProcessDefinitionKeys

        When("checking it has linked configurations by process definition key") {
            val result = bpmnService.hasLinkedZaaktypeBpmnConfiguration(processDefinitionKey)

            Then("true is returned") {
                result shouldBe true
            }
        }
    }

    Given("process definition key without linked configurations") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        val linkedProcessDefinitionKeys = listOf("otherProcessDefinitionKey")
        every {
            zaaktypeBpmnConfigurationBeheerService.findUniqueBpmnProcessDefinitionKeysFromZaaktypeConfigurations()
        } returns linkedProcessDefinitionKeys

        When("checking it has linked configurations by process definition key") {
            val result = bpmnService.hasLinkedZaaktypeBpmnConfiguration(processDefinitionKey)

            Then("false is returned") {
                result shouldBe false
            }
        }
    }

    Given("process definition key with current or historic process instances and linked configurations not checked") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every {
            historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count()
        } returns 3

        When("checking the process definition is in use by process definition key") {
            val result = bpmnService.isProcessDefinitionInUse(processDefinitionKey)

            Then("true is returned") {
                result shouldBe true
                verify(exactly = 0) {
                    zaaktypeBpmnConfigurationBeheerService.findUniqueBpmnProcessDefinitionKeysFromZaaktypeConfigurations()
                }
            }
        }
    }

    Given("process definition key with no current or historic process instances and linked configurations") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every {
            historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count()
        } returns 0
        val linkedProcessDefinitionKeys = listOf(processDefinitionKey, "otherProcessDefinitionKey")
        every {
            zaaktypeBpmnConfigurationBeheerService.findUniqueBpmnProcessDefinitionKeysFromZaaktypeConfigurations()
        } returns linkedProcessDefinitionKeys

        When("checking the process definition is in use by process definition key") {
            val result = bpmnService.isProcessDefinitionInUse(processDefinitionKey)

            Then("true is returned") {
                result shouldBe true
            }
        }
    }

    Given("process definition key with no current or historic process instances and no linked configurations") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every {
            historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count()
        } returns 0
        val linkedProcessDefinitionKeys = listOf("otherProcessDefinitionKey")
        every {
            zaaktypeBpmnConfigurationBeheerService.findUniqueBpmnProcessDefinitionKeysFromZaaktypeConfigurations()
        } returns linkedProcessDefinitionKeys

        When("checking the process definition is in use by process definition key") {
            val result = bpmnService.isProcessDefinitionInUse(processDefinitionKey)

            Then("false is returned") {
                result shouldBe false
            }
        }
    }

    Given("A zaak UUID for which a BPMN process instance and process definition exist") {
        val zaakUUID = UUID.randomUUID()
        val processDefinitionId = "fakeProcessDefinitionId"
        val processInstance = mockk<ProcessInstance>()
        val processDefinition = createProcessDefinition()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .singleResult()
        } returns processInstance
        every { processInstance.processDefinitionId } returns processDefinitionId
        every { repositoryService.getProcessDefinition(processDefinitionId) } returns processDefinition

        When("finding the process definition by zaak UUID") {
            val result = bpmnService.findProcessDefinitionByZaak(zaakUUID)

            Then("the process definition is returned") {
                result shouldBe processDefinition
            }
        }
    }

    Given("A zaak UUID for which no BPMN process instance exists") {
        val zaakUUID = UUID.randomUUID()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .singleResult()
        } returns null

        When("finding the process definition by zaak UUID") {
            val result = bpmnService.findProcessDefinitionByZaak(zaakUUID)

            Then("null is returned") {
                result shouldBe null
            }
        }
    }

    Context("Getting process definition metadata") {
        Given(
            "A process definition with full metadata including documentation, modification date, form keys and upload date"
        ) {
            val deploymentId = "fakeDeploymentId"
            val processDefinition = createProcessDefinition(deploymentId = deploymentId)
            val modificationDateStr = "2026-01-15T10:00:00+01:00"
            val modificationDate = ZonedDateTime.parse(modificationDateStr)
            val deploymentTime = Date()

            val extensionElement = mockk<ExtensionElement>()
            every { extensionElement.elementText } returns modificationDateStr

            val userTask1 = mockk<UserTask>()
            every { userTask1.formKey } returns "form1"
            val userTask2 = mockk<UserTask>()
            every { userTask2.formKey } returns "form2"

            val process = mockk<Process>()
            every { process.documentation } returns "Test documentation"
            every { process.extensionElements } returns mapOf("modificationdate" to listOf(extensionElement))
            every { process.flowElements } returns listOf(userTask1, userTask2)

            val bpmnModel = mockk<BpmnModel>()
            every { bpmnModel.processes } returns listOf(process)
            every { repositoryService.getBpmnModel(processDefinition.id) } returns bpmnModel

            val deployment = mockk<Deployment>()
            every { deployment.deploymentTime } returns deploymentTime
            val deploymentQuery = mockk<DeploymentQuery>()
            every { repositoryService.createDeploymentQuery() } returns deploymentQuery
            every { deploymentQuery.deploymentId(deploymentId) } returns deploymentQuery
            every { deploymentQuery.singleResult() } returns deployment

            When("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                Then("all metadata fields are populated correctly") {
                    result.documentation shouldBe "Test documentation"
                    result.modificationDate shouldBe modificationDate
                    result.uploadDate shouldNotBe null
                    result.formKeys shouldBe listOf("form1", "form2")
                }
            }
        }

        Given("A process definition with an empty process list") {
            val deploymentId = "fakeDeploymentId"
            val processDefinition = createProcessDefinition(deploymentId = deploymentId)
            val deploymentTime = Date()

            val bpmnModel = mockk<BpmnModel>()
            every { bpmnModel.processes } returns emptyList()
            every { repositoryService.getBpmnModel(processDefinition.id) } returns bpmnModel

            val deployment = mockk<Deployment>()
            every { deployment.deploymentTime } returns deploymentTime
            val deploymentQuery = mockk<DeploymentQuery>()
            every { repositoryService.createDeploymentQuery() } returns deploymentQuery
            every { deploymentQuery.deploymentId(deploymentId) } returns deploymentQuery
            every { deploymentQuery.singleResult() } returns deployment

            When("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                Then("documentation and modification date are null and form keys is empty") {
                    result.documentation shouldBe null
                    result.modificationDate shouldBe null
                    result.uploadDate shouldNotBe null
                    result.formKeys.shouldBeEmpty()
                }
            }
        }

        Given("A process definition whose deployment cannot be found") {
            val deploymentId = "fakeDeploymentId"
            val processDefinition = createProcessDefinition(deploymentId = deploymentId)

            val process = mockk<Process>()
            every { process.documentation } returns "Some documentation"
            every { process.extensionElements } returns emptyMap()
            every { process.flowElements } returns emptyList()

            val bpmnModel = mockk<BpmnModel>()
            every { bpmnModel.processes } returns listOf(process)
            every { repositoryService.getBpmnModel(processDefinition.id) } returns bpmnModel

            val deploymentQuery = mockk<DeploymentQuery>()
            every { repositoryService.createDeploymentQuery() } returns deploymentQuery
            every { deploymentQuery.deploymentId(deploymentId) } returns deploymentQuery
            every { deploymentQuery.singleResult() } returns null

            When("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                Then("upload date is null") {
                    result.uploadDate shouldBe null
                }
            }
        }

        Given("A process definition with multiple processes") {
            val deploymentId = "fakeDeploymentId"
            val processDefinition = createProcessDefinition(deploymentId = deploymentId)
            val modificationDateStr = "2026-03-01T09:00:00+01:00"
            val modificationDate = ZonedDateTime.parse(modificationDateStr)

            val extensionElement = mockk<ExtensionElement>()
            every { extensionElement.elementText } returns modificationDateStr

            val userTask1 = mockk<UserTask>()
            every { userTask1.formKey } returns "form-from-process-1"

            val userTask2 = mockk<UserTask>()
            every { userTask2.formKey } returns "form-from-process-2"

            val firstProcess = mockk<Process>()
            every { firstProcess.documentation } returns "First process documentation"
            every { firstProcess.extensionElements } returns mapOf("modificationdate" to listOf(extensionElement))
            every { firstProcess.flowElements } returns listOf(userTask1)

            val secondProcess = mockk<Process>()
            every { secondProcess.flowElements } returns listOf(userTask2)

            val bpmnModel = mockk<BpmnModel>()
            every { bpmnModel.processes } returns listOf(firstProcess, secondProcess)
            every { repositoryService.getBpmnModel(processDefinition.id) } returns bpmnModel

            val deploymentQuery = mockk<DeploymentQuery>()
            every { repositoryService.createDeploymentQuery() } returns deploymentQuery
            every { deploymentQuery.deploymentId(deploymentId) } returns deploymentQuery
            every { deploymentQuery.singleResult() } returns null

            When("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                Then(
                    "documentation and modification date come only from the first process and form keys are collected from all processes"
                ) {
                    result.documentation shouldBe "First process documentation"
                    result.modificationDate shouldBe modificationDate
                    result.formKeys shouldBe listOf("form-from-process-1", "form-from-process-2")
                }
            }
        }

        Given("A process definition with a process that has no modificationDate extension element") {
            val deploymentId = "fakeDeploymentId"
            val processDefinition = createProcessDefinition(deploymentId = deploymentId)

            val process = mockk<Process>()
            every { process.documentation } returns "Some documentation"
            every { process.extensionElements } returns emptyMap()
            every { process.flowElements } returns emptyList()

            val bpmnModel = mockk<BpmnModel>()
            every { bpmnModel.processes } returns listOf(process)
            every { repositoryService.getBpmnModel(processDefinition.id) } returns bpmnModel

            val deploymentQuery = mockk<DeploymentQuery>()
            every { repositoryService.createDeploymentQuery() } returns deploymentQuery
            every { deploymentQuery.deploymentId(deploymentId) } returns deploymentQuery
            every { deploymentQuery.singleResult() } returns null

            When("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                Then("modification date is null") {
                    result.modificationDate shouldBe null
                }
            }
        }

        Given("A process definition with user tasks where some have no form key") {
            val deploymentId = "fakeDeploymentId"
            val processDefinition = createProcessDefinition(deploymentId = deploymentId)

            val userTaskWithFormKey = mockk<UserTask>()
            every { userTaskWithFormKey.formKey } returns "someForm"

            val userTaskWithoutFormKey = mockk<UserTask>()
            every { userTaskWithoutFormKey.formKey } returns null

            val process = mockk<Process>()
            every { process.documentation } returns null
            every { process.extensionElements } returns emptyMap()
            every { process.flowElements } returns listOf(userTaskWithFormKey, userTaskWithoutFormKey)

            val bpmnModel = mockk<BpmnModel>()
            every { bpmnModel.processes } returns listOf(process)
            every { repositoryService.getBpmnModel(processDefinition.id) } returns bpmnModel

            val deploymentQuery = mockk<DeploymentQuery>()
            every { repositoryService.createDeploymentQuery() } returns deploymentQuery
            every { deploymentQuery.deploymentId(deploymentId) } returns deploymentQuery
            every { deploymentQuery.singleResult() } returns null

            When("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                Then("only user tasks with form keys are included in the form keys list") {
                    result.formKeys shouldHaveSize 1
                    result.formKeys[0] shouldBe "someForm"
                }
            }
        }
    }
})
