/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.productaanvraag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.zac.admin.model.createAutomaticEmailConfirmation
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.productaanvraag.exception.EmailAddressNotFoundException
import nl.info.zac.productaanvraag.exception.InitiatorNotFoundException
import nl.info.zac.productaanvraag.exception.MailTemplateNotFoundException
import nl.info.zac.zaak.ZaakService
import java.util.Optional

class ProductaanvraagEmailServiceTest : BehaviorSpec({
    val zgwApiService = mockk<ZGWApiService>()
    val klantClientService = mockk<KlantClientService>()
    val zaakService = mockk<ZaakService>()
    val mailService = mockk<MailService>()
    val mailTemplateService = mockk<MailTemplateService>()

    val productaanvraagEmailService = ProductaanvraagEmailService(
        zgwApiService,
        klantClientService,
        zaakService,
        mailService,
        mailTemplateService
    )

    Given("zaak created from productaanvraag and automatic email is enabled") {
        val zaak = createZaak()
        val zaakafhandelParameters = createZaakafhandelParameters()
        val rolMedewerker = createRolMedewerker()
        val receiverEmail = "receiver@server.com"
        val digitalAddress = createDigitalAddress(
            address = receiverEmail,
            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL
        )
        val mailTemplate = createMailTemplate()
        val mailGegevens = slot<MailGegevens>()
        val bronnen = slot<Bronnen>()

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolMedewerker
        every {
            klantClientService.findDigitalAddressesByNumber(rolMedewerker.identificatienummer)
        } returns listOf(digitalAddress)
        every {
            mailTemplateService.findMailtemplateByName(zaakafhandelParameters.automaticEmailConfirmation.templateName)
        } returns Optional.of(mailTemplate)
        every { mailService.sendMail(capture(mailGegevens), capture(bronnen)) } returns "body"
        every { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak) } just runs

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, zaakafhandelParameters)

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
        val zaakafhandelParameters = createZaakafhandelParameters(
            automaticEmailConfirmation = createAutomaticEmailConfirmation(enabled = false)
        )

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, zaakafhandelParameters)

            Then("no action is taken") {}
        }
    }

    Given("zaak created from productaanvraag and no mail template found") {
        val zaak = createZaak()
        val zaakafhandelParameters = createZaakafhandelParameters()
        val rolMedewerker = createRolMedewerker()
        val receiverEmail = "receiver@server.com"
        val digitalAddress = createDigitalAddress(
            address = receiverEmail,
            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL
        )

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolMedewerker
        every {
            klantClientService.findDigitalAddressesByNumber(rolMedewerker.identificatienummer)
        } returns listOf(digitalAddress)
        every {
            mailTemplateService.findMailtemplateByName(zaakafhandelParameters.automaticEmailConfirmation.templateName)
        } returns Optional.empty()

        When("sendEmailForZaakFromProductaanvraag is called") {
            val exception = shouldThrow<MailTemplateNotFoundException> {
                productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, zaakafhandelParameters)
            }

            Then("template name is logged and exception is thrown") {
                exception.message shouldContain zaakafhandelParameters.automaticEmailConfirmation.templateName
                verify(exactly = 1) {
                    mailTemplateService.findMailtemplateByName(
                        zaakafhandelParameters.automaticEmailConfirmation.templateName
                    )
                }
            }
        }
    }

    Given("zaak created from productaanvraag and no initiator rol") {
        val zaak = createZaak()
        val zaakafhandelParameters = createZaakafhandelParameters()

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns null

        When("sendEmailForZaakFromProductaanvraag is called") {
            shouldThrow<InitiatorNotFoundException> {
                productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, zaakafhandelParameters)
            }

            Then("exception is thrown") {
                verify(exactly = 1) {
                    zgwApiService.findInitiatorRoleForZaak(zaak)
                }
            }
        }
    }

    Given("zaak created from productaanvraag and initiator rol with no identification") {
        val zaak = createZaak()
        val zaakafhandelParameters = createZaakafhandelParameters()
        val rolMedewerker = createRolNatuurlijkPersoon(natuurlijkPersoonIdentificatie = null)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolMedewerker

        When("sendEmailForZaakFromProductaanvraag is called") {
            shouldThrow<InitiatorNotFoundException> {
                productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, zaakafhandelParameters)
            }

            Then("exception is thrown") {
                verify(exactly = 1) {
                    zgwApiService.findInitiatorRoleForZaak(zaak)
                }
            }
        }
    }

    Given("zaak created from productaanvraag and initiator without email") {
        val zaak = createZaak()
        val zaakafhandelParameters = createZaakafhandelParameters()
        val identificatie = "1234"
        val rolNatuurlijkPersoon = createRolNatuurlijkPersoon(
            natuurlijkPersoonIdentificatie = NatuurlijkPersoonIdentificatie().apply {
                anpIdentificatie = identificatie
            }
        )

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNatuurlijkPersoon
        every {
            klantClientService.findDigitalAddressesByNumber(identificatie)
        } returns emptyList()

        When("sendEmailForZaakFromProductaanvraag is called") {
            shouldThrow<EmailAddressNotFoundException> {
                productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(zaak, zaakafhandelParameters)
            }

            Then("exception is thrown") {
                verify(exactly = 1) {
                    zgwApiService.findInitiatorRoleForZaak(zaak)
                }
            }
        }
    }
})
