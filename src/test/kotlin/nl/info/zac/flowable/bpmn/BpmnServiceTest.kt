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
import io.mockk.runs
import io.mockk.just
import org.flowable.engine.history.HistoricProcessInstance
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
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ProcessEngine.BPMN
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.flowable.bpmn.exception.BpmnProcessDefinitionNotFoundException
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
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val bpmnProcessDefinitionTaskFormService = mockk<BpmnProcessDefinitionTaskFormService>()
    val bpmnService = BpmnService(
        repositoryService,
        runtimeService,
        historyService,
        processEngine,
        zaaktypeConfigurationService,
        bpmnProcessDefinitionTaskFormService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("A UUID for which a BPMN process instance exists") {
        val uuid = UUID.randomUUID()
        val processInstance = mockk<ProcessInstance>()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(uuid.toString())
                .singleResult()
        } returns processInstance

        `when`("a check is done to see if the zaak is process driven") {
            val isProcessDriven = bpmnService.isZaakProcessDriven(uuid)

            then("'true is returned") {
                isProcessDriven shouldBe true
            }
        }
    }

    given("A UUID for which no BPMN process instance exists") {
        val uuid = UUID.randomUUID()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(uuid.toString())
                .singleResult()
        } returns null

        `when`("a check is done to see if the zaak is process driven") {
            val isProcessDriven = bpmnService.isZaakProcessDriven(uuid)

            then("'false is returned") {
                isProcessDriven shouldBe false
            }
        }
    }

    given("A zaak and zaakdata and a zaaktype with a 'referentieproces'") {
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

        `when`("the zaak is started using a BPMN process definition") {
            bpmnService.startProcess(zaak = zaak, zaaktype = zaakType, processDefinitionKey = referentieProcesName, zaakData = zaakData)

            then("a Flowable BPMN process instance should be started") {
                verify(exactly = 1) {
                    processInstanceBuilder.start()
                }
            }
        }
    }
    given("A valid process definition key with an existing process definition") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        val processDefinition = createProcessDefinition()
        every { bpmnService.findProcessDefinitionByProcessDefinitionKey(processDefinitionKey) } returns processDefinition

        `when`("reading the process definition by process definition key") {
            val result = bpmnService.readProcessDefinitionByProcessDefinitionKey(processDefinitionKey)

            then("the correct process definition is returned") {
                result shouldBe processDefinition
            }
        }
    }

    given("An invalid process definition key with no existing process definition") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every { bpmnService.findProcessDefinitionByProcessDefinitionKey(processDefinitionKey) } returns null

        `when`("reading the process definition by process definition key") {
            val exception = shouldThrow<BpmnProcessDefinitionNotFoundException> {
                bpmnService.readProcessDefinitionByProcessDefinitionKey(processDefinitionKey)
            }

            then("a 'process definition not found exception' is thrown") {
                exception.message shouldBe "No BPMN process definition found for process definition key: '$processDefinitionKey'"
            }
        }
    }

    given("A valid zaaktype UUID with an existing process definition") {
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

        `when`("the process instance of the zaak is deleted") {
            bpmnService.deleteProcessInstance(zaaktypeUUID)

            then("the process instance is terminated") {
                verify(exactly = 1) {
                    runtimeService.deleteProcessInstance(processInstanceId, null)
                }
            }
        }
    }

    given("valid zaaktype UUID without an existing process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaaktypeUUID.toString())
                .singleResult()
        } returns null

        `when`("the process instance of the zaak is deleted") {
            bpmnService.deleteProcessInstance(zaaktypeUUID)

            then("the process instance is not found") {
                verify(exactly = 0) {
                    runtimeService.deleteProcessInstance(any(), null)
                }
            }
        }
    }

    given("Process definitions") {
        val historyProcessInstance1 = createHistoricProcessInstance(processDefinitionKey = "fakeKey1")
        val historyProcessInstance2 = createHistoricProcessInstance(processDefinitionKey = "fakeKey2")
        val historyProcessInstance3 = createHistoricProcessInstance(processDefinitionKey = "fakeKey1")
        every {
            historyService.createHistoricProcessInstanceQuery().list()
        } returns listOf(historyProcessInstance1, historyProcessInstance2, historyProcessInstance3)

        `when`("Returning a list of unique BPMN process definition keys used in process instances") {
            val result = bpmnService.findUniqueBpmnProcessDefinitionKeysFromProcessInstances()

            then("the unique BPMN process definition keys are returned") {
                result shouldBe setOf("fakeKey1", "fakeKey2")
            }
        }
    }

    given("a deployed process definition") {
        val processDefinitionId = "fakeProcessDefinitionId"
        val processDefinitionModel = "<definitions id=\"fakeDefinitions\"/>"
        every { repositoryService.getProcessModel(processDefinitionId) } returns
            processDefinitionModel.byteInputStream()

        `when`("reading the process definition model") {
            val result = bpmnService.readProcessDefinitionModel(processDefinitionId)

            then("the BPMN XML as deployed is returned") {
                result.readBytes().decodeToString() shouldBe processDefinitionModel
            }
        }
    }

    given("process definition key with current or historic process instances") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every {
            historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count()
        } returns 2

        `when`("checking it has process instances by process definition key") {
            val hasProcessInstances = bpmnService.hasProcessInstances(processDefinitionKey)

            then("true is returned") {
                hasProcessInstances shouldBe true
            }
        }
    }

    given("process definition key without current or historic process instances") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every {
            historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count()
        } returns 0

        `when`("checking it has process instances by process definition key") {
            val hasProcessInstances = bpmnService.hasProcessInstances(processDefinitionKey)

            then("false is returned") {
                hasProcessInstances shouldBe false
            }
        }
    }

    given("process definition key with linked configurations") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        val linkedProcessDefinitionKeys = listOf(processDefinitionKey, "otherProcessDefinitionKey")
        every {
            zaaktypeConfigurationService.listDefinitionKeysBoundTo(BPMN)
        } returns linkedProcessDefinitionKeys

        `when`("checking it has linked configurations by process definition key") {
            val hasLinkedZaaktypeBpmnConfiguration = bpmnService.hasLinkedZaaktypeBpmnConfiguration(processDefinitionKey)

            then("true is returned") {
                hasLinkedZaaktypeBpmnConfiguration shouldBe true
            }
        }
    }

    given("process definition key without linked configurations") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        val linkedProcessDefinitionKeys = listOf("otherProcessDefinitionKey")
        every {
            zaaktypeConfigurationService.listDefinitionKeysBoundTo(BPMN)
        } returns linkedProcessDefinitionKeys

        `when`("checking it has linked configurations by process definition key") {
            val hasLinkedZaaktypeBpmnConfiguration = bpmnService.hasLinkedZaaktypeBpmnConfiguration(processDefinitionKey)

            then("false is returned") {
                hasLinkedZaaktypeBpmnConfiguration shouldBe false
            }
        }
    }

    given("process definition key with current or historic process instances and linked configurations not checked") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every {
            historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count()
        } returns 3

        `when`("checking the process definition is in use by process definition key") {
            val isProcessDefinitionInUse = bpmnService.isProcessDefinitionInUse(processDefinitionKey)

            then("true is returned") {
                isProcessDefinitionInUse shouldBe true
                verify(exactly = 0) {
                    zaaktypeConfigurationService.listDefinitionKeysBoundTo(BPMN)
                }
            }
        }
    }

    given("process definition key with no current or historic process instances and linked configurations") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every {
            historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count()
        } returns 0
        val linkedProcessDefinitionKeys = listOf(processDefinitionKey, "otherProcessDefinitionKey")
        every {
            zaaktypeConfigurationService.listDefinitionKeysBoundTo(BPMN)
        } returns linkedProcessDefinitionKeys

        `when`("checking the process definition is in use by process definition key") {
            val isProcessDefinitionInUse = bpmnService.isProcessDefinitionInUse(processDefinitionKey)

            then("true is returned") {
                isProcessDefinitionInUse shouldBe true
            }
        }
    }

    given("process definition key with no current or historic process instances and no linked configurations") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every {
            historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count()
        } returns 0
        val linkedProcessDefinitionKeys = listOf("otherProcessDefinitionKey")
        every {
            zaaktypeConfigurationService.listDefinitionKeysBoundTo(BPMN)
        } returns linkedProcessDefinitionKeys

        `when`("checking the process definition is in use by process definition key") {
            val isProcessDefinitionInUse = bpmnService.isProcessDefinitionInUse(processDefinitionKey)

            then("false is returned") {
                isProcessDefinitionInUse shouldBe false
            }
        }
    }

    given("A zaak UUID for which a BPMN process instance and process definition exist") {
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

        `when`("finding the process definition by zaak UUID") {
            val result = bpmnService.findProcessDefinitionByZaak(zaakUUID)

            then("the process definition is returned") {
                result shouldBe processDefinition
            }
        }
    }

    given("A zaak UUID for which no BPMN process instance exists") {
        val zaakUUID = UUID.randomUUID()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .singleResult()
        } returns null

        `when`("finding the process definition by zaak UUID") {
            val result = bpmnService.findProcessDefinitionByZaak(zaakUUID)

            then("null is returned") {
                result shouldBe null
            }
        }
    }

    context("Getting process definition metadata") {
        given(
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

            `when`("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                then("all metadata fields are populated correctly") {
                    result.documentation shouldBe "Test documentation"
                    result.modificationDate shouldBe modificationDate
                    result.uploadDate shouldNotBe null
                    result.formKeys shouldBe listOf("form1", "form2")
                }
            }
        }

        given("A process definition with an empty process list") {
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

            `when`("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                then("documentation and modification date are null and form keys is empty") {
                    result.documentation shouldBe null
                    result.modificationDate shouldBe null
                    result.uploadDate shouldNotBe null
                    result.formKeys.shouldBeEmpty()
                }
            }
        }

        given("A process definition whose deployment cannot be found") {
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

            `when`("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                then("upload date is null") {
                    result.uploadDate shouldBe null
                }
            }
        }

        given("A process definition with multiple processes") {
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

            `when`("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                then(
                    "documentation and modification date come only from the first process and form keys are collected from all processes"
                ) {
                    result.documentation shouldBe "First process documentation"
                    result.modificationDate shouldBe modificationDate
                    result.formKeys shouldBe listOf("form-from-process-1", "form-from-process-2")
                }
            }
        }

        given("A process definition with a process that has no modificationDate extension element") {
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

            `when`("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                then("modification date is null") {
                    result.modificationDate shouldBe null
                }
            }
        }

        given("A process definition with a modificationDate extension element that is not a date") {
            val deploymentId = "fakeDeploymentId"
            val processDefinition = createProcessDefinition(deploymentId = deploymentId)

            val extensionElement = mockk<ExtensionElement>()
            every { extensionElement.elementText } returns "not a date"

            val process = mockk<Process>()
            every { process.documentation } returns "Some documentation"
            every { process.extensionElements } returns mapOf("modificationdate" to listOf(extensionElement))
            every { process.flowElements } returns emptyList()

            val bpmnModel = mockk<BpmnModel>()
            every { bpmnModel.processes } returns listOf(process)
            every { repositoryService.getBpmnModel(processDefinition.id) } returns bpmnModel

            val deploymentQuery = mockk<DeploymentQuery>()
            every { repositoryService.createDeploymentQuery() } returns deploymentQuery
            every { deploymentQuery.deploymentId(deploymentId) } returns deploymentQuery
            every { deploymentQuery.singleResult() } returns null

            `when`("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                then("the modification date is null and the rest of the metadata is still returned") {
                    result.modificationDate shouldBe null
                    result.documentation shouldBe "Some documentation"
                }
            }
        }

        given("A process definition with user tasks where some have no form key") {
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

            `when`("getting the process definition metadata") {
                val result = bpmnService.getProcessDefinitionMetadata(processDefinition)

                then("only user tasks with form keys are included in the form keys list") {
                    result.formKeys shouldHaveSize 1
                    result.formKeys[0] shouldBe "someForm"
                }
            }
        }
    }

    context("deleting the process instance and the history of a zaak") {
        given("a zaak with a running process instance and the history of two process instances") {
            val zaakUuid = UUID.randomUUID()
            val processInstance = mockk<ProcessInstance>()
            val historicProcessInstance1 = mockk<HistoricProcessInstance>()
            val historicProcessInstance2 = mockk<HistoricProcessInstance>()
            every { processInstance.id } returns "fakeProcessInstanceId"
            every { historicProcessInstance1.id } returns "fakeHistoricProcessInstanceId1"
            every { historicProcessInstance2.id } returns "fakeHistoricProcessInstanceId2"
            every {
                runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(zaakUuid.toString()).singleResult()
            } returns processInstance
            every { runtimeService.deleteProcessInstance("fakeProcessInstanceId", "Zaak deleted") } just runs
            every {
                historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(zaakUuid.toString()).list()
            } returns listOf(historicProcessInstance1, historicProcessInstance2)
            every { historyService.deleteHistoricProcessInstance(any()) } just runs

            `when`("the process instance and the history of the zaak are deleted") {
                bpmnService.deleteProcessInstanceAndHistory(zaakUuid)

                then("the running process instance and the history of every process instance of the zaak are deleted") {
                    verify(exactly = 1) {
                        runtimeService.deleteProcessInstance("fakeProcessInstanceId", "Zaak deleted")
                        historyService.deleteHistoricProcessInstance("fakeHistoricProcessInstanceId1")
                        historyService.deleteHistoricProcessInstance("fakeHistoricProcessInstanceId2")
                    }
                }
            }
        }

        given("a zaak without a process instance or history") {
            val zaakUuid = UUID.randomUUID()
            every {
                runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(zaakUuid.toString()).singleResult()
            } returns null
            every {
                historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(zaakUuid.toString()).list()
            } returns emptyList()

            `when`("the process instance and the history of the zaak are deleted") {
                bpmnService.deleteProcessInstanceAndHistory(zaakUuid)

                then("nothing is deleted") {
                    verify(exactly = 0) {
                        runtimeService.deleteProcessInstance(any(), "Zaak deleted")
                    }
                }
            }
        }
    }
})
