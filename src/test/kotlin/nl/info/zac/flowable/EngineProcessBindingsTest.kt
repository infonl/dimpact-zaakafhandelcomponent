/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_COMMUNICATIEKANAAL
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_GROUP
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_USER
import net.atos.zac.flowable.cmmn.CmmnService
import net.atos.zac.flowable.exception.CaseOrProcessNotFoundException
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck

class EngineProcessBindingsTest : BehaviorSpec({
    val cmmnService = mockk<CmmnService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val healthCheckService = mockk<HealthCheckService>()
    val bpmnService = mockk<BpmnService>()
    val cmmnProcessBinding = CmmnProcessBinding(cmmnService, healthCheckService)
    val bpmnProcessBinding = BpmnProcessBinding(bpmnService, zaakVariabelenService)

    afterEach { checkUnnecessaryStub() }

    context("starting the process of a zaak") {
        given("start data with zaakdata and an assignment") {
            val zaak = createZaak()
            val zaaktype = createZaakType()
            val processStartData = ProcessStartData(
                zaakData = mapOf("fakeKey" to "fakeValue"),
                groupId = "fakeGroupId",
                behandelaarId = "fakeBehandelaarId",
                communicatiekanaal = "fakeCommunicatiekanaal"
            )
            every {
                cmmnService.startCase(
                    zaak = zaak,
                    zaaktype = zaaktype,
                    caseDefinitionKey = "fakeCaseDefinition",
                    zaakData = mapOf("fakeKey" to "fakeValue")
                )
            } just runs
            every {
                bpmnService.startProcess(
                    zaak = zaak,
                    zaaktype = zaaktype,
                    processDefinitionKey = "fakeProcessDefinitionKey",
                    zaakData = mapOf(
                        "fakeKey" to "fakeValue",
                        VAR_ZAAK_GROUP to "fakeGroupId",
                        VAR_ZAAK_USER to "fakeBehandelaarId",
                        VAR_ZAAK_COMMUNICATIEKANAAL to "fakeCommunicatiekanaal"
                    )
                )
            } just runs

            `when`("the process is started in each engine") {
                cmmnProcessBinding.start(
                    zaak = zaak,
                    zaaktype = zaaktype,
                    definitionKey = "fakeCaseDefinition",
                    processStartData = processStartData
                )
                bpmnProcessBinding.start(
                    zaak = zaak,
                    zaaktype = zaaktype,
                    definitionKey = "fakeProcessDefinitionKey",
                    processStartData = processStartData
                )

                then("the CMMN case gets the zaakdata only, and the BPMN process also gets the assignment") {
                    verify(exactly = 1) {
                        cmmnService.startCase(
                            zaak = zaak,
                            zaaktype = zaaktype,
                            caseDefinitionKey = "fakeCaseDefinition",
                            zaakData = mapOf("fakeKey" to "fakeValue")
                        )
                        bpmnService.startProcess(
                            zaak = zaak,
                            zaaktype = zaaktype,
                            processDefinitionKey = "fakeProcessDefinitionKey",
                            zaakData = any()
                        )
                    }
                }
            }
        }

        given("start data without zaakdata") {
            val zaak = createZaak()
            val zaaktype = createZaakType()
            every {
                cmmnService.startCase(
                    zaak = zaak,
                    zaaktype = zaaktype,
                    caseDefinitionKey = "fakeCaseDefinition",
                    zaakData = null
                )
            } just runs

            `when`("a CMMN case is started") {
                cmmnProcessBinding.start(
                    zaak = zaak,
                    zaaktype = zaaktype,
                    definitionKey = "fakeCaseDefinition",
                    processStartData = ProcessStartData()
                )

                then("the case starts without zaakdata") {
                    verify(exactly = 1) {
                        cmmnService.startCase(
                            zaak = zaak,
                            zaaktype = zaaktype,
                            caseDefinitionKey = "fakeCaseDefinition",
                            zaakData = null
                        )
                    }
                }
            }
        }
    }

    context("checking whether a zaaktype is ready for zaak creation") {
        given("a zaaktype whose CMMN zaaktype check fails") {
            val zaaktype = createZaakType()
            every {
                healthCheckService.controleerZaaktype(zaaktype.url)
            } returns createZaaktypeInrichtingscheck().apply { isZaakafhandelParametersValide = false }

            `when`("each engine checks the zaaktype") {
                val isReadyForCmmn = cmmnProcessBinding.isZaaktypeReady(zaaktype.url)
                val isReadyForBpmn = bpmnProcessBinding.isZaaktypeReady(zaaktype.url)

                then("CMMN reports the zaaktype as not ready, and BPMN, which has no zaaktype check, as ready") {
                    isReadyForCmmn shouldBe false
                    isReadyForBpmn shouldBe true
                }
            }
        }
    }

    context("terminating and deleting the process of a zaak") {
        given("a zaak") {
            val zaak = createZaak()
            every { cmmnService.terminateCase(zaak.uuid) } returns Unit
            every { cmmnService.deleteCase(zaak.uuid) } just runs
            every { bpmnService.deleteProcessInstance(zaak.uuid) } just runs
            every { bpmnService.deleteProcessInstanceAndHistory(zaak.uuid) } just runs

            `when`("its process is terminated and deleted in each engine") {
                cmmnProcessBinding.terminate(zaak.uuid)
                bpmnProcessBinding.terminate(zaak.uuid)
                cmmnProcessBinding.delete(zaak.uuid)
                bpmnProcessBinding.delete(zaak.uuid)

                then("each engine terminates and deletes its own process") {
                    verify(exactly = 1) {
                        cmmnService.terminateCase(zaak.uuid)
                        bpmnService.deleteProcessInstance(zaak.uuid)
                        cmmnService.deleteCase(zaak.uuid)
                        bpmnService.deleteProcessInstanceAndHistory(zaak.uuid)
                    }
                }
            }
        }
    }

    context("checking whether a zaak has an active process") {
        given("a zaak with a running CMMN case and no running BPMN process") {
            val zaak = createZaak()
            every { cmmnService.isZaakCaseDriven(zaak.uuid) } returns true
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false

            `when`("each engine is asked for an active process") {
                val hasActiveCmmnProcess = cmmnProcessBinding.hasActiveProcess(zaak.uuid)
                val hasActiveBpmnProcess = bpmnProcessBinding.hasActiveProcess(zaak.uuid)

                then("only CMMN reports an active process") {
                    hasActiveCmmnProcess shouldBe true
                    hasActiveBpmnProcess shouldBe false
                }
            }
        }
    }

    context("updating the zaak data in the process of a zaak") {
        given("a zaak with a running BPMN process") {
            val zaak = createZaak()
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, "fakeGroupId") } just runs
            every { zaakVariabelenService.setUser(zaak.uuid, "fakeBehandelaarId") } just runs
            every { zaakVariabelenService.setCommunicationChannel(zaak.uuid, "fakeCommunicatiekanaal") } just runs

            `when`("the assignment and the communicatiekanaal are updated in each engine") {
                cmmnProcessBinding.updateAssignment(zaak.uuid, "fakeGroupId", "fakeBehandelaarId")
                bpmnProcessBinding.updateAssignment(zaak.uuid, "fakeGroupId", "fakeBehandelaarId")
                cmmnProcessBinding.updateCommunicatiekanaal(zaak.uuid, "fakeCommunicatiekanaal")
                bpmnProcessBinding.updateCommunicatiekanaal(zaak.uuid, "fakeCommunicatiekanaal")

                then("only the BPMN process gets them as zaak variables, because a CMMN case reads the rollen") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, "fakeGroupId")
                        zaakVariabelenService.setUser(zaak.uuid, "fakeBehandelaarId")
                        zaakVariabelenService.setCommunicationChannel(zaak.uuid, "fakeCommunicatiekanaal")
                    }
                }
            }
        }

        given("a zaak with a running BPMN process and a behandelaar") {
            val zaak = createZaak()
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.removeUser(zaak.uuid) } just runs

            `when`("the assignment is updated without a groep and without a behandelaar") {
                bpmnProcessBinding.updateAssignment(zaak.uuid, null, null)

                then("the behandelaar is removed from the process and the groep is left untouched") {
                    verify(exactly = 1) { zaakVariabelenService.removeUser(zaak.uuid) }
                    verify(exactly = 0) { zaakVariabelenService.setGroup(any(), any()) }
                }
            }
        }

        given("a zaak without a running BPMN process, for example because the zaak was closed and reopened") {
            val zaak = createZaak()
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false

            `when`("the assignment and the communicatiekanaal are updated") {
                bpmnProcessBinding.updateAssignment(zaak.uuid, "fakeGroupId", "fakeBehandelaarId")
                bpmnProcessBinding.updateCommunicatiekanaal(zaak.uuid, "fakeCommunicatiekanaal")

                then("no zaak variable is set, because there is no process to set it in") {
                    verify(exactly = 0) {
                        zaakVariabelenService.setGroup(any(), any())
                        zaakVariabelenService.setUser(any(), any())
                        zaakVariabelenService.setCommunicationChannel(any(), any())
                    }
                }
            }
        }

        given("a zaak whose BPMN process ends while its assignment is updated") {
            val zaak = createZaak()
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every {
                zaakVariabelenService.setGroup(zaak.uuid, "fakeGroupId")
            } throws CaseOrProcessNotFoundException("fakeCaseOrProcessNotFoundMessage")

            `when`("the assignment is updated") {
                bpmnProcessBinding.updateAssignment(zaak.uuid, "fakeGroupId", "fakeBehandelaarId")

                then("the update is skipped without an error") {
                    verify(exactly = 0) { zaakVariabelenService.setUser(any(), any()) }
                }
            }
        }
    }
})
