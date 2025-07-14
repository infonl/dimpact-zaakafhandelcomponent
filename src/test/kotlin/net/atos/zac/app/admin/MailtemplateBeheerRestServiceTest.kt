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
import net.atos.zac.app.admin.model.createRestMailTemplate
import net.atos.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.MailTemplate
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
})
