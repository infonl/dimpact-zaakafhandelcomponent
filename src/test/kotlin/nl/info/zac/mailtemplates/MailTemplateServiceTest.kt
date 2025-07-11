/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates

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
import nl.info.zac.mailtemplates.model.MailTemplate
import java.util.Optional

class MailTemplateServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<MailTemplate>>(relaxed = true)
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
            mailTemplateNaam = mailTemplateName
            onderwerp = "Welcome"
            body = "Hello, welcome!"
            mail = Mail.ZAAK_ONTVANKELIJK
            isDefaultMailtemplate = true
        }

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
        every { criteriaQuery.from(MailTemplate::class.java) } returns root
        every { root.get<String>("mailTemplateNaam") } returns mailTemplateNamePath
        every { criteriaBuilder.equal(mailTemplateNamePath, mailTemplateName) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultList.stream().findFirst() } returns Optional.of(mailTemplate)

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
        every { root.get<String>("mailTemplateNaam") } returns mailTemplateNamePath
        every { criteriaBuilder.equal(mailTemplateNamePath, mailTemplateName) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultList.stream().findFirst() } returns Optional.empty()

        When("findMailtemplateByName is called") {
            val result = mailTemplateService.findMailtemplateByName(mailTemplateName)

            then("it should return an empty Optional") {
                result.isPresent shouldBe false
            }
        }
    }
})
