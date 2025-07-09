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
import nl.info.zac.productaanvraag.exception.EmailAddressNotFoundException
import nl.info.zac.productaanvraag.exception.InitiatorNotFoundException
import nl.info.zac.productaanvraag.exception.MailTemplateNotFoundException
import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ProductaanvraagEmailService @Inject constructor(
    private val klantClientService: KlantClientService,
    private val zaakService: ZaakService,
    private val mailService: MailService,
    private val mailTemplateService: MailTemplateService,
) {
    fun sendEmailForZaakFromProductaanvraag(
        zaakFromProductaanvraag: Zaak,
        betrokkene: Betrokkene?,
        zaakafhandelParameters: ZaakafhandelParameters
    ) {
        if (betrokkene == null) { throw InitiatorNotFoundException("No initiator provided") }
        zaakafhandelParameters.automaticEmailConfirmation?.takeIf { it.enabled }?.let { automaticEmailConfirmation ->
            val to = extractInitiatorEmail(betrokkene)
            val mailTemplate = mailTemplateService
                .findMailtemplateByName(automaticEmailConfirmation.templateName)
                .orElseThrow {
                    MailTemplateNotFoundException(
                        "No mail template found with name: '${automaticEmailConfirmation.templateName}'"
                    )
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

    @Suppress("ThrowsCount")
    private fun extractInitiatorEmail(betrokkene: Betrokkene): String {
        val identification = when {
            betrokkene.inpBsn != null -> betrokkene.inpBsn
            betrokkene.vestigingsNummer?.isNotBlank() == true -> betrokkene.vestigingsNummer
            else -> throw InitiatorNotFoundException(
                "Cannot find initiator identification for betrokkene: '$betrokkene'."
            )
        }
        return klantClientService.findDigitalAddressesByNumber(identification)
            .firstOrNull { it.soortDigitaalAdres == SoortDigitaalAdresEnum.EMAIL }
            ?.adres
            ?: throw EmailAddressNotFoundException(
                "No e-mail address found for identification number: '$identification'"
            )
    }
}
