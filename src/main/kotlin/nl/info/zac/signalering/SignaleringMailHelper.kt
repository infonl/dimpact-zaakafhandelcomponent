/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering

import jakarta.inject.Inject
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.signalering.model.Signalering
import nl.info.zac.signalering.model.SignaleringDetail
import nl.info.zac.signalering.model.SignaleringTarget
import nl.info.zac.signalering.model.SignaleringType

class SignaleringMailHelper @Inject constructor(
    private val identityService: IdentityService,
    private val mailTemplateService: MailTemplateService,
) {
    fun getTargetMail(signalering: Signalering): SignaleringTarget.Mail? =
        when (signalering.targettype) {
            SignaleringTarget.GROUP -> {
                identityService.readGroup(
                    groupId = signalering.target,
                    logContext = "signalering '${signalering.id}'"
                ).let { group ->
                    group.email?.let {
                        SignaleringTarget.Mail(group.description, it)
                    }
                }
            }
            SignaleringTarget.USER -> {
                identityService.readUser(
                    username = signalering.target,
                    logContext = "signalering '${signalering.id}'"
                ).let { user ->
                    user.email?.let {
                        SignaleringTarget.Mail(user.getFullName(), it)
                    }
                }
            }
        }

    fun getMailTemplate(signalering: Signalering): MailTemplate =
        mailTemplateService.readDefaultMailTemplate(
            when (signalering.type.type) {
                SignaleringType.Type.TAAK_OP_NAAM -> Mail.SIGNALERING_TAAK_OP_NAAM
                SignaleringType.Type.TAAK_VERLOPEN -> Mail.SIGNALERING_TAAK_VERLOPEN
                SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD -> Mail.SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD
                SignaleringType.Type.ZAAK_OP_NAAM -> Mail.SIGNALERING_ZAAK_OP_NAAM
                SignaleringType.Type.ZAAK_VERLOPEND -> when (
                    SignaleringDetail.valueOf(checkNotNull(signalering.detail) { "Signalering $signalering has no detail" })
                ) {
                    SignaleringDetail.STREEFDATUM -> Mail.SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM
                    SignaleringDetail.FATALE_DATUM -> Mail.SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM
                }
            }
        )
}

fun formatTo(mail: SignaleringTarget.Mail): MailAdres = MailAdres(mail.emailadres, mail.naam)
