/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.delegate

import net.atos.zac.flowable.delegate.AbstractDelegate
import net.atos.zac.flowable.delegate.resolveValueAsString
import nl.info.client.klant.model.toContactDetails
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.zac.flowable.FlowableHelper
import nl.info.zac.flowable.bpmn.delegate.log.ConfirmationMailSending
import nl.info.zac.flowable.bpmn.delegate.log.EmailSource
import nl.info.zac.identification.IdentificatieType
import nl.info.zac.identification.toIdentificatieType
import nl.info.zac.log.ZacLogger
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mail.model.getBronnenFromZaak
import nl.info.zac.mailtemplates.model.MailGegevens
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution

/**
 * Flowable BPMN delegate to send a confirmation e-mail.
 *
 * This class may be used in existing BPMN process definitions, so be careful renaming or moving it to another package
 * because that will break all zaken and tasks that were created with (previous versions of) the related BPMN process.
 */
@Suppress("LongParameterList")
class SendConfirmationEmailDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var from: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var replyTo: Expression? = null

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var template: Expression

    companion object {
        private val LOG = ZacLogger.getLogger(SendConfirmationEmailDelegate::class)
        val flowableHelper = FlowableHelper.getInstance()
    }

    override fun execute(execution: DelegateExecution) {
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        val templateName = template.resolveValueAsString(execution)
        val fromAddress = from.resolveValueAsString(execution)
        val replyToAddress = replyTo?.resolveValueAsString(execution)

        val (emailSource, toAddress) = flowableHelper.klantClientService
            .findZaakSpecificContactDetails(zaak.uuid)
            ?.emailAddress
            ?.let { EmailSource.ZAAK_CONTACT_DETAILS to it }
            ?: flowableHelper.zgwApiService.findInitiatorRoleForZaak(zaak)
                ?.let { findEmailForInitiatorRole(it) }
                ?.let { EmailSource.INITIATOR to it }
            ?: (EmailSource.NONE to null)

        if (toAddress == null) {
            logConfirmationMailSending(
                zaakIdentificatie = zaak.identificatie,
                templateName = templateName,
                emailSource = emailSource,
                recipient = null
            )
            return
        }

        val mailTemplate = flowableHelper.mailTemplateService.findMailtemplateByName(templateName)
            ?: throw IllegalArgumentException("Mail template '$templateName' not found")

        logConfirmationMailSending(
            zaakIdentificatie = zaak.identificatie,
            templateName = templateName,
            emailSource = emailSource,
            recipient = toAddress
        )
        flowableHelper.mailService.sendMail(
            mailGegevens = MailGegevens(
                from = MailAdres(fromAddress, null),
                to = MailAdres(toAddress, null),
                replyTo = replyToAddress?.let { MailAdres(replyToAddress, null) },
                subject = mailTemplate.onderwerp,
                body = mailTemplate.body,
                attachments = null,
                isCreateDocumentFromMail = true,
                vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR
            ),
            bronnen = zaak.getBronnenFromZaak()
        )
    }

    private fun logConfirmationMailSending(
        zaakIdentificatie: String?,
        templateName: String,
        emailSource: EmailSource,
        recipient: String?
    ) = LOG.fine(
        ConfirmationMailSending(
            zaakIdentificatie = zaakIdentificatie,
            templateName = templateName,
            emailSource = emailSource,
            recipient = recipient
        )
    )

    private fun findEmailForInitiatorRole(initiatorRole: Rol<*>): String? {
        val identificatie = initiatorRole.identificatienummer ?: return null
        return when (initiatorRole.betrokkeneType) {
            BetrokkeneTypeEnum.NATUURLIJK_PERSOON ->
                flowableHelper.klantClientService.findDigitalAddressesForNaturalPerson(identificatie)

            BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON -> {
                val nietNatuurlijkPersoonIdentificatie =
                    initiatorRole.betrokkeneIdentificatie as NietNatuurlijkPersoonIdentificatie
                when (nietNatuurlijkPersoonIdentificatie.toIdentificatieType()) {
                    IdentificatieType.RSIN ->
                        flowableHelper.klantClientService.findDigitalAddressesForNonNaturalPerson(identificatie)
                    IdentificatieType.VN -> nietNatuurlijkPersoonIdentificatie.kvkNummer
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            flowableHelper.klantClientService.findDigitalAddressesForVestiging(
                                nietNatuurlijkPersoonIdentificatie.vestigingsNummer,
                                it
                            )
                        }.orEmpty()
                    else -> emptyList()
                }
            }

            else -> emptyList()
        }.toContactDetails().emailAddress
    }
}
