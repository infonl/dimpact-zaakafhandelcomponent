/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.klant.KlantClientService
import nl.info.client.klant.model.CodeObjecttypeEnum
import nl.info.client.klant.model.SoortDigitaalAdresEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCmmnEmailParameters
import nl.info.zac.admin.model.ZaaktypeCmmnZaakafzenderParameters
import nl.info.zac.configuratie.ConfiguratieService
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
    private val configuratieService: ConfiguratieService,
    private val zaakService: ZaakService,
    private val mailService: MailService,
    private val mailTemplateService: MailTemplateService,
) {
    companion object {
        private val LOG = Logger.getLogger(ProductaanvraagEmailService::class.java.name)
    }

    fun sendEmailForZaakFromProductaanvraag(
        zaak: Zaak,
        betrokkene: Betrokkene?,
        zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) {
        LOG.fine {
            "Attempting to send automatic email confirmation for zaak '${zaak.uuid}' " +
                "and zaaktype '${zaak.zaaktype}'. For initiator '$betrokkene'."
        }
        zaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.takeIf { it.enabled }?.let { zaaktypeCmmnEmailParameters ->
            betrokkene?.let { betrokkene ->
                extractBetrokkeneEmail(betrokkene)?.let { to ->
                    sendMail(zaaktypeCmmnEmailParameters, to, zaak)
                } ?: LOG.fine(
                    "No email address found for initiator '$betrokkene'. " +
                        "Skipping automatic email confirmation."
                )
            } ?: LOG.fine(
                "No initiator provided for zaak '$zaak'. " +
                    "Skipping automatic email confirmation."
            )
        }
    }

    private fun extractBetrokkeneEmail(betrokkene: Betrokkene) =
        betrokkene.performAction(
            onNatuurlijkPersoonIdentity = ::fetchEmailForNatuurlijkPersoon,
            onKvkIdentity = ::fetchEmail,
            onNoIdentity = { null }
        )

    private fun fetchEmailForNatuurlijkPersoon(identity: String): String? =
        klantClientService.findDigitalAddressesForPerson(identity)
            .firstOrNull { it.soortDigitaalAdres == SoortDigitaalAdresEnum.EMAIL }
            ?.adres

    private fun fetchEmail(kvkNummer: String, vestigingsNummer: String?): String? {
        val objectType = if (vestigingsNummer != null) {
            CodeObjecttypeEnum.VESTIGING
        } else {
            // KVK companies are always stored as non-natural persons in Open Klant
            CodeObjecttypeEnum.NIET_NATUURLIJK_PERSOON
        }
        return klantClientService.findDigitalAddresses(objectType, vestigingsNummer ?: kvkNummer)
            .firstOrNull { it.soortDigitaalAdres == SoortDigitaalAdresEnum.EMAIL }
            ?.adres
    }

    private fun sendMail(
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
                    "No email sender configured for zaaktype ${zaakFromProductaanvraag.zaaktype}. " +
                        "Skipping automatic email confirmation."
                )
            } ?: LOG.warning(
                "No mail template found with name: '${zaaktypeCmmnEmailParameters.templateName}'. " +
                    "Skipping automatic email confirmation."
            )
        } ?: LOG.warning(
            "No email template configured for zaaktype ${zaakFromProductaanvraag.zaaktype}. " +
                "Skipping automatic email confirmation."
        )
    }

    private fun configureEmail(
        zaaktypeCmmnEmailParameters: ZaaktypeCmmnEmailParameters,
        to: String,
        mailTemplate: MailTemplate
    ) = zaaktypeCmmnEmailParameters.emailSender?.let { emailSender ->
        MailGegevens(
            from = emailSender.generateMailAddress(configuratieService),
            to = MailAdres(email = to, name = null),
            replyTo = zaaktypeCmmnEmailParameters.emailReply?.generateMailAddress(configuratieService),
            subject = mailTemplate.onderwerp,
            body = mailTemplate.body,
            attachments = null,
            isCreateDocumentFromMail = true
        )
    }

    private fun String.generateMailAddress(configurationService: ConfiguratieService) =
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
