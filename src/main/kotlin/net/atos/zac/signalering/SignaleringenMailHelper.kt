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


class SignaleringenMailHelper {
    @Inject
    private val identityService: IdentityService? = null

    fun getTargetMail(signalering: Signalering?): SignaleringTarget.Mail? {
        when (signalering!!.targettype) {
            SignaleringTarget.GROUP -> {
                val group = identityService!!.readGroup(signalering.target)
                if (group.email != null) {
                    return SignaleringTarget.Mail(group.name, group.email)
                }
            }

            SignaleringTarget.USER -> {
                val user = identityService!!.readUser(signalering.target)
                if (user.email != null) {
                    return SignaleringTarget.Mail(user.fullName, user.email)
                }
            }
        }
        return null
    }

    fun formatTo(mail: SignaleringTarget.Mail): MailAdres {
        return MailAdres(mail.emailadres, mail.naam)
    }
}
