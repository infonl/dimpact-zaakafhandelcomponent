/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.productaanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import nl.info.client.klant.KlantClientService
import nl.info.client.klant.createDigitalAddress
import nl.info.client.klanten.model.generated.SoortDigitaalAdresEnum
import nl.info.client.zgw.model.createZaak
import nl.info.zac.admin.model.createAutomaticEmailConfirmation
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.MailGegevens
import nl.info.zac.mailtemplates.model.createMailTemplate
import nl.info.zac.zaak.ZaakService

class ProductaanvraagEmailServiceTest : BehaviorSpec({
    val klantClientService = mockk<KlantClientService>()
    val configurationService = mockk<ConfigurationService>()
    val zaakService = mockk<ZaakService>()
    val mailService = mockk<MailService>()
    val mailTemplateService = mockk<MailTemplateService>()

    val productaanvraagEmailService = ProductaanvraagEmailService(
        klantClientService,
        configurationService,
        zaakService,
        mailService,
        mailTemplateService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("zaak created from productaanvraag and automatic email is enabled") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene()
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val receiverEmail = "receiver@example.com"
        val otherEmail = "other@example.com"
        val digitalAddresses = listOf(
            createDigitalAddress(
                address = otherEmail,
                soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                isStandaardAdres = false
            ),
            createDigitalAddress(
                address = receiverEmail,
                soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                isStandaardAdres = true
            )
        )
        val mailTemplate = createMailTemplate()
        val mailGegevens = slot<MailGegevens>()
        val bronnen = slot<Bronnen>()

        every {
            klantClientService.findDigitalAddressesForNaturalPerson(betrokkene.inpBsn)
        } returns digitalAddresses
        every {
            mailTemplateService.findMailtemplateByName(zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName!!)
        } returns mailTemplate
        every { mailService.sendMail(capture(mailGegevens), capture(bronnen)) } returns "body"
        every { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak) } just runs

        When("sendConfirmationOfReceiptEmailFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                betrokkene,
                null,
                zaaktypeCmmnConfiguration
            )

            Then("email is sent to the correct address") {
                verify(exactly = 1) {
                    mailService.sendMail(any<MailGegevens>(), any<Bronnen>())
                }
                with(mailGegevens.captured) {
                    from.email shouldBe zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.emailSender
                    to.email shouldBe receiverEmail
                    replyTo!!.email shouldBe zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.emailReply
                    subject shouldBe mailTemplate.onderwerp
                    body shouldBe mailTemplate.body
                    attachments shouldBe emptyList()
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

    Given("zaak created from productaanvraag and automatic email is enabled with GEMEENTE as sender") {
        val councilEmailAddress = "fake-council@example.com"
        val councilName = "Fake Council"
        val zaak = createZaak()
        val betrokkene = createBetrokkene()
        val automaticEmailConfirmation = createAutomaticEmailConfirmation(
            emailSender = "GEMEENTE",
            emailReply = "GEMEENTE"
        )
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeCmmnEmailParameters = automaticEmailConfirmation
        )
        val receiverEmail = "receiver@example.com"
        val digitalAddress = createDigitalAddress(
            address = receiverEmail,
            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL
        )
        val mailTemplate = createMailTemplate()
        val mailGegevens = slot<MailGegevens>()
        val bronnen = slot<Bronnen>()

        every {
            klantClientService.findDigitalAddressesForNaturalPerson(betrokkene.inpBsn)
        } returns listOf(digitalAddress)
        every {
            mailTemplateService.findMailtemplateByName(zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName!!)
        } returns mailTemplate

        every { configurationService.readGemeenteMail() } returns councilEmailAddress
        every { configurationService.readGemeenteNaam() } returns councilName
        every { mailService.sendMail(capture(mailGegevens), capture(bronnen)) } returns "body"
        every { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak) } just runs

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                betrokkene,
                null,
                zaaktypeCmmnConfiguration
            )

            Then("email is sent") {
                verify(exactly = 1) {
                    mailService.sendMail(any<MailGegevens>(), any<Bronnen>())
                }
                with(mailGegevens.captured) {
                    from.email shouldBe councilEmailAddress
                    to.email shouldBe receiverEmail
                    replyTo!!.email shouldBe councilEmailAddress
                    subject shouldBe mailTemplate.onderwerp
                    body shouldBe mailTemplate.body
                    attachments shouldBe emptyList()
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
        val betrokkene = createBetrokkene(inBsn = null)
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val receiverEmail = "receiver@example.com"
        val otherEmail = "other@example.com"
        val digitalAddresses = listOf(
            createDigitalAddress(
                address = otherEmail,
                soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                isStandaardAdres = false
            ),
            createDigitalAddress(
                address = receiverEmail,
                soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                isStandaardAdres = true
            )
        )
        val mailTemplate = createMailTemplate()
        val mailGegevens = slot<MailGegevens>()
        val bronnen = slot<Bronnen>()

        every {
            klantClientService.findDigitalAddressesForVestiging(betrokkene.vestigingsNummer, betrokkene.kvkNummer)
        } returns digitalAddresses
        every {
            mailTemplateService.findMailtemplateByName(zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName!!)
        } returns mailTemplate
        every { mailService.sendMail(capture(mailGegevens), capture(bronnen)) } returns "body"
        every { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak) } just runs

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                betrokkene,
                null,
                zaaktypeCmmnConfiguration
            )

            Then("email is sent to the correct address") {
                verify(exactly = 1) {
                    mailService.sendMail(any<MailGegevens>(), any<Bronnen>())
                }
                with(mailGegevens.captured) {
                    from.email shouldBe zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.emailSender
                    to.email shouldBe receiverEmail
                    replyTo!!.email shouldBe zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.emailReply
                    subject shouldBe mailTemplate.onderwerp
                    body shouldBe mailTemplate.body
                    attachments shouldBe emptyList()
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
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeCmmnEmailParameters = createAutomaticEmailConfirmation(enabled = false)
        )

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                betrokkene,
                null,
                zaaktypeCmmnConfiguration
            )

            Then("no action is taken") {}
        }
    }

    Given("zaak created from productaanvraag and no mail template found") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene(inBsn = null)
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val receiverEmail = "receiver@example.com"
        val digitalAddress = createDigitalAddress(
            address = receiverEmail,
            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL
        )

        every {
            klantClientService.findDigitalAddressesForVestiging(betrokkene.vestigingsNummer, betrokkene.kvkNummer)
        } returns listOf(digitalAddress)
        every {
            mailTemplateService.findMailtemplateByName(zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName!!)
        } returns null

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                betrokkene,
                null,
                zaaktypeCmmnConfiguration
            )

            Then("no mail is sent") {
                verify(exactly = 1) {
                    mailTemplateService.findMailtemplateByName(
                        zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName!!
                    )
                }
            }
        }
    }

    Given("zaak created from productaanvraag and no identification for the initiator") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene(inBsn = null, kvkNummer = null)
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                betrokkene,
                null,
                zaaktypeCmmnConfiguration
            )

            Then("no mail is sent") {}
        }
    }

    Given("zaak created from productaanvraag and initiator without email") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene()
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val digitalAddress = createDigitalAddress()

        every {
            klantClientService.findDigitalAddressesForNaturalPerson(betrokkene.inpBsn)
        } returns listOf(digitalAddress)

        When("sendEmailForZaakFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                betrokkene,
                null,
                zaaktypeCmmnConfiguration
            )

            Then("no mail is sent") {}
        }
    }

    Given(
        "zaak created from productaanvraag with an application-specific email address and automatic email is enabled"
    ) {
        val zaak = createZaak()
        val betrokkene = createBetrokkene()
        val specificEmail = "specific@server.com"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val mailTemplate = createMailTemplate()
        val mailGegevens = slot<MailGegevens>()
        val bronnen = slot<Bronnen>()

        every {
            mailTemplateService.findMailtemplateByName(zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName!!)
        } returns mailTemplate
        every { mailService.sendMail(capture(mailGegevens), capture(bronnen)) } returns "body"
        every { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak) } just runs

        When("sendConfirmationOfReceiptEmailFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                betrokkene,
                specificEmail,
                zaaktypeCmmnConfiguration
            )

            Then("email is sent to the application-specific email address without consulting betrokkene") {
                verify(exactly = 1) {
                    mailService.sendMail(any<MailGegevens>(), any<Bronnen>())
                }
                verify(exactly = 0) {
                    klantClientService.findDigitalAddressesForNaturalPerson(any())
                }
                mailGegevens.captured.to.email shouldBe specificEmail
            }

            And("sent email flag is set") {
                verify(exactly = 1) {
                    zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)
                }
            }
        }
    }

    Given("zaak created from productaanvraag with an application-specific email address and no betrokkene") {
        val zaak = createZaak()
        val specificEmail = "specific@server.com"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val mailTemplate = createMailTemplate()
        val mailGegevens = slot<MailGegevens>()
        val bronnen = slot<Bronnen>()

        every {
            mailTemplateService.findMailtemplateByName(zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName!!)
        } returns mailTemplate
        every { mailService.sendMail(capture(mailGegevens), capture(bronnen)) } returns "body"
        every { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak) } just runs

        When("sendConfirmationOfReceiptEmailFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                null,
                specificEmail,
                zaaktypeCmmnConfiguration
            )

            Then("email is sent to the application-specific email address") {
                verify(exactly = 1) {
                    mailService.sendMail(any<MailGegevens>(), any<Bronnen>())
                }
                mailGegevens.captured.to.email shouldBe specificEmail
            }

            And("sent email flag is set") {
                verify(exactly = 1) {
                    zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)
                }
            }
        }
    }

    Given("zaak created from productaanvraag with no betrokkene and no application-specific email address") {
        val zaak = createZaak()
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()

        When("sendConfirmationOfReceiptEmailFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                null,
                null,
                zaaktypeCmmnConfiguration
            )

            Then("no mail is sent") {}
        }
    }

    Given("zaak created from productaanvraag and no email address is marked as standaard adres") {
        val zaak = createZaak()
        val betrokkene = createBetrokkene()
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val firstEmail = "first@example.com"
        val secondEmail = "second@example.com"
        val digitalAddresses = listOf(
            createDigitalAddress(
                address = firstEmail,
                soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                isStandaardAdres = false
            ),
            createDigitalAddress(
                address = secondEmail,
                soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                isStandaardAdres = null
            )
        )
        val mailTemplate = createMailTemplate()
        val mailGegevens = slot<MailGegevens>()
        val bronnen = slot<Bronnen>()

        every {
            klantClientService.findDigitalAddressesForNaturalPerson(betrokkene.inpBsn)
        } returns digitalAddresses
        every {
            mailTemplateService.findMailtemplateByName(zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName!!)
        } returns mailTemplate
        every { mailService.sendMail(capture(mailGegevens), capture(bronnen)) } returns "body"
        every { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak) } just runs

        When("sendConfirmationOfReceiptEmailFromProductaanvraag is called") {
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak,
                betrokkene,
                null,
                zaaktypeCmmnConfiguration
            )

            Then("email is sent to the first address as fallback") {
                verify(exactly = 1) {
                    mailService.sendMail(any<MailGegevens>(), any<Bronnen>())
                }
                mailGegevens.captured.to.email shouldBe firstEmail
            }
        }
    }
})
