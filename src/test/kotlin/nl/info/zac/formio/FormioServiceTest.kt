/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.formio

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import nl.info.zac.formio.model.FormioFormulier
import java.util.Optional

class FormioServiceTest : BehaviorSpec({
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<FormioFormulier>>()
    val root = mockk<Root<FormioFormulier>>()
    val entityManager = mockk<EntityManager>()
    val formioService = FormioService(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A formio form") {
        val testFormName = "testForm"
        val content = """{ "dummyKey": "dummyValue" }""".trimIndent()
        val formioFormulier = createFormioFormulier(
            name = testFormName,
            content = content
        )
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(FormioFormulier::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(any(), testFormName) } returns mockk()
        every { criteriaQuery.from(FormioFormulier::class.java) } returns root
        every { criteriaQuery.where(any()) } returns criteriaQuery
        every { root.get<String>("name") } returns mockk()
        every {
            entityManager.createQuery(any<CriteriaQuery<FormioFormulier>>()).resultStream.findFirst()
        } returns Optional.of(formioFormulier)

        When("the formio form is read") {
            val result = formioService.readFormioFormulier(testFormName)

            Then("the formio form as JSON object is returned") {
                result.getString("dummyKey") shouldBe "dummyValue"
            }
        }
    }
    Given("A formio ID for which a form exists") {
        val formid = 1234L
        val formioFormulier = createFormioFormulier(id = formid)
        every { entityManager.find(FormioFormulier::class.java, formid) } returns formioFormulier
        every { entityManager.remove(formioFormulier) } just Runs

        When("deleteFormulier is called with this ID") {
            formioService.deleteFormulier(formid)

            Then("it should remove the formio formulier") {
                verify(exactly = 1) { entityManager.remove(formioFormulier) }
            }
        }
    }
    Given("A formio ID for which no form exists") {
        val id = 1234L
        every { entityManager.find(FormioFormulier::class.java, id) } returns null

        When("deleteFormulier is called with this ID") {
            formioService.deleteFormulier(id)

            Then("it should not attempt to remove any formio formulier") {
                verify(exactly = 0) { entityManager.remove(any<FormioFormulier>()) }
            }
        }
    }
})
