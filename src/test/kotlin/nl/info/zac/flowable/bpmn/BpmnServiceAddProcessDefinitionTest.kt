/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BPMN_PROCESS_DEFINITION_ID_MISSING
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BPMN_PROCESS_DEFINITION_MULTIPLE_PROCESSES
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BPMN_PROCESS_DEFINITION_NAME_MISSING
import nl.info.zac.flowable.bpmn.exception.BpmnProcessDefinitionIdMissingException
import nl.info.zac.flowable.bpmn.exception.BpmnProcessDefinitionMultipleProcessesException
import nl.info.zac.flowable.bpmn.exception.BpmnProcessDefinitionNameMissingException
import org.flowable.engine.HistoryService
import org.flowable.engine.ProcessEngine
import org.flowable.engine.RepositoryService
import org.flowable.engine.RuntimeService
import org.flowable.engine.repository.Deployment
import org.flowable.engine.repository.DeploymentBuilder

class BpmnServiceAddProcessDefinitionTest : BehaviorSpec({
    val repositoryService = mockk<RepositoryService>()
    val bpmnService = BpmnService(
        repositoryService,
        runtimeService = mockk<RuntimeService>(),
        historyService = mockk<HistoryService>(),
        processEngine = mockk<ProcessEngine>(),
        zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>(),
        bpmnProcessDefinitionTaskFormService = mockk<BpmnProcessDefinitionTaskFormService>()
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Adding a process definition") {
        fun bpmnContent(vararg processAttributes: String) =
            """<?xml version="1.0" encoding="UTF-8"?>""" +
                """<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="https://example.com/bpmn">""" +
                processAttributes.joinToString("") { """<process $it><startEvent id="start"/></process>""" } +
                "</definitions>"

        fun stubDeployment(processDefinitionContent: String): Deployment {
            val deploymentBuilder = mockk<DeploymentBuilder>()
            val deployment = mockk<Deployment>()
            every { repositoryService.createDeployment() } returns deploymentBuilder
            every { deploymentBuilder.addString("fakeFilename.bpmn", processDefinitionContent) } returns deploymentBuilder
            every { deploymentBuilder.name("fakeFilename.bpmn") } returns deploymentBuilder
            every { deploymentBuilder.enableDuplicateFiltering() } returns deploymentBuilder
            every { deploymentBuilder.deploy() } returns deployment
            return deployment
        }

        given("a BPMN file whose executable process has a name but no id") {
            val processDefinitionContent = bpmnContent("""name="fakeProcessDefinitionName" isExecutable="true"""")

            `when`("the process definition is added") {
                val bpmnProcessDefinitionIdMissingException = shouldThrow<BpmnProcessDefinitionIdMissingException> {
                    bpmnService.addProcessDefinition("fakeFilename.bpmn", processDefinitionContent)
                }

                then("an exception is thrown with the id missing error code and nothing is deployed") {
                    bpmnProcessDefinitionIdMissingException.errorCode shouldBe ERROR_CODE_BPMN_PROCESS_DEFINITION_ID_MISSING
                    bpmnProcessDefinitionIdMissingException.message shouldBe "BPMN process definition has no id"
                    verify(exactly = 0) { repositoryService.createDeployment() }
                }
            }
        }

        given("a BPMN file whose executable process has a name and an empty id") {
            val processDefinitionContent = bpmnContent("""id="" name="fakeProcessDefinitionName" isExecutable="true"""")

            `when`("the process definition is added") {
                val bpmnProcessDefinitionIdMissingException = shouldThrow<BpmnProcessDefinitionIdMissingException> {
                    bpmnService.addProcessDefinition("fakeFilename.bpmn", processDefinitionContent)
                }

                then("an exception is thrown with the id missing error code and nothing is deployed") {
                    bpmnProcessDefinitionIdMissingException.errorCode shouldBe ERROR_CODE_BPMN_PROCESS_DEFINITION_ID_MISSING
                    bpmnProcessDefinitionIdMissingException.message shouldBe "BPMN process definition has no id"
                    verify(exactly = 0) { repositoryService.createDeployment() }
                }
            }
        }

        given("a BPMN file whose executable process has neither an id nor a name") {
            val processDefinitionContent = bpmnContent("""isExecutable="true"""")

            `when`("the process definition is added") {
                val bpmnProcessDefinitionIdMissingException = shouldThrow<BpmnProcessDefinitionIdMissingException> {
                    bpmnService.addProcessDefinition("fakeFilename.bpmn", processDefinitionContent)
                }

                then("the missing id is reported and nothing is deployed") {
                    bpmnProcessDefinitionIdMissingException.errorCode shouldBe ERROR_CODE_BPMN_PROCESS_DEFINITION_ID_MISSING
                    verify(exactly = 0) { repositoryService.createDeployment() }
                }
            }
        }

        given("a BPMN file whose executable process has an id but no name") {
            val processDefinitionContent = bpmnContent("""id="fakeProcessDefinitionKey" isExecutable="true"""")

            `when`("the process definition is added") {
                val bpmnProcessDefinitionNameMissingException = shouldThrow<BpmnProcessDefinitionNameMissingException> {
                    bpmnService.addProcessDefinition("fakeFilename.bpmn", processDefinitionContent)
                }

                then("an exception is thrown with the name missing error code and nothing is deployed") {
                    bpmnProcessDefinitionNameMissingException.errorCode shouldBe ERROR_CODE_BPMN_PROCESS_DEFINITION_NAME_MISSING
                    bpmnProcessDefinitionNameMissingException.message shouldBe
                        "BPMN process definition with key 'fakeProcessDefinitionKey' has no name"
                    verify(exactly = 0) { repositoryService.createDeployment() }
                }
            }
        }

        given("a BPMN file whose executable process has an id and a blank name") {
            val processDefinitionContent = bpmnContent("""id="fakeProcessDefinitionKey" name=" " isExecutable="true"""")

            `when`("the process definition is added") {
                val bpmnProcessDefinitionNameMissingException = shouldThrow<BpmnProcessDefinitionNameMissingException> {
                    bpmnService.addProcessDefinition("fakeFilename.bpmn", processDefinitionContent)
                }

                then("an exception is thrown with the name missing error code and nothing is deployed") {
                    bpmnProcessDefinitionNameMissingException.errorCode shouldBe ERROR_CODE_BPMN_PROCESS_DEFINITION_NAME_MISSING
                    bpmnProcessDefinitionNameMissingException.message shouldBe
                        "BPMN process definition with key 'fakeProcessDefinitionKey' has no name"
                    verify(exactly = 0) { repositoryService.createDeployment() }
                }
            }
        }

        given("a BPMN file whose process has an id, no name and no isExecutable attribute") {
            val processDefinitionContent = bpmnContent("""id="fakeProcessDefinitionKey"""")

            `when`("the process definition is added") {
                val bpmnProcessDefinitionNameMissingException = shouldThrow<BpmnProcessDefinitionNameMissingException> {
                    bpmnService.addProcessDefinition("fakeFilename.bpmn", processDefinitionContent)
                }

                then("the process is treated as executable, the missing name is reported and nothing is deployed") {
                    bpmnProcessDefinitionNameMissingException.errorCode shouldBe ERROR_CODE_BPMN_PROCESS_DEFINITION_NAME_MISSING
                    verify(exactly = 0) { repositoryService.createDeployment() }
                }
            }
        }

        given("a BPMN file whose executable process has an id and a name") {
            val processDefinitionContent = bpmnContent(
                """id="fakeProcessDefinitionKey" name="fakeProcessDefinitionName" isExecutable="true""""
            )
            val deployment = stubDeployment(processDefinitionContent)

            `when`("the process definition is added") {
                val result = bpmnService.addProcessDefinition("fakeFilename.bpmn", processDefinitionContent)

                then("the process definition is deployed") {
                    result shouldBe deployment
                }
            }
        }

        given("a BPMN file whose non-executable process has an id but no name") {
            val processDefinitionContent = bpmnContent("""id="fakeProcessDefinitionKey" isExecutable="false"""")
            val deployment = stubDeployment(processDefinitionContent)

            `when`("the process definition is added") {
                val result = bpmnService.addProcessDefinition("fakeFilename.bpmn", processDefinitionContent)

                then("the process definition is deployed, because only an executable process needs a name") {
                    result shouldBe deployment
                }
            }
        }

        mapOf(
            "two executable processes that both have an id and a name" to listOf(
                """id="fakeProcessDefinitionKey1" name="fakeProcessDefinitionName1" isExecutable="true"""",
                """id="fakeProcessDefinitionKey2" name="fakeProcessDefinitionName2" isExecutable="true""""
            ),
            "a named executable process and a non-executable process" to listOf(
                """id="fakeProcessDefinitionKey1" name="fakeProcessDefinitionName1" isExecutable="true"""",
                """id="fakeProcessDefinitionKey2" name="fakeProcessDefinitionName2" isExecutable="false""""
            ),
            "a named executable process and a second process without an id" to listOf(
                """id="fakeProcessDefinitionKey1" name="fakeProcessDefinitionName1" isExecutable="true"""",
                """name="fakeProcessDefinitionName2" isExecutable="true""""
            ),
            "a named executable process and a second executable process without a name" to listOf(
                """id="fakeProcessDefinitionKey1" name="fakeProcessDefinitionName1" isExecutable="true"""",
                """id="fakeProcessDefinitionKey2" isExecutable="true""""
            ),
            "three named executable processes" to listOf(
                """id="fakeProcessDefinitionKey1" name="fakeProcessDefinitionName1" isExecutable="true"""",
                """id="fakeProcessDefinitionKey2" name="fakeProcessDefinitionName2" isExecutable="true"""",
                """id="fakeProcessDefinitionKey3" name="fakeProcessDefinitionName3" isExecutable="true""""
            )
        ).forEach { (description, processAttributes) ->
            given("a BPMN file with $description") {
                val processDefinitionContent = bpmnContent(*processAttributes.toTypedArray())

                `when`("the process definition is added") {
                    val bpmnProcessDefinitionMultipleProcessesException =
                        shouldThrow<BpmnProcessDefinitionMultipleProcessesException> {
                            bpmnService.addProcessDefinition("fakeFilename.bpmn", processDefinitionContent)
                        }

                    then("more than one process is reported and nothing is deployed") {
                        bpmnProcessDefinitionMultipleProcessesException.errorCode shouldBe
                            ERROR_CODE_BPMN_PROCESS_DEFINITION_MULTIPLE_PROCESSES
                        bpmnProcessDefinitionMultipleProcessesException.message shouldBe
                            "BPMN process definition contains more than one process"
                        verify(exactly = 0) { repositoryService.createDeployment() }
                    }
                }
            }
        }
    }
})
