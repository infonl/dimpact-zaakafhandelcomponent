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
import net.atos.zac.mailtemplates.model.Mail
import net.atos.zac.mailtemplates.model.MailTemplate

class MailTemplateServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
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

        every { entityManager.find(MailTemplate::class.java, mailTemplateName) } returns mailTemplate

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
        every { entityManager.find(MailTemplate::class.java, mailTemplateName) } returns null
        When("findMailtemplateByName is called") {
            val result = mailTemplateService.findMailtemplateByName(mailTemplateName)

            then("it should return an empty Optional") {
                result.isPresent shouldBe false
            }
        }
    }
})
