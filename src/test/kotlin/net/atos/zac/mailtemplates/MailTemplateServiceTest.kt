/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.mailtemplates

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import net.atos.zac.mailtemplates.model.Mail
import net.atos.zac.mailtemplates.model.MailTemplate

class MailTemplateServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<MailTemplate>>()
    val predicate = mockk<Predicate>()
    val root = mockk<Root<MailTemplate>>()
    val mailTemplateNamePath = mockk<Path<String>>()
    val typedQuery = mockk<TypedQuery<MailTemplate>>()
    val mailTemplateService = MailTemplateService(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a mail template exists for the given name") {
        val mailTemplateName = "welcome_email"
        val mailTemplate = MailTemplate().apply {
            id = 1L
            setMailTemplateNaam(mailTemplateName)
            setOnderwerp("Welcome")
            setBody("Hello, welcome!")
            setMail(Mail.ZAAK_ONTVANKELIJK)
            setDefaultMailtemplate(true)
        }

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
        every { criteriaQuery.from(MailTemplate::class.java) } returns root
        every { root.get<String>("mailTemplateNaam") } returns mailTemplateNamePath
        every { criteriaBuilder.equal(mailTemplateNamePath, mailTemplateName) } returns predicate
        every { criteriaQuery.select(any()) } returns criteriaQuery
        every { criteriaQuery.where(any()) } returns criteriaQuery
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultList } returns listOf(mailTemplate)

        every { entityManager.createQuery(criteriaQuery).resultList } returns listOf(mailTemplate)

        When("findMailtemplateByName is called") {
            val result = mailTemplateService.findMailtemplateByName(mailTemplateName)

            then("it should return the matching MailTemplate in an Optional") {
                result.isPresent shouldBe true
                result.get().mailTemplateNaam shouldBe mailTemplateName
            }
        }
    }

    Given("no mail template exists for the given name") {
        val mailTemplateName = "non_existent_template"

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
        every { criteriaQuery.from(MailTemplate::class.java) } returns root
        every { criteriaQuery.where(predicate) } returns criteriaQuery
        every { criteriaBuilder.equal(mailTemplateNamePath, mailTemplateName) } returns predicate
        every { root.get<String>("mailTemplateNaam") } returns mailTemplateNamePath
        every { entityManager.createQuery(criteriaQuery).resultList } returns emptyList()

        When("findMailtemplateByName is called") {
            val result = mailTemplateService.findMailtemplateByName(mailTemplateName)

            then("it should return an empty Optional") {
                result.isPresent shouldBe false
            }
        }
    }
})
