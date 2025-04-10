/*
 * SPDX-FileCopyrightText: 2025 Lifely
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
import net.atos.zac.policy.PolicyService
import nl.info.zac.formio.FormioService
import nl.info.zac.formio.createFormioFormulier
import org.apache.http.HttpStatus

class FormioFormulierenRestServiceTest : BehaviorSpec({
    val formioService = mockk<FormioService>()
    val policyService = mockk<PolicyService>()
    val formioFormulierenRestService = FormioFormulierenRestService(formioService, policyService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A list of formio forms") {
        val formioForms = listOf(
            createFormioFormulier(
                id = 1234L,
                name = "name1"
            ),
            createFormioFormulier(
                id = 5678L,
                name = "name2"
            ),
        )
        every { policyService.readOverigeRechten().beheren } returns true
        every { formioService.listFormulieren() } returns formioForms

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
    Given("Formio form data") {
        every { policyService.readOverigeRechten().beheren } returns true
        every { formioService.addFormulier(any(), any()) } just Runs
        val formioFormulierContent = createRestFormioFormulierContent()

        When("a new formio is created") {
            val response = formioFormulierenRestService.createFormulier(formioFormulierContent)

            Then("it should add the FormioFormulier and return a created response") {
                verify(exactly = 1) {
                    formioService.addFormulier(formioFormulierContent.filename, formioFormulierContent.content)
                }
                response.status shouldBe HttpStatus.SC_CREATED
            }
        }
    }
    Given("A formio form") {
        val id = 1L
        every { policyService.readOverigeRechten().beheren } returns true
        every { formioService.deleteFormulier(any()) } just Runs

        When("the form is deleted") {
            val response = formioFormulierenRestService.deleteFormulier(id)

            Then("it should delete the form") {
                verify(exactly = 1) {
                    formioService.deleteFormulier(id)
                }
                response.status shouldBe HttpStatus.SC_NO_CONTENT
            }
        }
    }
})
