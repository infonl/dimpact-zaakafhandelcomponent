/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.productaanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.client.klant.KlantClientService
import net.atos.client.klant.createDigitalAddress
import net.atos.client.klant.model.SoortDigitaalAdresEnum
import net.atos.zac.mailtemplates.MailTemplateService
import net.atos.zac.mailtemplates.model.MailGegevens
import net.atos.zac.mailtemplates.model.createMailTemplate
import nl.info.client.zgw.model.createZaak
import nl.info.zac.admin.model.createAutomaticEmailConfirmation
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.zaak.ZaakService
import java.util.Optional

class ProductaanvraagEmailServiceTest : BehaviorSpec({
    val klantClientService = mockk<KlantClientService>()
    val zaakService = mockk<ZaakService>()
    val mailService = mockk<MailService>()
    val mailTemplateService = mockk<MailTemplateService>()

    val productaanvraagEmailService = ProductaanvraagEmailService(
        klantClientService,
        zaakService,
        mailService,
        mailTemplateService
    )

    Given("zaak created from productaanvraag and automatic email is enabled") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene()
        val zaakafhandelParameters = createZaakafhandelParameters()
        val receiverEmail = "receiver@server.com"
        val digitalAddress = createDigitalAddress(
            address = receiverEmail,
            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL
        )
        val mailTemplate = createMailTemplate()
        val mailGegevens = slot<MailGegevens>()
        val bronnen = slot<Bronnen>()

        every {
            klantClientService.findDigitalAddressesByNumber(betrokkene.inpBsn)
        } returns listOf(digitalAddress)
        every {
            mailTemplateService.findMailtemplateByName(zaakafhandelParameters.automaticEmailConfirmation.templateName)
        } returns Optional.of(mailTemplate)
        every { mailService.sendMail(capture(mailGegevens), capture(bronnen)) } returns "body"
        every { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak) } just runs

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, betrokkene, zaakafhandelParameters)

            Then("email is sent") {
                verify(exactly = 1) {
                    mailService.sendMail(any<MailGegevens>(), any<Bronnen>())
                }
                with(mailGegevens.captured) {
                    from.email shouldBe zaakafhandelParameters.automaticEmailConfirmation.emailSender
                    to.email shouldBe receiverEmail
                    replyTo.email shouldBe zaakafhandelParameters.automaticEmailConfirmation.emailReply
                    subject shouldBe mailTemplate.onderwerp
                    body shouldBe mailTemplate.body
                    attachments shouldBe emptyArray()
                    isCreateDocumentFromMail shouldBe true
                }
            }

            And("sent email flag is set") {
                verify(exactly = 1) {
                    zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)
                }
            }
        }
    }

    Given("zaak with bedrijf as an initiator is created from productaanvraag and automatic email is enabled") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene()
        val zaakafhandelParameters = createZaakafhandelParameters()
        val receiverEmail = "receiver@server.com"
        val digitalAddress = createDigitalAddress(
            address = receiverEmail,
            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL
        )
        val mailTemplate = createMailTemplate()
        val mailGegevens = slot<MailGegevens>()
        val bronnen = slot<Bronnen>()

        every {
            klantClientService.findDigitalAddressesByNumber(betrokkene.inpBsn)
        } returns listOf(digitalAddress)
        every {
            mailTemplateService.findMailtemplateByName(zaakafhandelParameters.automaticEmailConfirmation.templateName)
        } returns Optional.of(mailTemplate)
        every { mailService.sendMail(capture(mailGegevens), capture(bronnen)) } returns "body"
        every { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak) } just runs

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, betrokkene, zaakafhandelParameters)

            Then("email is sent") {
                verify(exactly = 1) {
                    mailService.sendMail(any<MailGegevens>(), any<Bronnen>())
                }
                with(mailGegevens.captured) {
                    from.email shouldBe zaakafhandelParameters.automaticEmailConfirmation.emailSender
                    to.email shouldBe receiverEmail
                    replyTo.email shouldBe zaakafhandelParameters.automaticEmailConfirmation.emailReply
                    subject shouldBe mailTemplate.onderwerp
                    body shouldBe mailTemplate.body
                    attachments shouldBe emptyArray()
                    isCreateDocumentFromMail shouldBe true
                }
            }

            And("sent email flag is set") {
                verify(exactly = 1) {
                    zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)
                }
            }
        }
    }

    Given("zaak created from productaanvraag and automatic email is disabled") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene()
        val zaakafhandelParameters = createZaakafhandelParameters(
            automaticEmailConfirmation = createAutomaticEmailConfirmation(enabled = false)
        )

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, betrokkene, zaakafhandelParameters)

            Then("no action is taken") {}
        }
    }

    Given("zaak created from productaanvraag and no mail template found") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene(inBsn = null)
        val zaakafhandelParameters = createZaakafhandelParameters()
        val receiverEmail = "receiver@server.com"
        val digitalAddress = createDigitalAddress(
            address = receiverEmail,
            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL
        )

        every {
            klantClientService.findDigitalAddressesByNumber(betrokkene.vestigingsNummer)
        } returns listOf(digitalAddress)
        every {
            mailTemplateService.findMailtemplateByName(zaakafhandelParameters.automaticEmailConfirmation.templateName)
        } returns Optional.empty()

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, betrokkene, zaakafhandelParameters)

            Then("no mail is sent") {
                verify(exactly = 1) {
                    mailTemplateService.findMailtemplateByName(
                        zaakafhandelParameters.automaticEmailConfirmation.templateName
                    )
                }
            }
        }
    }

    Given("zaak created from productaanvraag and no initiator") {
        val zaak = createZaak()
        val zaakafhandelParameters = createZaakafhandelParameters()

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, null, zaakafhandelParameters)

            Then("no mail is sent") {}
        }
    }

    Given("zaak created from productaanvraag and no identification for the initiator") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene(inBsn = null, vestigingsNummer = null)
        val zaakafhandelParameters = createZaakafhandelParameters()

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, betrokkene, zaakafhandelParameters)

            Then("no mail is sent") {}
        }
    }

    Given("zaak created from productaanvraag and initiator without email") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene()
        val zaakafhandelParameters = createZaakafhandelParameters()
        val digitalAddress = createDigitalAddress()

        every {
            klantClientService.findDigitalAddressesByNumber(betrokkene.inpBsn)
        } returns listOf(digitalAddress)

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, betrokkene, zaakafhandelParameters)

            Then("no mail is sent") {}
        }
    }
})
