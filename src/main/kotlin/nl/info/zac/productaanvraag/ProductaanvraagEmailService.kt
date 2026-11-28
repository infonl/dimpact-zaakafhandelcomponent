/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.klant.KlantClientService
import nl.info.client.klant.model.getPreferredDigitaalAdres
import nl.info.client.klanten.model.generated.SoortDigitaalAdresEnum
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.ZaaktypeEmailParameters
import nl.info.zac.admin.model.ZaaktypeZaakafzenderParameters
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.log.ZacLogger
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mail.model.getBronnenFromZaak
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.MailGegevens
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.productaanvraag.log.ConfirmationOfReceiptEmailSkipped
import nl.info.zac.productaanvraag.log.ConfirmationOfReceiptSkipReason
import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.productaanvraag.util.performAction
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService

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
        private val LOG = ZacLogger.getLogger(ProductaanvraagEmailService::class)
    }

    fun sendConfirmationOfReceiptEmailFromProductaanvraag(
        zaak: Zaak,
        betrokkene: Betrokkene?,
        productaanvraagSpecificEmailAddress: String?,
        zaaktypeConfiguration: ZaaktypeConfiguration
    ) {
        zaaktypeConfiguration.zaaktypeEmailParameters?.takeIf { it.isEnabled }?.let { zaaktypeEmailParameters ->
            productaanvraagSpecificEmailAddress?.let { to ->
                sendConfirmationOfReceiptMail(zaaktypeEmailParameters, to, zaak)
            } ?: betrokkene?.let {
                extractBetrokkeneEmail(it)?.let { to ->
                    sendConfirmationOfReceiptMail(zaaktypeEmailParameters, to, zaak)
                }
            } ?: LOG.fine (
                ConfirmationOfReceiptEmailSkipped(
                zaakIdentificatie = zaak.identificatie,
                zaaktypeUri = zaak.zaaktype,
                skipReason = ConfirmationOfReceiptSkipReason.NO_EMAIL_ADDRESS
            )
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
        klantClientService.findDigitalAddressesForNaturalPerson(identity)
            .filter { it.soortDigitaalAdres == SoortDigitaalAdresEnum.EMAIL }
            .getPreferredDigitaalAdres()
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
            .getPreferredDigitaalAdres()
            ?.adres
    }

    private fun sendConfirmationOfReceiptMail(
        zaaktypeEmailParameters: ZaaktypeEmailParameters,
        to: String,
        zaakFromProductaanvraag: Zaak
    ) {
        zaaktypeEmailParameters.templateName?.let { templateName ->
            mailTemplateService.findMailtemplateByName(templateName)?.let { mailTemplate ->
                configureEmail(zaaktypeEmailParameters, to, mailTemplate)?.let { mailGegevens ->
                    mailService.sendMail(mailGegevens, zaakFromProductaanvraag.getBronnenFromZaak())?.also {
                        zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaakFromProductaanvraag)
                    }
                } ?: LOG.warning(
                    ConfirmationOfReceiptEmailSkipped(
                zaakIdentificatie = zaakFromProductaanvraag.identificatie,
                zaaktypeUri = zaakFromProductaanvraag.zaaktype,
                skipReason = ConfirmationOfReceiptSkipReason.NO_SENDER_CONFIGURED
            )
                )
            } ?: LOG.warning(
                ConfirmationOfReceiptEmailSkipped(
                zaakIdentificatie = zaakFromProductaanvraag.identificatie,
                zaaktypeUri = zaakFromProductaanvraag.zaaktype,
                skipReason = ConfirmationOfReceiptSkipReason.TEMPLATE_NOT_FOUND,
                templateName = templateName
            )
            )
        } ?: LOG.warning(
            ConfirmationOfReceiptEmailSkipped(
                zaakIdentificatie = zaakFromProductaanvraag.identificatie,
                zaaktypeUri = zaakFromProductaanvraag.zaaktype,
                skipReason = ConfirmationOfReceiptSkipReason.NO_TEMPLATE_CONFIGURED
            )
        )
    }

    private fun configureEmail(
        zaaktypeEmailParameters: ZaaktypeEmailParameters,
        to: String,
        mailTemplate: MailTemplate
    ) = zaaktypeEmailParameters.emailSender?.let { emailSender ->
        MailGegevens(
            from = emailSender.generateMailAddress(configurationService),
            to = MailAdres(email = to, name = null),
            replyTo = zaaktypeEmailParameters.emailReply?.generateMailAddress(configurationService),
            subject = mailTemplate.onderwerp,
            body = mailTemplate.body,
            attachments = null,
            isCreateDocumentFromMail = true,
            vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR
        )
    }

    private fun String.generateMailAddress(configurationService: ConfigurationService) =
        when (this) {
            ZaaktypeZaakafzenderParameters.SpecialMail.GEMEENTE.toString() -> MailAdres(
                email = configurationService.readGemeenteMail(),
                name = configurationService.readGemeenteNaam()
            )
            else -> MailAdres(
                email = this,
                name = null
            )
        }
}
