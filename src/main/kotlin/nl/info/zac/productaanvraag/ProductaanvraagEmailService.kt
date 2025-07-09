/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.klant.KlantClientService
import net.atos.client.klant.model.SoortDigitaalAdresEnum
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.mailtemplates.MailTemplateService
import net.atos.zac.mailtemplates.model.MailGegevens
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mail.model.getBronnenFromZaak
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
    private val zaakService: ZaakService,
    private val mailService: MailService,
    private val mailTemplateService: MailTemplateService,
) {
    companion object {
        private val LOG = Logger.getLogger(ProductaanvraagEmailService::class.java.name)
    }

    @Suppress("ReturnCount")
    fun sendEmailForZaakFromProductaanvraag(
        zaakFromProductaanvraag: Zaak,
        betrokkene: Betrokkene?,
        zaakafhandelParameters: ZaakafhandelParameters
    ) {
        zaakafhandelParameters.automaticEmailConfirmation?.takeIf { it.enabled }?.let { automaticEmailConfirmation ->
            if (betrokkene == null) {
                LOG.fine(
                    "No initiator provided for zaak '$zaakFromProductaanvraag'. " +
                        "Skipping automatic email confirmation."
                )
                return
            }
            val to = extractInitiatorEmail(betrokkene)
            if (to == null) {
                LOG.fine(
                    "No email address found for initiator '$betrokkene'. " +
                        "Skipping automatic email confirmation."
                )
                return
            }

            val mailTemplate = mailTemplateService
                .findMailtemplateByName(automaticEmailConfirmation.templateName)
                .orElse(null)
            if (mailTemplate == null) {
                LOG.warning(
                    "No mail template found with name: '${automaticEmailConfirmation.templateName}'. " +
                        "Skipping automatic email confirmation."
                )
                return
            }

            val mailGegevens = MailGegevens(
                MailAdres(automaticEmailConfirmation.emailSender, null),
                MailAdres(to, null),
                MailAdres(automaticEmailConfirmation.emailReply, null),
                mailTemplate.onderwerp,
                mailTemplate.body,
                null,
                true
            )
            mailService.sendMail(mailGegevens, zaakFromProductaanvraag.getBronnenFromZaak())
            zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaakFromProductaanvraag)
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
}
