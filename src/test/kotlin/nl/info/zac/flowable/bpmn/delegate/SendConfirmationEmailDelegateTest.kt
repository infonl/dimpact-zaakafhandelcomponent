/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.delegate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.klant.KlantClientService
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.admin.model.createMailTemplate
import nl.info.zac.app.klant.model.contactdetails.ContactDetails
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.MailGegevens
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution

class SendConfirmationEmailDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val klantClientService = mockk<KlantClientService>()
    val zgwApiService = mockk<ZgwApiService>()
    val mailTemplateService = mockk<MailTemplateService>()
    val mailService = mockk<MailService>()

    val zaak = createZaak()
    val fromEmail = "noreply@gemeente.nl"
    val replyToEmail = "support@gemeente.nl"
    val templateName = "Ontvangstbevestiging productaanvraag"
    val mailTemplate = createMailTemplate()
    val initiatorRole = createRolNatuurlijkPersoon()

    val fromExpression = mockk<Expression>()
    val replyToExpression = mockk<Expression>()
    val templateExpression = mockk<Expression>()

    val capturedMailGegevens = mutableListOf<MailGegevens>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a BPMN service task for sending a confirmation email after a product aanvraag") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.klantClientService } returns klantClientService
        every { flowableHelper.mailTemplateService } returns mailTemplateService
        every { flowableHelper.mailService } returns mailService

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie
        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        every { fromExpression.getValue(delegateExecution) } returns fromEmail
        every { templateExpression.getValue(delegateExecution) } returns templateName
        every { mailTemplateService.findMailtemplateByName(templateName) } returns mailTemplate

        When("zaak-specific contact details contain an email address") {
            capturedMailGegevens.clear()
            clearMocks(klantClientService, zgwApiService, mailService, answers = false)
            val zaakSpecificEmail = "klant-zaak@example.com"
            every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns ContactDetails(
                telephoneNumber = null,
                emailAddress = zaakSpecificEmail
            )
            every { mailService.sendMail(capture(capturedMailGegevens), any<Bronnen>()) } returns "mailBody"

            SendConfirmationEmailDelegate().apply {
                from = fromExpression
                template = templateExpression
            }.execute(delegateExecution)

            Then("the mail is sent to the zaak-specific contact email without consulting the initiator role") {
                capturedMailGegevens shouldHaveSize 1
                with(capturedMailGegevens.first()) {
                    to.email shouldBeEqual zaakSpecificEmail
                    from.email shouldBeEqual fromEmail
                    replyTo.shouldBeNull()
                    subject shouldBeEqual mailTemplate.onderwerp
                    body shouldBeEqual mailTemplate.body
                    isCreateDocumentFromMail shouldBe true
                }
                verify(exactly = 0) { zgwApiService.findInitiatorRoleForZaak(any()) }
            }
        }

        When("zaak-specific contact details have no email address and the initiator role has one") {
            capturedMailGegevens.clear()
            clearMocks(klantClientService, zgwApiService, mailService, answers = false)
            val initiatorEmail = "initiator@example.com"
            every { flowableHelper.zgwApiService } returns zgwApiService
            every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns ContactDetails(
                telephoneNumber = "0612345678",
                emailAddress = null
            )
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns initiatorRole
            every { klantClientService.findEmailForInitiatorRole(initiatorRole) } returns initiatorEmail
            every { mailService.sendMail(capture(capturedMailGegevens), any<Bronnen>()) } returns "mailBody"

            SendConfirmationEmailDelegate().apply {
                from = fromExpression
                template = templateExpression
            }.execute(delegateExecution)

            Then("the mail is sent to the initiator email") {
                capturedMailGegevens shouldHaveSize 1
                with(capturedMailGegevens.first()) {
                    to.email shouldBeEqual initiatorEmail
                    from.email shouldBeEqual fromEmail
                    replyTo.shouldBeNull()
                    isCreateDocumentFromMail shouldBe true
                }
            }
        }

        When("no zaak-specific contact details exist and the initiator role has an email address") {
            capturedMailGegevens.clear()
            clearMocks(klantClientService, zgwApiService, mailService, answers = false)
            val initiatorEmail = "initiator@example.com"
            every { flowableHelper.zgwApiService } returns zgwApiService
            every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns initiatorRole
            every { klantClientService.findEmailForInitiatorRole(initiatorRole) } returns initiatorEmail
            every { mailService.sendMail(capture(capturedMailGegevens), any<Bronnen>()) } returns "mailBody"

            SendConfirmationEmailDelegate().apply {
                from = fromExpression
                template = templateExpression
            }.execute(delegateExecution)

            Then("the mail is sent to the initiator email") {
                capturedMailGegevens shouldHaveSize 1
                with(capturedMailGegevens.first()) {
                    to.email shouldBeEqual initiatorEmail
                    from.email shouldBeEqual fromEmail
                    isCreateDocumentFromMail shouldBe true
                }
            }
        }

        When("no zaak-specific contact details exist and the zaak has no initiator role") {
            capturedMailGegevens.clear()
            clearMocks(klantClientService, zgwApiService, mailService, answers = false)
            every { flowableHelper.zgwApiService } returns zgwApiService
            every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns null

            SendConfirmationEmailDelegate().apply {
                from = fromExpression
                template = templateExpression
            }.execute(delegateExecution)

            Then("no mail is sent") {
                capturedMailGegevens shouldHaveSize 0
                verify(exactly = 0) { mailService.sendMail(any<MailGegevens>(), any<Bronnen>()) }
            }
        }

        When("no zaak-specific contact details exist and the initiator role has no email address") {
            capturedMailGegevens.clear()
            clearMocks(klantClientService, zgwApiService, answers = false)
            every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns initiatorRole
            every { klantClientService.findEmailForInitiatorRole(initiatorRole) } returns null

            SendConfirmationEmailDelegate().apply {
                from = fromExpression
                template = templateExpression
            }.execute(delegateExecution)

            Then("no mail is sent") {
                capturedMailGegevens shouldHaveSize 0
                verify(exactly = 0) { mailService.sendMail(any<MailGegevens>(), any<Bronnen>()) }
            }
        }

        When("the configured mail template does not exist") {
            clearMocks(klantClientService, mailTemplateService)
            val zaakSpecificEmail = "klant-zaak@example.com"
            every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns ContactDetails(
                telephoneNumber = null,
                emailAddress = zaakSpecificEmail
            )
            every { mailTemplateService.findMailtemplateByName(templateName) } returns null

            shouldThrow<IllegalArgumentException> {
                SendConfirmationEmailDelegate().apply {
                    from = fromExpression
                    template = templateExpression
                }.execute(delegateExecution)
            }

            Then("an IllegalArgumentException is thrown and no mail is sent") {
                verify(exactly = 0) { mailService.sendMail(any<MailGegevens>(), any<Bronnen>()) }
            }
        }

        When("a replyTo address is configured") {
            capturedMailGegevens.clear()
            clearMocks(klantClientService, mailService, answers = false)
            val zaakSpecificEmail = "klant-zaak@example.com"
            every { replyToExpression.getValue(delegateExecution) } returns replyToEmail
            every { mailTemplateService.findMailtemplateByName(templateName) } returns mailTemplate
            every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns ContactDetails(
                telephoneNumber = null,
                emailAddress = zaakSpecificEmail
            )
            every { mailService.sendMail(capture(capturedMailGegevens), any<Bronnen>()) } returns "mailBody"

            SendConfirmationEmailDelegate().apply {
                from = fromExpression
                replyTo = replyToExpression
                template = templateExpression
            }.execute(delegateExecution)

            Then("the mail is sent with the replyTo address included") {
                capturedMailGegevens shouldHaveSize 1
                with(capturedMailGegevens.first()) {
                    to.email shouldBeEqual zaakSpecificEmail
                    from.email shouldBeEqual fromEmail
                    replyTo!!.email shouldBeEqual replyToEmail
                    isCreateDocumentFromMail shouldBe true
                }
            }
        }
    }
})
