/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering

import jakarta.inject.Inject
import net.atos.zac.identity.IdentityService
import net.atos.zac.mail.model.MailAdres
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringTarget

class SignaleringenMailHelper @Inject constructor(
    private val identityService: IdentityService
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
}
