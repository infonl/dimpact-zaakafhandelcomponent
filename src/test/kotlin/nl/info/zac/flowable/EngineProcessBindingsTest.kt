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
    val cmmnProcessBinding = CmmnProcessBinding(cmmnService, zaakVariabelenService, healthCheckService)
    val bpmnProcessBinding = BpmnProcessBinding(bpmnService)

    afterEach { checkUnnecessaryStub() }

    context("starting the process of a zaak") {
        given("start data with case data and an assignment") {
            val zaak = createZaak()
            val zaaktype = createZaakType()
            val processStartData = ProcessStartData(
                caseData = mapOf("fakeKey" to "fakeValue"),
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

                then("the CMMN case gets the case data only, and the BPMN process also gets the assignment") {
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

        given("start data without case data") {
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

                then("the case starts without case data") {
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
            every { cmmnService.deleteCase(zaak.uuid) } returns Unit
            every { zaakVariabelenService.deleteAllCaseVariables(zaak.uuid) } just runs
            every { bpmnService.terminateCase(zaak.uuid) } returns Unit
            every { bpmnService.deleteProcessInstance(zaak.uuid) } just runs

            `when`("its process is terminated and deleted in each engine") {
                cmmnProcessBinding.terminate(zaak.uuid)
                bpmnProcessBinding.terminate(zaak.uuid)
                cmmnProcessBinding.delete(zaak.uuid)
                bpmnProcessBinding.delete(zaak.uuid)

                then("each engine terminates and deletes its own process, and CMMN also deletes the case variables") {
                    verify(exactly = 1) {
                        cmmnService.terminateCase(zaak.uuid)
                        bpmnService.terminateCase(zaak.uuid)
                        bpmnService.deleteProcessInstance(zaak.uuid)
                    }
                    verifyOrder {
                        cmmnService.deleteCase(zaak.uuid)
                        zaakVariabelenService.deleteAllCaseVariables(zaak.uuid)
                    }
                }
            }
        }
    }
})
