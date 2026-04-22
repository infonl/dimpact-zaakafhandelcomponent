/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.delegate

import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.delegate.AbstractDelegate
import net.atos.zac.flowable.delegate.resolveValueAsString
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mail.model.getBronnenFromZaak
import nl.info.zac.mailtemplates.model.MailGegevens
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

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
        private val LOG = Logger.getLogger(SendConfirmationEmailDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        val templateName = template.resolveValueAsString(execution)
        val fromAddress = from.resolveValueAsString(execution)
        // TODO
        // - Kijk of er zaak-specifieke contactgegevens zijn (klantService.findZaakSpecificContactDetails(zaak.uuid))
        // - zo ja, gebruik dat e-mail. Als dat er niet is, haal betrokkenen op en check of er een initiator bij zit
        //   en haal daar het e-mailadres uit. Zie ProductaanvraagEmailService
        // - zet na het succesvol versturen het vlaggetje dat deze is verstuurd
        //   met zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaakFromProductaanvraag)
        val toAddress = "klant-email"
        val replyToAddress = replyTo?.resolveValueAsString(execution)

        val mailTemplate = flowableHelper.mailTemplateService.findMailtemplateByName(templateName)
            ?: throw IllegalArgumentException("Mail template '$templateName' not found")

        LOG.fine(
            "Sending mail to '$toAddress' from '$fromAddress' for zaak ${zaak.identificatie}, using template '$templateName'"
        )
        flowableHelper.mailService.sendMail(
            mailGegevens = MailGegevens(
                from = MailAdres(fromAddress, null),
                to = MailAdres(toAddress, null),
                replyTo = replyToAddress?.let { MailAdres(replyToAddress, null) },
                subject = mailTemplate.onderwerp,
                body = mailTemplate.body,
                attachments = null,
                isCreateDocumentFromMail = true
            ),
            bronnen = zaak.getBronnenFromZaak()
        )
    }
}
