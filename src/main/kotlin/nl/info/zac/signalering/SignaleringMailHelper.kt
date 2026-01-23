/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering

import jakarta.inject.Inject
import net.atos.zac.mailtemplates.MailTemplateService
import net.atos.zac.mailtemplates.model.Mail
import net.atos.zac.mailtemplates.model.MailTemplate
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringDetail
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.mail.model.MailAdres

class SignaleringMailHelper @Inject constructor(
    private val identityService: IdentityService,
    private val mailTemplateService: MailTemplateService,
) {
    fun getTargetMail(signalering: Signalering): SignaleringTarget.Mail? =
        when (signalering.targettype) {
            SignaleringTarget.GROUP -> {
                identityService.readGroup(signalering.target).let { group ->
                    group.email?.let {
                        SignaleringTarget.Mail(group.name, it)
                    }
                }
            }
            SignaleringTarget.USER -> {
                identityService.readUser(signalering.target).let { user ->
                    user.email?.let {
                        SignaleringTarget.Mail(user.getFullName(), it)
                    }
                }
            }
            else -> null
        }

    fun getMailTemplate(signalering: Signalering): MailTemplate =
        mailTemplateService.readMailtemplate(
            when (signalering.type.type) {
                SignaleringType.Type.TAAK_OP_NAAM -> Mail.SIGNALERING_TAAK_OP_NAAM
                SignaleringType.Type.TAAK_VERLOPEN -> Mail.SIGNALERING_TAAK_VERLOPEN
                SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD -> Mail.SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD
                SignaleringType.Type.ZAAK_OP_NAAM -> Mail.SIGNALERING_ZAAK_OP_NAAM
                SignaleringType.Type.ZAAK_VERLOPEND -> when (SignaleringDetail.valueOf(signalering.detail)) {
                    SignaleringDetail.STREEFDATUM -> Mail.SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM
                    SignaleringDetail.FATALE_DATUM -> Mail.SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM
                }
                else -> null
            }
        )
}

fun formatTo(mail: SignaleringTarget.Mail): MailAdres = MailAdres(mail.emailadres, mail.naam)
