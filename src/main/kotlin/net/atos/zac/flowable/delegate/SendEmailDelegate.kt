/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.mailtemplates.model.MailGegevens
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mail.model.getBronnenFromZaak
import org.flowable.engine.delegate.DelegateExecution
import org.flowable.engine.impl.el.FixedValue
import java.util.logging.Logger

@Suppress("LongParameterList")
class SendEmailDelegate : AbstractDelegate() {
    // set by Flowable
    private lateinit var from: FixedValue

    // set by Flowable
    private lateinit var to: FixedValue

    // set by Flowable
    private lateinit var replyTo: FixedValue

    // set by Flowable
    private lateinit var template: FixedValue

    companion object {
        private val LOG: Logger = Logger.getLogger(SendEmailDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        val mailTemplate = flowableHelper.mailTemplateService.listMailtemplates().find {
            it.mailTemplateNaam == template.expressionText
        }
        require(mailTemplate != null) {
            "Mail template '${template.expressionText}' not found"
        }

        LOG.info(
            "Sending mail to '${to.expressionText}' from '${from.expressionText}' for zaak " +
                "${zaak.identificatie}, using template '${mailTemplate.mailTemplateNaam}'"
        )
        flowableHelper.mailService.sendMail(
            mailGegevens = MailGegevens(
                MailAdres(from.expressionText, null),
                MailAdres(to.expressionText, null),
                MailAdres(replyTo.expressionText, null),
                mailTemplate.onderwerp,
                mailTemplate.body,
                null,
                false
            ),
            bronnen = zaak.getBronnenFromZaak()
        )
    }
}
