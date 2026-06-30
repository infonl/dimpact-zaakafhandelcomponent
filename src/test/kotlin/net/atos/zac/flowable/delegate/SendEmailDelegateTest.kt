/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.admin.model.createMailTemplate
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.MailGegevens
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import org.flowable.common.engine.impl.el.FixedValue
import org.flowable.common.engine.impl.el.JuelExpression
import org.flowable.engine.delegate.DelegateExecution

class SendEmailDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val mailTemplateService = mockk<MailTemplateService>()
    val mailService = mockk<MailService>()
    val policyService = mockk<PolicyService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = createLoggedInUser()
    val zaak = createZaak()
    val fromEmail = "from@example.com"
    val toEmail = "to@example.com"
    val templateName = "fakeTemplate"
    val mailTemplate = createMailTemplate()

    afterEach {
        checkUnnecessaryStub()
    }

    Given("JUEL expression in a BPMN service task") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.mailTemplateService } returns mailTemplateService
        every { flowableHelper.mailService } returns mailService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(versturenEmail = true)

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
                    isCreateDocumentFromMail shouldBe true
                }
            }
        }
    }

    Given("Fixed value in a BPMN service task") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.mailTemplateService } returns mailTemplateService
        every { flowableHelper.mailService } returns mailService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(versturenEmail = true)

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
                    isCreateDocumentFromMail shouldBe true
                }
            }
        }
    }

    Given("Policy denies sending email for zaak") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny()

        val sendEmailDelegate = SendEmailDelegate().apply {
            from = mockk()
            to = mockk()
            template = mockk()
        }

        When("the delegate is called") {
            val policyException = shouldThrow<PolicyException> {
                sendEmailDelegate.execute(delegateExecution)
            }

            Then("a PolicyException is thrown") {
                policyException shouldNotBe null
            }

            And("no mail is sent") {
                verify(exactly = 0) {
                    mailService.sendMail(any(), any())
                }
            }
        }
    }
})
