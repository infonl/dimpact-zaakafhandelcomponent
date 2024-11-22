/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail.model

import jakarta.json.bind.annotation.JsonbProperty

class EMail(
    @field:JsonbProperty("From") var from: MailAdres,
    @field:JsonbProperty("To") var to: List<MailAdres>,
    @field:JsonbProperty("ReplyTo") var replyTo: MailAdres?,
    @field:JsonbProperty("Subject") var subject: String,
    body: String,
    @field:JsonbProperty("Attachments") var attachments: List<Attachment>
) {
    @JsonbProperty("HTMLPart")
    var body: String = "<pre>$body</pre>"
}
