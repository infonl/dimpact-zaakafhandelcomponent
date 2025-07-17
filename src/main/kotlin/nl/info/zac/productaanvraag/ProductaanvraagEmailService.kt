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
        zaakFromProductaanvraag: Zaak,
        betrokkene: Betrokkene?,
        zaakafhandelParameters: ZaakafhandelParameters
    ) {
        zaakafhandelParameters.automaticEmailConfirmation?.takeIf { it.enabled }?.let { automaticEmailConfirmation ->
            betrokkene?.let { initiator ->
                extractInitiatorEmail(initiator)?.let { to ->
                    sendMail(automaticEmailConfirmation, to, zaakFromProductaanvraag)
                } ?: LOG.fine(
                    "No email address found for initiator '$initiator'. " +
                        "Skipping automatic email confirmation."
                )
            } ?: LOG.fine(
                "No initiator provided for zaak '$zaakFromProductaanvraag'. " +
                    "Skipping automatic email confirmation."
            )
        }
    }

    private fun extractInitiatorEmail(betrokkene: Betrokkene) =
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
        mailTemplateService.findMailtemplateByName(automaticEmailConfirmation.templateName)?.let {
                mailTemplate ->
            val mailGegevens = MailGegevens(
                from = MailAdres(
                    email = automaticEmailConfirmation.emailSender.extractEmailAddress(configuratieService),
                    name = null
                ),
                to = MailAdres(email = to, name = null),
                replyTo = automaticEmailConfirmation.emailReply?.let {
                    MailAdres(
                        email = it.extractEmailAddress(configuratieService),
                        name = null
                    )
                },
                subject = mailTemplate.onderwerp,
                body = mailTemplate.body,
                attachments = null,
                isCreateDocumentFromMail = true
            )
            mailService.sendMail(mailGegevens, zaakFromProductaanvraag.getBronnenFromZaak())?.also {
                zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaakFromProductaanvraag)
            }
        } ?: LOG.warning(
            "No mail template found with name: '${automaticEmailConfirmation.templateName}'. " +
                "Skipping automatic email confirmation."
        )
    }

    private fun String.extractEmailAddress(configurationService: ConfiguratieService): String =
        when (this) {
            ZaakAfzender.Speciaal.GEMEENTE.toString() -> configurationService.readGemeenteMail()
            else -> this
        }
}
