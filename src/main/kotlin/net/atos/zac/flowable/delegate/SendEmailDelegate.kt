/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.FlowableHelper
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mail.model.getBronnenFromZaak
import nl.info.zac.mailtemplates.model.MailGegevens
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

@Suppress("LongParameterList")
class SendEmailDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var from: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var to: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var replyTo: Expression? = null

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var template: Expression

    companion object {
        private val LOG: Logger = Logger.getLogger(SendEmailDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        val templateName = template.resolveValueAsString(execution)
        val fromAddress = from.resolveValueAsString(execution)
        val toAddress = to.resolveValueAsString(execution)
        val replyToAddress = replyTo?.resolveValueAsString(execution)

        val mailTemplate = flowableHelper.mailTemplateService.findMailtemplateByName(templateName)
            ?: throw IllegalArgumentException("Mail template '$templateName' not found")

        LOG.info(
            "Sending mail to '$toAddress' from '$fromAddress' for zaak " +
                "${zaak.identificatie}, using template '$templateName'"
        )
        flowableHelper.mailService.sendMail(
            mailGegevens = MailGegevens(
                from = MailAdres(fromAddress, null),
                to = MailAdres(toAddress, null),
                replyTo = replyToAddress?.let { MailAdres(replyToAddress, null) },
                subject = mailTemplate.onderwerp,
                body = mailTemplate.body,
                attachments = null,
                isCreateDocumentFromMail = false
            ),
            bronnen = zaak.getBronnenFromZaak()
        )
    }
}
