/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.formio

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
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
        every { entityManager.createQuery(any<CriteriaQuery<FormioFormulier>>()).resultStream.findFirst() } returns
            Optional.of(formioFormulier)

        When("the formio form is read") {
            val result = formioService.readFormioFormulier(testFormName)

            Then("the formio form as JSON object is returned") {
                result.getString("dummyKey") shouldBe "dummyValue"
            }
        }
    }
})
