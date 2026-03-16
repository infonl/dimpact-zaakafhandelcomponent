/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.flowable.bpmn.BpmnProcessDefinitionTaskFormService
import nl.info.zac.flowable.bpmn.model.createBpmnProcessDefinitionTaskForm
import nl.info.zac.policy.PolicyService

class FormioFormulierenRestServiceTest : BehaviorSpec({
    val policyService = mockk<PolicyService>()
    val bpmnProcessDefinitionTaskFormService = mockk<BpmnProcessDefinitionTaskFormService>()
    val formioFormulierenRestService = FormioFormulierenRestService(
        bpmnProcessDefinitionTaskFormService,
        policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A list of formio forms") {
        val formioForms = listOf(
            createBpmnProcessDefinitionTaskForm(
                id = 1234L,
                name = "name1"
            ),
            createBpmnProcessDefinitionTaskForm(
                id = 5678L,
                name = "name2"
            ),
        )
        every { policyService.readOverigeRechten().beheren } returns true
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns formioForms

        When("the forms are listed") {
            val restFormioForms = formioFormulierenRestService.listFormulieren()

            Then("it should return the list of formio forms") {
                restFormioForms.size shouldBe 2
                with(restFormioForms[0]) {
                    id shouldBe 1234L
                    name shouldBe "name1"
                }
                with(restFormioForms[1]) {
                    id shouldBe 5678L
                    name shouldBe "name2"
                }
            }
        }
    }
})
