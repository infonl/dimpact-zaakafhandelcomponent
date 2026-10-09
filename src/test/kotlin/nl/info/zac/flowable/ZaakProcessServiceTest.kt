/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.exception.ZaaktypeConfigurationNotFoundException
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createZaaktypeConfigurationsUnderTest
import java.net.URI
import java.util.UUID

class ZaakProcessServiceTest : BehaviorSpec({
    val cmmnProcessBinding = mockk<ProcessBinding>()
    val bpmnProcessBinding = mockk<ProcessBinding>()
    fun createZaakProcessService(): ZaakProcessService {
        every { cmmnProcessBinding.processEngine } returns ProcessEngine.CMMN
        every { bpmnProcessBinding.processEngine } returns ProcessEngine.BPMN
        val processBindingInstances = mockk<Instance<ProcessBinding>>()
        every { processBindingInstances.iterator() } answers {
            mutableListOf(cmmnProcessBinding, bpmnProcessBinding).iterator()
        }
        return ZaakProcessService(processBindingInstances)
    }
    val processBindingsByEngine = mapOf(ProcessEngine.CMMN to cmmnProcessBinding, ProcessEngine.BPMN to bpmnProcessBinding)

    afterEach {
        checkUnnecessaryStub()
        clearMocks(cmmnProcessBinding, bpmnProcessBinding)
    }

    createZaaktypeConfigurationsUnderTest().forEach { (configurationType, createZaaktypeConfiguration) ->
        given("a zaaktype configuration bound to $configurationType") {
            val zaakProcessService = createZaakProcessService()
            val zaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype")
            val definitionKey = checkNotNull(zaaktypeConfiguration.processBinding).definitionKey
            val zaak = createZaak()
            val zaaktype = createZaakType()
            val processStartData = ProcessStartData(groupId = "fakeGroupId")
            val zaaktypeUri = URI("https://example.com/zaaktypes/${UUID.randomUUID()}")
            val boundProcessBinding = processBindingsByEngine.getValue(configurationType)
            val otherProcessBinding = processBindingsByEngine.values.single { it != boundProcessBinding }
            every { boundProcessBinding.start(zaak, zaaktype, definitionKey, processStartData) } just runs
            every { boundProcessBinding.terminate(zaak.uuid) } just runs
            every { boundProcessBinding.isZaaktypeReady(zaaktypeUri) } returns true

            `when`("the process of a zaak is started, the zaaktype is checked, and the process is terminated") {
                zaakProcessService.start(zaaktypeConfiguration, zaak, zaaktype, processStartData)
                val isZaaktypeReady = zaakProcessService.isZaaktypeReady(zaaktypeConfiguration, zaaktypeUri)
                zaakProcessService.terminate(zaaktypeConfiguration, zaak.uuid)

                then("each operation runs in the $configurationType engine with the bound definition, and none in the other") {
                    isZaaktypeReady shouldBe true
                    verify(exactly = 1) {
                        boundProcessBinding.start(zaak, zaaktype, definitionKey, processStartData)
                        boundProcessBinding.terminate(zaak.uuid)
                    }
                    verify(exactly = 0) {
                        otherProcessBinding.start(
                            zaak = any(),
                            zaaktype = any(),
                            definitionKey = any(),
                            processStartData = any()
                        )
                        otherProcessBinding.terminate(any())
                    }
                }
            }
        }
    }

    given("a zaaktype configuration without a process binding") {
        val zaakProcessService = ZaakProcessService(mockk())
        val zaaktypeConfiguration = ZaaktypeConfiguration().apply { zaaktypeUuid = UUID.randomUUID() }
        val zaak = createZaak()

        `when`("the process of a zaak is started") {
            val exception = shouldThrow<ZaaktypeConfigurationNotFoundException> {
                zaakProcessService.start(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    zaak = zaak,
                    zaaktype = createZaakType(),
                    processStartData = ProcessStartData()
                )
            }

            then("the start is refused") {
                exception.message shouldContain "is not bound to a process"
            }
        }

        `when`("the process of a zaak is terminated and the zaaktype is checked") {
            zaakProcessService.terminate(zaaktypeConfiguration, zaak.uuid)
            val isZaaktypeReady = zaakProcessService.isZaaktypeReady(
                zaaktypeConfiguration,
                URI("https://example.com/zaaktypes/${UUID.randomUUID()}")
            )

            then("no engine is called, because such a zaaktype has no zaken with a process, and the zaaktype is not ready") {
                isZaaktypeReady shouldBe false
                verify(exactly = 0) {
                    cmmnProcessBinding.terminate(any())
                    bpmnProcessBinding.terminate(any())
                }
            }
        }
    }

    given("a deleted zaak") {
        val zaakProcessService = createZaakProcessService()
        val zaakUuid = UUID.randomUUID()
        every { cmmnProcessBinding.delete(zaakUuid) } just runs
        every { bpmnProcessBinding.delete(zaakUuid) } just runs

        `when`("its process is deleted in all engines") {
            zaakProcessService.deleteInAllEngines(zaakUuid)

            then("every engine deletes the process of the zaak") {
                verify(exactly = 1) {
                    cmmnProcessBinding.delete(zaakUuid)
                    bpmnProcessBinding.delete(zaakUuid)
                }
            }
        }
    }

    given("a zaak with an active process in the BPMN engine only") {
        val zaakProcessService = createZaakProcessService()
        val zaakUuid = UUID.randomUUID()
        every { cmmnProcessBinding.hasActiveProcess(zaakUuid) } returns false
        every { bpmnProcessBinding.hasActiveProcess(zaakUuid) } returns true

        `when`("asked whether the zaak has an active process") {
            val hasActiveProcess = zaakProcessService.hasActiveProcess(zaakUuid)

            then("the zaak has an active process") {
                hasActiveProcess shouldBe true
            }
        }
    }

    given("a zaak without an active process in any engine") {
        val zaakProcessService = createZaakProcessService()
        val zaakUuid = UUID.randomUUID()
        every { cmmnProcessBinding.hasActiveProcess(zaakUuid) } returns false
        every { bpmnProcessBinding.hasActiveProcess(zaakUuid) } returns false

        `when`("asked whether the zaak has an active process") {
            val hasActiveProcess = zaakProcessService.hasActiveProcess(zaakUuid)

            then("the zaak has no active process") {
                hasActiveProcess shouldBe false
            }
        }
    }

    given("a zaak whose assignment and communicatiekanaal change") {
        val zaakProcessService = createZaakProcessService()
        val zaakUuid = UUID.randomUUID()
        every { cmmnProcessBinding.updateAssignment(zaakUuid, "fakeGroupId", null) } just runs
        every { bpmnProcessBinding.updateAssignment(zaakUuid, "fakeGroupId", null) } just runs
        every { cmmnProcessBinding.updateCommunicatiekanaal(zaakUuid, "fakeCommunicatiekanaal") } just runs
        every { bpmnProcessBinding.updateCommunicatiekanaal(zaakUuid, "fakeCommunicatiekanaal") } just runs

        `when`("the process of the zaak is updated") {
            zaakProcessService.updateAssignment(zaakUuid, "fakeGroupId", null)
            zaakProcessService.updateCommunicatiekanaal(zaakUuid, "fakeCommunicatiekanaal")

            then("every engine is updated, without a lookup of the zaaktype configuration of the zaak") {
                verify(exactly = 1) {
                    cmmnProcessBinding.updateAssignment(zaakUuid, "fakeGroupId", null)
                    bpmnProcessBinding.updateAssignment(zaakUuid, "fakeGroupId", null)
                    cmmnProcessBinding.updateCommunicatiekanaal(zaakUuid, "fakeCommunicatiekanaal")
                    bpmnProcessBinding.updateCommunicatiekanaal(zaakUuid, "fakeCommunicatiekanaal")
                }
            }
        }
    }

    given("only a CMMN process binding") {
        every { cmmnProcessBinding.processEngine } returns ProcessEngine.CMMN
        val processBindingInstances = mockk<Instance<ProcessBinding>>()
        every { processBindingInstances.iterator() } answers { mutableListOf(cmmnProcessBinding).iterator() }
        val zaakProcessService = ZaakProcessService(processBindingInstances)

        `when`("a process operation is run") {
            val illegalStateException = shouldThrow<IllegalStateException> {
                zaakProcessService.hasActiveProcess(UUID.randomUUID())
            }

            then("it fails and names the engine without a binding") {
                illegalStateException.message shouldBe "Expected one process binding for engine 'BPMN', found 0"
            }
        }
    }

    given("two process bindings for the CMMN engine") {
        val otherCmmnProcessBinding = mockk<ProcessBinding>()
        every { cmmnProcessBinding.processEngine } returns ProcessEngine.CMMN
        every { otherCmmnProcessBinding.processEngine } returns ProcessEngine.CMMN
        every { bpmnProcessBinding.processEngine } returns ProcessEngine.BPMN
        val processBindingInstances = mockk<Instance<ProcessBinding>>()
        every { processBindingInstances.iterator() } answers {
            mutableListOf(cmmnProcessBinding, otherCmmnProcessBinding, bpmnProcessBinding).iterator()
        }
        val zaakProcessService = ZaakProcessService(processBindingInstances)

        `when`("a process operation is run") {
            val illegalStateException = shouldThrow<IllegalStateException> {
                zaakProcessService.hasActiveProcess(UUID.randomUUID())
            }

            then("it fails and names the engine with more than one binding") {
                illegalStateException.message shouldBe "Expected one process binding for engine 'CMMN', found 2"
            }
        }
    }
})
