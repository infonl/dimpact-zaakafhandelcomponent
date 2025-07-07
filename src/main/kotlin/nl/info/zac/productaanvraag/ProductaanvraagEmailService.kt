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
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mail.model.getBronnenFromZaak
import nl.info.zac.productaanvraag.exception.EmailAddressNotFoundException
import nl.info.zac.productaanvraag.exception.InitiatorNotFoundException
import nl.info.zac.productaanvraag.exception.MailTemplateNotFoundException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ProductaanvraagEmailService @Inject constructor(
    private val zgwApiService: ZGWApiService,
    private val klantClientService: KlantClientService,
    private val zaakService: ZaakService,
    private val mailService: MailService,
    private val mailTemplateService: MailTemplateService,
) {
    fun sendEmailForProductaanvraag(
        zaak: Zaak,
        zaakafhandelParameters: ZaakafhandelParameters
    ) {
        val automaticEmailConfirmation = zaakafhandelParameters.automaticEmailConfirmation
        zaakafhandelParameters.automaticEmailConfirmation.let {
            val to = extractInitiatorEmail(zaak)
            val mailTemplate = mailTemplateService
                .findMailtemplateByName(automaticEmailConfirmation.templateName)
                .orElseThrow {
                    MailTemplateNotFoundException("No mail template found with name: " +
                            automaticEmailConfirmation.templateName
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
            mailService.sendMail(mailGegevens, zaak.getBronnenFromZaak())
            zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)
        }
    }

    private fun extractInitiatorEmail(createdZaak: Zaak): String {
        val identificatie = zgwApiService.findInitiatorRoleForZaak(createdZaak)?.betrokkeneIdentificatie
            ?: throw InitiatorNotFoundException("No initiator or identification found for the zaak: " +
                    createdZaak.uuid)
        return when(identificatie) {
            is MedewerkerIdentificatie -> extractEmail(identificatie.identificatie)
            is NatuurlijkPersoonIdentificatie -> extractEmail(identificatie.anpIdentificatie)
            is NietNatuurlijkPersoonIdentificatie -> extractEmail(identificatie.annIdentificatie)
            is OrganisatorischeEenheidIdentificatie -> extractEmail(identificatie.identificatie)
            else ->
                throw InitiatorNotFoundException("Unknown initiator type attached to the zaak-'${createdZaak.uuid}'.")
        }
    }

    private fun extractEmail(identification: String): String {
        return klantClientService.findDigitalAddressesByNumber(identification)
            .firstOrNull { it.soortDigitaalAdres == SoortDigitaalAdresEnum.EMAIL }
            ?.adres
            ?: throw EmailAddressNotFoundException("No e-mail address found for identificatie: $identification")
    }
}
