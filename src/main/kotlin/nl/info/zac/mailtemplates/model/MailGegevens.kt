/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates.model

import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mailtemplates.stripParagraphTags

@Suppress("LongParameterList")
class MailGegevens(
    val from: MailAdres,
    val to: MailAdres,
    val replyTo: MailAdres?,
    subject: String,
    val body: String,
    attachments: String?,
    val isCreateDocumentFromMail: Boolean
) {
    val subject: String = stripParagraphTags(subject)

    val attachments: List<String> = attachments?.split(";") ?: emptyList()

    constructor(
        from: MailAdres,
        to: MailAdres,
        subject: String,
        body: String
    ) : this(from, to, null, subject, body, null, false)
}
