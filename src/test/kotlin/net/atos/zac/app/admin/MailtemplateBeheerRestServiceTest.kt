/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.ws.rs.core.Response
import net.atos.zac.app.admin.model.createRestMailTemplate
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.mailtemplates.model.MailTemplateVariables.Companion.ZAAK_VOORTGANG_VARIABELEN
import nl.info.zac.mailtemplates.model.createMailTemplate
import nl.info.zac.policy.PolicyService

class MailtemplateBeheerRestServiceTest : BehaviorSpec({
    val mailTemplateService = mockk<MailTemplateService>()
    val policyService = mockk<PolicyService>()
    val mailtemplateBeheerRestService = MailtemplateBeheerRestService(
        mailTemplateService,
        policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("REST mail templates can be persisted") {
        Given(
            """
                A REST mail template for a 'ZAAK_ALGEMEEN' email with a subject that includes HTML paragraph tags
                and 'beheren' rechten
                """
        ) {
            val restMailTemplate = createRestMailTemplate(
                mail = Mail.ZAAK_ALGEMEEN.name,
                subject = "fake<p>Subject</p>",
            )
            val storedMailTemplate = createMailTemplate(mail = Mail.ZAAK_ALGEMEEN)
            val mailTemplateSlot = slot<MailTemplate>()
            every { policyService.readOverigeRechten().beheren } returns true
            every { mailTemplateService.storeMailtemplate(capture(mailTemplateSlot)) } returns storedMailTemplate

            When("the mail template is persisted") {
                val storedRestMailTemplate = mailtemplateBeheerRestService.persistMailtemplate(restMailTemplate)

                Then(
                    """
                    the mail template service should be called to persist the template with the HTML paragraph tags stripped
                    from the subject
                    """
                ) {
                    with(mailTemplateSlot.captured) {
                        id shouldBe restMailTemplate.id
                        mailTemplateNaam shouldBe restMailTemplate.mailTemplateNaam
                        onderwerp shouldBe "fakeSubject"
                        body shouldBe restMailTemplate.body
                        mail.name shouldBe restMailTemplate.mail
                        isDefaultMailtemplate shouldBe restMailTemplate.defaultMailtemplate
                    }
                }

                And("the stored REST mail template should be returned") {
                    with(storedRestMailTemplate) {
                        id shouldBe storedMailTemplate.id
                        mailTemplateNaam shouldBe storedMailTemplate.mailTemplateNaam
                        onderwerp shouldBe storedMailTemplate.onderwerp
                        body shouldBe storedMailTemplate.body
                        mail shouldBe storedMailTemplate.mail.name
                        defaultMailtemplate shouldBe storedMailTemplate.isDefaultMailtemplate
                    }
                }
            }
        }
    }

    Context("Creating new mail templates via POST endpoint") {
        Given("A REST mail template without ID and 'beheren' rechten") {
            val restMailTemplate = createRestMailTemplate(
                mail = Mail.ZAAK_ALGEMEEN.name,
                subject = "New<p>Template</p>",
            ).apply { id = null } // No ID provided for creation
            val createdMailTemplate = createMailTemplate(
                id = 123L,
                mail = Mail.ZAAK_ALGEMEEN
            )
            val mailTemplateSlot = slot<MailTemplate>()
            every { policyService.readOverigeRechten().beheren } returns true
            every { mailTemplateService.createMailtemplate(capture(mailTemplateSlot)) } returns createdMailTemplate

            When("the mail template is created via POST") {
                val response = mailtemplateBeheerRestService.createMailtemplate(restMailTemplate)

                Then("the mail template service should be called with convertForCreate") {
                    verify { mailTemplateService.createMailtemplate(any()) }
                    with(mailTemplateSlot.captured) {
                        id shouldBe 0L // Should be 0 for new entities
                        mailTemplateNaam shouldBe restMailTemplate.mailTemplateNaam
                        onderwerp shouldBe "NewTemplate" // HTML tags stripped
                        body shouldBe restMailTemplate.body
                        mail.name shouldBe restMailTemplate.mail
                        isDefaultMailtemplate shouldBe restMailTemplate.defaultMailtemplate
                    }
                }

                And("it should return HTTP 201 Created with the created template") {
                    response.status shouldBe Response.Status.CREATED.statusCode
                    val createdRestTemplate = response.entity as net.atos.zac.app.admin.model.RESTMailtemplate
                    createdRestTemplate.id shouldBe createdMailTemplate.id
                    createdRestTemplate.mailTemplateNaam shouldBe createdMailTemplate.mailTemplateNaam
                }
            }
        }
    }

    Context("Updating existing mail templates via PUT endpoint") {
        Given("A REST mail template with ID and 'beheren' rechten") {
            val templateId = 456L
            val restMailTemplate = createRestMailTemplate(
                mail = Mail.ZAAK_ALGEMEEN.name,
                subject = "Updated<p>Template</p>",
            )
            val updatedMailTemplate = createMailTemplate(
                id = templateId,
                mail = Mail.ZAAK_ALGEMEEN
            )
            val idSlot = slot<Long>()
            val mailTemplateSlot = slot<MailTemplate>()
            every { policyService.readOverigeRechten().beheren } returns true
            every { 
                mailTemplateService.updateMailtemplate(capture(idSlot), capture(mailTemplateSlot)) 
            } returns updatedMailTemplate

            When("the mail template is updated via PUT") {
                val updatedRestTemplate = mailtemplateBeheerRestService.updateMailtemplate(templateId, restMailTemplate)

                Then("the mail template service should be called with the correct ID and convertForUpdate") {
                    verify { mailTemplateService.updateMailtemplate(templateId, any()) }
                    idSlot.captured shouldBe templateId
                    with(mailTemplateSlot.captured) {
                        mailTemplateNaam shouldBe restMailTemplate.mailTemplateNaam
                        onderwerp shouldBe "UpdatedTemplate" // HTML tags stripped
                        body shouldBe restMailTemplate.body
                        mail.name shouldBe restMailTemplate.mail
                        isDefaultMailtemplate shouldBe restMailTemplate.defaultMailtemplate
                    }
                }

                And("it should return the updated template") {
                    updatedRestTemplate.id shouldBe updatedMailTemplate.id
                    updatedRestTemplate.mailTemplateNaam shouldBe updatedMailTemplate.mailTemplateNaam
                }
            }
        }
    }

    Context("Retrieving variables for a mail template") {
        Given("A mail template for ZAAK_ALGEMEEN") {
            val mail = Mail.ZAAK_ALGEMEEN

            When("retrieveMailVariables is called") {
                val mailTemplateVariables = mailtemplateBeheerRestService.getMailTemplateVariables(mail)

                Then("it should return the associated variables") {
                    mailTemplateVariables shouldBe ZAAK_VOORTGANG_VARIABELEN
                }
            }
        }
    }
})
