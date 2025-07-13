/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import net.atos.zac.mailtemplates.model.Mail
import net.atos.zac.mailtemplates.model.MailTemplateVariabelen

fun createRestMailTemplate(
    id: Long = 1234L,
    mailTemplateName: String = "fakeTemplateName",
    subject: String = "fakeSubject",
    body: String = "fakeBody",
    mail: String = Mail.ZAAK_ALGEMEEN.name,
    mailTemplateVariables: Set<MailTemplateVariabelen> = emptySet(),
    defaultTemplate: Boolean = false
) = RESTMailtemplate().apply {
    this.id = id
    this.mailTemplateNaam = mailTemplateName
    this.onderwerp = subject
    this.body = body
    this.mail = mail
    this.variabelen = mailTemplateVariables
    this.defaultMailtemplate = defaultTemplate
}
