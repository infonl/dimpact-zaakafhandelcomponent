/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.klant.KlantClientService
import nl.info.client.klanten.model.generated.SoortDigitaalAdresEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCmmnEmailParameters
import nl.info.zac.admin.model.ZaaktypeCmmnZaakafzenderParameters
import nl.info.zac.app.klant.model.contactdetails.getStandaardAdres
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mail.model.getBronnenFromZaak
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.MailGegevens
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.productaanvraag.util.performAction
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ProductaanvraagEmailService @Inject constructor(
    private val klantClientService: KlantClientService,
    private val configurationService: ConfigurationService,
    private val zaakService: ZaakService,
    private val mailService: MailService,
    private val mailTemplateService: MailTemplateService,
) {
    companion object {
        private val LOG = Logger.getLogger(ProductaanvraagEmailService::class.java.name)
    }

    fun sendConfirmationOfReceiptEmailFromProductaanvraag(
        zaak: Zaak,
        betrokkene: Betrokkene,
        zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) {
        LOG.fine {
            "Attempting to send automatic confirmation of receipt email for zaak with identification '${zaak.identificatie}' " +
                "and zaaktype '${zaak.zaaktype}' to zaak initiator."
        }
        zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.takeIf { it.enabled }?.let { zaaktypeCmmnEmailParameters ->
            extractBetrokkeneEmail(betrokkene)?.let { to ->
                sendConfirmationOfReceiptMail(zaaktypeCmmnEmailParameters, to, zaak)
            } ?: LOG.fine {
                "No email address found for initiator of zaak with identification: '${zaak.identificatie}' " +
                    "and zaaktype '${zaak.zaaktype}'. Skipping automatic email confirmation."
            }
        }
    }

    private fun extractBetrokkeneEmail(betrokkene: Betrokkene) =
        betrokkene.performAction(
            onNatuurlijkPersoonIdentity = ::fetchEmailForNatuurlijkPersoon,
            onKvkIdentity = ::fetchEmail,
            onNoIdentity = { null }
        )

    private fun fetchEmailForNatuurlijkPersoon(identity: String): String? =
        klantClientService.findDigitalAddressesForNaturalPerson(identity)
            .filter { it.soortDigitaalAdres == SoortDigitaalAdresEnum.EMAIL }
            .getStandaardAdres()
            ?.adres

    private fun fetchEmail(kvkNummer: String, vestigingsNummer: String?): String? {
        val digitalAddresses = if (vestigingsNummer != null) {
            klantClientService.findDigitalAddressesForVestiging(vestigingsNummer, kvkNummer)
        } else {
            // KVK companies are always stored as non-natural persons in Open Klant
            klantClientService.findDigitalAddressesForNonNaturalPerson(kvkNummer)
        }
        return digitalAddresses
            .filter { it.soortDigitaalAdres == SoortDigitaalAdresEnum.EMAIL }
            .getStandaardAdres()
            ?.adres
    }

    private fun sendConfirmationOfReceiptMail(
        zaaktypeCmmnEmailParameters: ZaaktypeCmmnEmailParameters,
        to: String,
        zaakFromProductaanvraag: Zaak
    ) {
        zaaktypeCmmnEmailParameters.templateName?.let { templateName ->
            mailTemplateService.findMailtemplateByName(templateName)?.let { mailTemplate ->
                configureEmail(zaaktypeCmmnEmailParameters, to, mailTemplate)?.let { mailGegevens ->
                    mailService.sendMail(mailGegevens, zaakFromProductaanvraag.getBronnenFromZaak())?.also {
                        zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaakFromProductaanvraag)
                    }
                } ?: LOG.warning(
                    "No email sender configured for zaaktype '${zaakFromProductaanvraag.zaaktype}.' " +
                        "Skipping automatic email confirmation."
                )
            } ?: LOG.warning(
                "No mail template found with name: '${zaaktypeCmmnEmailParameters.templateName}'. " +
                    "Skipping automatic email confirmation."
            )
        } ?: LOG.warning(
            "No email template configured for zaaktype '${zaakFromProductaanvraag.zaaktype}'. " +
                "Skipping automatic email confirmation."
        )
    }

    private fun configureEmail(
        zaaktypeCmmnEmailParameters: ZaaktypeCmmnEmailParameters,
        to: String,
        mailTemplate: MailTemplate
    ) = zaaktypeCmmnEmailParameters.emailSender?.let { emailSender ->
        MailGegevens(
            from = emailSender.generateMailAddress(configurationService),
            to = MailAdres(email = to, name = null),
            replyTo = zaaktypeCmmnEmailParameters.emailReply?.generateMailAddress(configurationService),
            subject = mailTemplate.onderwerp,
            body = mailTemplate.body,
            attachments = null,
            isCreateDocumentFromMail = true
        )
    }

    private fun String.generateMailAddress(configurationService: ConfigurationService) =
        when (this) {
            ZaaktypeCmmnZaakafzenderParameters.SpecialMail.GEMEENTE.toString() -> MailAdres(
                email = configurationService.readGemeenteMail(),
                name = configurationService.readGemeenteNaam()
            )
            else -> MailAdres(
                email = this,
                name = null
            )
        }
}
