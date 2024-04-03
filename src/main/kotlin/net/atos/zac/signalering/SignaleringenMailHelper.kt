/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering

import jakarta.inject.Inject
import net.atos.zac.identity.IdentityService
import net.atos.zac.mail.model.MailAdres
import net.atos.zac.mailtemplates.MailTemplateService
import net.atos.zac.mailtemplates.model.Mail
import net.atos.zac.mailtemplates.model.MailTemplate
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringDetail
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType

class SignaleringenMailHelper @Inject constructor(
    private val identityService: IdentityService,
    private val mailTemplateService: MailTemplateService,
) {

    fun getTargetMail(signalering: Signalering?): SignaleringTarget.Mail? {
        var mail: SignaleringTarget.Mail? = null

        when (signalering!!.targettype!!) {
            SignaleringTarget.GROUP -> {
                val group = identityService.readGroup(signalering.target)
                if (group.email != null) {
                    mail = SignaleringTarget.Mail(group.name, group.email)
                }
            }

            SignaleringTarget.USER -> {
                val user = identityService.readUser(signalering.target)
                if (user.email != null) {
                    mail = SignaleringTarget.Mail(user.fullName, user.email)
                }
            }
        }
        return mail
    }

    fun formatTo(mail: SignaleringTarget.Mail): MailAdres {
        return MailAdres(mail.emailadres, mail.naam)
    }

    fun getMailTemplate(signalering: Signalering?): MailTemplate {
        return mailTemplateService.readMailtemplate(
            when (signalering!!.type.type!!) {
                SignaleringType.Type.TAAK_OP_NAAM -> Mail.SIGNALERING_TAAK_OP_NAAM
                SignaleringType.Type.TAAK_VERLOPEN -> Mail.SIGNALERING_TAAK_VERLOPEN
                SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD -> Mail.SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD
                SignaleringType.Type.ZAAK_OP_NAAM -> Mail.SIGNALERING_ZAAK_OP_NAAM
                SignaleringType.Type.ZAAK_VERLOPEND -> when (SignaleringDetail.valueOf(signalering.detail)) {
                    SignaleringDetail.STREEFDATUM -> Mail.SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM
                    SignaleringDetail.FATALE_DATUM -> Mail.SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM
                }
            }
        )
    }
}
