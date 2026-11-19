/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.ZaakListParameters
import nl.info.client.zgw.zrc.model.ZaakUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BPMN_PROCESS_DEFINITION_NOT_FOUND
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import org.flowable.engine.repository.ProcessDefinition
import java.net.URI
import java.util.UUID

class ZaaktypeConfigurationBeheerServiceProcessBindingTest : BehaviorSpec({
    val zaaktypeConfigurationRepository = mockk<ZaaktypeConfigurationRepository>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val zaaktypeConfigurationVersioning = mockk<ZaaktypeConfigurationVersioning>()
    val bpmnService = mockk<BpmnService>()
    val zaaktypeConfigurationBeheerService = ZaaktypeConfigurationBeheerService(
        zaaktypeConfigurationRepository = zaaktypeConfigurationRepository,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        zaaktypeConfigurationVersioning = zaaktypeConfigurationVersioning,
        bpmnService = bpmnService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    fun stubZakenOfZaaktypeVersion(zaaktypeUuid: UUID, zakenCount: Int) {
        val zaaktype = createZaakType(uri = URI("https://example.com/zaaktypen/$zaaktypeUuid"))
        every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
        every {
            zrcClientService.listZakenUuids(match<ZaakListParameters> { it.zaaktype == zaaktype.url })
        } returns Results(List(zakenCount) { ZaakUuid(UUID.randomUUID()) }, zakenCount)
    }

    context("whether the process definition of a configuration may change") {
        given("a configuration that is not bound to a process definition") {
            val zaaktypeConfiguration = ZaaktypeConfiguration().apply { zaaktypeUuid = UUID.randomUUID() }

            `when`("it is checked") {
                val isChangeable = zaaktypeConfigurationBeheerService.isProcessDefinitionChangeable(zaaktypeConfiguration)

                then("it may change, so that a beheerder can bind it for the first time") {
                    isChangeable shouldBe true
                }
            }
        }

        given("a BPMN configuration of a zaaktype version without zaken") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration()
            stubZakenOfZaaktypeVersion(zaaktypeConfiguration.zaaktypeUuid, zakenCount = 0)

            `when`("it is checked") {
                val isChangeable = zaaktypeConfigurationBeheerService.isProcessDefinitionChangeable(zaaktypeConfiguration)

                then("it may change, so that the first zaken of the zaaktype version start the chosen process") {
                    isChangeable shouldBe true
                }
            }
        }

        given("a BPMN configuration of a zaaktype version with a zaak") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration()
            stubZakenOfZaaktypeVersion(zaaktypeConfiguration.zaaktypeUuid, zakenCount = 1)

            `when`("it is checked") {
                val isChangeable = zaaktypeConfigurationBeheerService.isProcessDefinitionChangeable(zaaktypeConfiguration)

                then("it may not change, so that all zaken of a zaaktype version follow the same process") {
                    isChangeable shouldBe false
                }
            }
        }

        given("a CMMN configuration of a zaaktype version without zaken") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration()
            stubZakenOfZaaktypeVersion(zaaktypeConfiguration.zaaktypeUuid, zakenCount = 0)

            `when`("it is checked") {
                val isChangeable = zaaktypeConfigurationBeheerService.isProcessDefinitionChangeable(zaaktypeConfiguration)

                then("it may change, as no zaak reads the plan item settings of its case definition yet") {
                    isChangeable shouldBe true
                }
            }
        }

        given("a CMMN configuration of a zaaktype version with a zaak") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration()
            stubZakenOfZaaktypeVersion(zaaktypeConfiguration.zaaktypeUuid, zakenCount = 1)

            `when`("it is checked") {
                val isChangeable = zaaktypeConfigurationBeheerService.isProcessDefinitionChangeable(zaaktypeConfiguration)

                then("it may not change, so that all zaken of a zaaktype version follow the same case definition") {
                    isChangeable shouldBe false
                }
            }
        }
    }

    context("binding a configuration to a process definition") {
        fun stubActiveBpmnProcessDefinition(definitionKey: String) {
            every { bpmnService.findProcessDefinitionByProcessDefinitionKey(definitionKey) } returns mockk<ProcessDefinition>()
        }

        fun stubBoundConfiguration(zaaktypeConfiguration: ZaaktypeConfiguration, zakenCount: Int) {
            every {
                zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeConfiguration.zaaktypeUuid)
            } returns zaaktypeConfiguration
            stubZakenOfZaaktypeVersion(zaaktypeConfiguration.zaaktypeUuid, zakenCount)
        }

        given("a BPMN configuration of a zaaktype version with a zaak") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration(bpmnProcessDefinitionKey = "fakeProcessA")
            val zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid
            stubBoundConfiguration(zaaktypeConfiguration, zakenCount = 1)

            `when`("it is bound to the process definition it already has") {
                val isProcessDefinitionChangeable = shouldNotThrowAny {
                    zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                        zaaktypeUuid = zaaktypeUuid,
                        processEngine = ProcessEngine.BPMN,
                        definitionKey = "fakeProcessA"
                    )
                }

                then("the binding is accepted, and the process definition may not change any more") {
                    isProcessDefinitionChangeable shouldBe false
                }

                and("it asks Open Zaak once whether the zaaktype version has zaken") {
                    verify(exactly = 1) {
                        zrcClientService.listZakenUuids(
                            match<ZaakListParameters> { it.zaaktype == URI("https://example.com/zaaktypen/$zaaktypeUuid") }
                        )
                    }
                }
            }

            `when`("it is bound to another BPMN process definition") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                        zaaktypeUuid = zaaktypeUuid,
                        processEngine = ProcessEngine.BPMN,
                        definitionKey = "fakeProcessB"
                    )
                }

                then("the binding is rejected with an error code that the beheerder sees explained") {
                    inputValidationFailedException.errorCode shouldBe ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
                    inputValidationFailedException.message shouldBe "The process definition of the configuration of zaaktype " +
                        "'$zaaktypeUuid' cannot change from BPMN 'fakeProcessA' to BPMN 'fakeProcessB'"
                }
            }

            `when`("it is bound to a CMMN case definition with the same key as its BPMN process definition") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                        zaaktypeUuid = zaaktypeUuid,
                        processEngine = ProcessEngine.CMMN,
                        definitionKey = "fakeProcessA"
                    )
                }

                then("the binding is rejected, because the engine is part of the binding") {
                    inputValidationFailedException.errorCode shouldBe ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
                }
            }
        }

        given("a BPMN configuration of a zaaktype version without zaken") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration(bpmnProcessDefinitionKey = "fakeProcessA")
            val zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid
            stubBoundConfiguration(zaaktypeConfiguration, zakenCount = 0)

            `when`("it is bound to another BPMN process definition") {
                stubActiveBpmnProcessDefinition("fakeProcessB")

                val isProcessDefinitionChangeable = shouldNotThrowAny {
                    zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                        zaaktypeUuid = zaaktypeUuid,
                        processEngine = ProcessEngine.BPMN,
                        definitionKey = "fakeProcessB"
                    )
                }

                then("the binding is accepted, and the process definition may still change") {
                    isProcessDefinitionChangeable shouldBe true
                }
            }

            `when`("it is bound to a BPMN process definition without an active version") {
                every { bpmnService.findProcessDefinitionByProcessDefinitionKey("fakeDeletedProcess") } returns null

                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                        zaaktypeUuid = zaaktypeUuid,
                        processEngine = ProcessEngine.BPMN,
                        definitionKey = "fakeDeletedProcess"
                    )
                }

                then("the binding is rejected, so that no zaak is created that fails to start and locks the binding") {
                    inputValidationFailedException.errorCode shouldBe ERROR_CODE_BPMN_PROCESS_DEFINITION_NOT_FOUND
                    inputValidationFailedException.message shouldBe "The configuration of zaaktype '$zaaktypeUuid' " +
                        "cannot be bound to BPMN process definition 'fakeDeletedProcess', because it has no active version"
                }
            }
        }

        given("a CMMN configuration of a zaaktype version without zaken") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration()
            val zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid
            stubBoundConfiguration(zaaktypeConfiguration, zakenCount = 0)

            `when`("it is bound to another CMMN case definition") {
                val isProcessDefinitionChangeable = shouldNotThrowAny {
                zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                    zaaktypeUuid = zaaktypeUuid,
                    processEngine = ProcessEngine.CMMN,
                    definitionKey = "fakeOtherCaseDefinition"
                )
                }

                then("the binding is accepted, and the process definition may still change") {
                    isProcessDefinitionChangeable shouldBe true
                }
            }

            `when`("it is bound to a BPMN process definition") {
                stubActiveBpmnProcessDefinition("fakeProcessB")

                val isProcessDefinitionChangeable = shouldNotThrowAny {
                zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                    zaaktypeUuid = zaaktypeUuid,
                    processEngine = ProcessEngine.BPMN,
                    definitionKey = "fakeProcessB"
                )
                }

                then("the binding is accepted, so that the zaaktype version switches engine, and the process definition may still change") {
                    isProcessDefinitionChangeable shouldBe true
                }
            }
        }

        given("a CMMN configuration of a zaaktype version with a zaak") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration()
            val zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid
            val caseDefinitionKey = zaaktypeConfiguration.processBinding!!.definitionKey
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns zaaktypeConfiguration
            stubZakenOfZaaktypeVersion(zaaktypeUuid, zakenCount = 1)

            `when`("it is bound to another CMMN case definition") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                        zaaktypeUuid = zaaktypeUuid,
                        processEngine = ProcessEngine.CMMN,
                        definitionKey = "fakeOtherCaseDefinition"
                    )
                }

                then("the binding is rejected with the error code that the beheerder sees explained") {
                    inputValidationFailedException.errorCode shouldBe ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
                    inputValidationFailedException.message shouldBe "The process definition of the configuration of zaaktype " +
                        "'$zaaktypeUuid' cannot change from CMMN '$caseDefinitionKey' to CMMN 'fakeOtherCaseDefinition'"
                }
            }

            `when`("it is bound to a BPMN process definition") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                        zaaktypeUuid = zaaktypeUuid,
                        processEngine = ProcessEngine.BPMN,
                        definitionKey = "fakeProcessB"
                    )
                }

                then("the binding is rejected with the same error code") {
                    inputValidationFailedException.errorCode shouldBe ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
                }
            }
        }

        given("a BPMN configuration of a zaaktype version without zaken, bound to a CMMN case definition") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration(bpmnProcessDefinitionKey = "fakeProcessA")
            val zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid
            stubBoundConfiguration(zaaktypeConfiguration, zakenCount = 0)

            `when`("the binding is checked") {
                val isProcessDefinitionChangeable = shouldNotThrowAny {
                zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                    zaaktypeUuid = zaaktypeUuid,
                    processEngine = ProcessEngine.CMMN,
                    definitionKey = "fakeCaseDefinition"
                )
                }

                then("it is accepted, so that the zaaktype version switches engine, and the process definition may still change") {
                    isProcessDefinitionChangeable shouldBe true
                }
            }
        }

        given("a BPMN configuration of a zaaktype version with a zaak, bound to a CMMN case definition") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration(bpmnProcessDefinitionKey = "fakeProcessA")
            val zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid
            stubBoundConfiguration(zaaktypeConfiguration, zakenCount = 1)

            `when`("the binding is checked") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                        zaaktypeUuid = zaaktypeUuid,
                        processEngine = ProcessEngine.CMMN,
                        definitionKey = "fakeCaseDefinition"
                    )
                }

                then("it is rejected with the error code that the beheerder sees explained") {
                    inputValidationFailedException.errorCode shouldBe ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
                }
            }
        }

        given("a zaaktype version without a configuration") {
            val zaaktypeUuid = UUID.randomUUID()
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns null
            stubZakenOfZaaktypeVersion(zaaktypeUuid, zakenCount = 0)

            `when`("it is bound to an active BPMN process definition") {
                stubActiveBpmnProcessDefinition("fakeProcessB")

                val isProcessDefinitionChangeable = shouldNotThrowAny {
                zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                    zaaktypeUuid = zaaktypeUuid,
                    processEngine = ProcessEngine.BPMN,
                    definitionKey = "fakeProcessB"
                )
                }

                then("the binding is accepted, and the process definition may still change") {
                    isProcessDefinitionChangeable shouldBe true
                }
            }

            `when`("it is bound to a BPMN process definition without an active version") {
                every { bpmnService.findProcessDefinitionByProcessDefinitionKey("fakeDeletedProcess") } returns null

                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                        zaaktypeUuid = zaaktypeUuid,
                        processEngine = ProcessEngine.BPMN,
                        definitionKey = "fakeDeletedProcess"
                    )
                }

                then("the binding is rejected") {
                    inputValidationFailedException.errorCode shouldBe ERROR_CODE_BPMN_PROCESS_DEFINITION_NOT_FOUND
                }
            }
        }

        given("a zaaktype version for which Open Zaak cannot list the zaken") {
            val zaaktypeUuid = UUID.randomUUID()
            val zaaktype = createZaakType(uri = URI("https://example.com/zaaktypen/$zaaktypeUuid"))
            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
            every {
                zrcClientService.listZakenUuids(match<ZaakListParameters> { it.zaaktype == zaaktype.url })
            } throws IllegalStateException("fakeOpenZaakFailure")

            `when`("the binding is checked") {
                val illegalStateException = shouldThrow<IllegalStateException> {
                    zaaktypeConfigurationBeheerService.checkProcessBindingAndReadChangeability(
                        zaaktypeUuid = zaaktypeUuid,
                        processEngine = ProcessEngine.BPMN,
                        definitionKey = "fakeProcessA"
                    )
                }

                then("the failure is passed on before the configuration is read, so that the caller stores nothing") {
                    illegalStateException.message shouldBe "fakeOpenZaakFailure"
                }
            }
        }
    }

    context("whether a zaaktype version has zaken") {
        given("a zaaktype version without zaken in Open Zaak") {
            val zaaktypeUuid = UUID.randomUUID()
            stubZakenOfZaaktypeVersion(zaaktypeUuid, zakenCount = 0)

            `when`("it is checked") {
                val hasZaken = zaaktypeConfigurationBeheerService.hasZaken(zaaktypeUuid)

                then("it has none") {
                    hasZaken shouldBe false
                }
            }
        }

        given("a zaaktype version with a zaak in Open Zaak") {
            val zaaktypeUuid = UUID.randomUUID()
            stubZakenOfZaaktypeVersion(zaaktypeUuid, zakenCount = 1)

            `when`("it is checked") {
                val hasZaken = zaaktypeConfigurationBeheerService.hasZaken(zaaktypeUuid)

                then("it has zaken") {
                    hasZaken shouldBe true
                }

                and("it asks Open Zaak for a single zaak, because only the total count is needed") {
                    verify(exactly = 1) {
                        zrcClientService.listZakenUuids(match<ZaakListParameters> { it.pageSize == 1 })
                    }
                }
            }
        }
    }
})
