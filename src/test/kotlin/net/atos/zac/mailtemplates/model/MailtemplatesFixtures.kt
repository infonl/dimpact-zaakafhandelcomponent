/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mailtemplates.model

import net.atos.zac.mail.model.MailAdres
import net.atos.zac.mail.model.createMailAdres

@Suppress("LongParameterList")
fun createMailGegevens(
    from: MailAdres = createMailAdres(),
    to: MailAdres = createMailAdres(),
    replyTo: MailAdres = createMailAdres(),
    subject: String? = "dummySubject",
    body: String = "dummyBody",
    attachments: String? = null,
    createDocumentFromMail: Boolean = false
) = MailGegevens(
    from,
    to,
    replyTo,
    subject,
    body,
    attachments,
    createDocumentFromMail
)
