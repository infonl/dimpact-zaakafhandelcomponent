/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.admin.model.createMailTemplate
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.MailGegevens
import org.flowable.common.engine.impl.el.FixedValue
import org.flowable.engine.delegate.DelegateExecution
import org.flowable.engine.impl.el.JuelExpression

class SendEmailDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val mailTemplateService = mockk<MailTemplateService>()
    val mailService = mockk<MailService>()
    val zaak = createZaak()
    val fromEmail = "from@example.com"
    val toEmail = "to@example.com"
    val templateName = "fakeTemplate"
    val mailTemplate = createMailTemplate()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("JUEL expression in a BPMN service task") {
        mockkStatic(FlowableHelper::class)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.mailTemplateService } returns mailTemplateService
        every { flowableHelper.mailService } returns mailService

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns "fakeUUID"

        every { zrcClientService.readZaakByID("fakeUUID") } returns zaak

        val fromExpression = mockk<JuelExpression>()
        every { fromExpression.getValue(delegateExecution) } returns fromEmail
        val toExpression = mockk<JuelExpression>()
        every { toExpression.getValue(delegateExecution) } returns toEmail
        val templateExpression = mockk<JuelExpression>()
        every { templateExpression.getValue(delegateExecution) } returns templateName

        every { mailTemplateService.findMailtemplateByName(templateName) } returns mailTemplate

        val mailGegevensSlot = mutableListOf<MailGegevens>()
        every {
            mailService.sendMail(capture(mailGegevensSlot), any<Bronnen>())
        } returns "mailBody"

        val sendEmailDelegate = SendEmailDelegate().apply {
            from = fromExpression
            to = toExpression
            template = templateExpression
        }

        When("the delegate is called") {
            sendEmailDelegate.execute(delegateExecution)

            Then("the expressions were resolved") {
                verify(exactly = 1) {
                    fromExpression.getValue(delegateExecution)
                    toExpression.getValue(delegateExecution)
                    templateExpression.getValue(delegateExecution)
                }
            }

            And("the mail was sent") {
                verify(exactly = 1) {
                    mailTemplateService.findMailtemplateByName(templateName)
                    mailService.sendMail(any<MailGegevens>(), any<Bronnen>())
                }
                mailGegevensSlot shouldHaveSize 1
                with(mailGegevensSlot.first()) {
                    to.email shouldBeEqual toEmail
                    from.email shouldBeEqual fromEmail
                }
            }
        }
    }

    Given("Fixed value in a BPMN service task") {
        mockkStatic(FlowableHelper::class)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.mailTemplateService } returns mailTemplateService
        every { flowableHelper.mailService } returns mailService

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns "fakeUUID"

        every { zrcClientService.readZaakByID("fakeUUID") } returns zaak

        val fromExpression = mockk<FixedValue>()
        every { fromExpression.getValue(delegateExecution) } returns fromEmail
        val toExpression = mockk<FixedValue>()
        every { toExpression.getValue(delegateExecution) } returns toEmail
        val templateExpression = mockk<FixedValue>()
        every { templateExpression.getValue(delegateExecution) } returns templateName

        every { mailTemplateService.findMailtemplateByName(templateName) } returns mailTemplate

        val mailGegevensSlot = mutableListOf<MailGegevens>()
        every {
            mailService.sendMail(capture(mailGegevensSlot), any<Bronnen>())
        } returns "mailBody"

        val sendEmailDelegate = SendEmailDelegate().apply {
            from = fromExpression
            to = toExpression
            template = templateExpression
        }

        When("the delegate is called") {
            sendEmailDelegate.execute(delegateExecution)

            Then("the expressions were resolved") {
                verify(exactly = 1) {
                    fromExpression.getValue(delegateExecution)
                    toExpression.getValue(delegateExecution)
                    templateExpression.getValue(delegateExecution)
                }
            }

            And("the mail was sent") {
                verify(exactly = 1) {
                    mailTemplateService.findMailtemplateByName(templateName)
                    mailService.sendMail(any<MailGegevens>(), any<Bronnen>())
                }
                mailGegevensSlot shouldHaveSize 1
                with(mailGegevensSlot.first()) {
                    to.email shouldBeEqual toEmail
                    from.email shouldBeEqual fromEmail
                }
            }
        }
    }
})
