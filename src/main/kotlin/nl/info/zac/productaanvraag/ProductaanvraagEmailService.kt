/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.klant.KlantClientService
import net.atos.client.klant.model.SoortDigitaalAdresEnum
import net.atos.zac.admin.model.AutomaticEmailConfirmation
import net.atos.zac.admin.model.ZaakAfzender
import net.atos.zac.admin.model.ZaakafhandelParameters
import nl.info.client.zgw.zrc.model.generated.Zaak
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
        zaakafhandelParameters: ZaakafhandelParameters
    ) {
        zaakafhandelParameters.automaticEmailConfirmation?.takeIf { it.enabled }?.let { automaticEmailConfirmation ->
            betrokkene?.let { betrokkene ->
                extractBetrokkeneEmail(betrokkene)?.let { to ->
                    sendMail(automaticEmailConfirmation, to, zaak)
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
            onNatuurlijkPersoonIdentity = ::fetchEmail,
            onVestigingIdentity = ::fetchEmail,
            onNoIdentity = { null }
        )

    private fun fetchEmail(identity: String): String? =
        klantClientService.findDigitalAddressesByNumber(identity)
            .firstOrNull { it.soortDigitaalAdres == SoortDigitaalAdresEnum.EMAIL }
            ?.adres

    private fun sendMail(
        automaticEmailConfirmation: AutomaticEmailConfirmation,
        to: String,
        zaakFromProductaanvraag: Zaak
    ) {
        automaticEmailConfirmation.templateName?.let { templateName ->
            mailTemplateService.findMailtemplateByName(templateName)?.let { mailTemplate ->
                configureEmail(automaticEmailConfirmation, to, mailTemplate)?.let { mailGegevens ->
                    mailService.sendMail(mailGegevens, zaakFromProductaanvraag.getBronnenFromZaak())?.also {
                        zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaakFromProductaanvraag)
                    }
                } ?: LOG.warning(
                    "No email sender configured for zaaktype ${zaakFromProductaanvraag.zaaktype}. " +
                        "Skipping automatic email confirmation."
                )
            } ?: LOG.warning(
                "No mail template found with name: '${automaticEmailConfirmation.templateName}'. " +
                    "Skipping automatic email confirmation."
            )
        } ?: LOG.warning(
            "No email template configured for zaaktype ${zaakFromProductaanvraag.zaaktype}. " +
                "Skipping automatic email confirmation."
        )
    }

    private fun configureEmail(
        automaticEmailConfirmation: AutomaticEmailConfirmation,
        to: String,
        mailTemplate: MailTemplate
    ) = automaticEmailConfirmation.emailSender?.let { emailSender ->
        MailGegevens(
            from = emailSender.generateMailAddress(configuratieService),
            to = MailAdres(email = to, name = null),
            replyTo = automaticEmailConfirmation.emailReply?.generateMailAddress(configuratieService),
            subject = mailTemplate.onderwerp,
            body = mailTemplate.body,
            attachments = null,
            isCreateDocumentFromMail = true
        )
    }

    private fun String.generateMailAddress(configurationService: ConfiguratieService) =
        when (this) {
            ZaakAfzender.SpecialMail.GEMEENTE.toString() -> MailAdres(
                email = configurationService.readGemeenteMail(),
                name = configurationService.readGemeenteNaam()
            )
            else -> MailAdres(
                email = this,
                name = null
            )
        }
}
