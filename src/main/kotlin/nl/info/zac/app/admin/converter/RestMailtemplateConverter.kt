/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import net.atos.zac.app.admin.model.RESTMailtemplate
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.util.stripHtmlParagraphTags

fun MailTemplate.toRestMailtemplate() =
    RESTMailtemplate().apply {
        id = this@toRestMailtemplate.id
        mailTemplateNaam = this@toRestMailtemplate.mailTemplateNaam
        mail = this@toRestMailtemplate.mail
        variabelen = this@toRestMailtemplate.mail.mailTemplateVariables
        onderwerp = this@toRestMailtemplate.onderwerp
        body = this@toRestMailtemplate.body
        defaultMailtemplate = this@toRestMailtemplate.isDefaultMailtemplate
    }

fun RESTMailtemplate.toMailTemplate() =
    toMailTemplateWithoutID().apply {
        id = this@toMailTemplate.id
    }

/**
 * Converts a [RESTMailtemplate] to a [MailTemplate] object for 'create' operations.
 * Explicitly does not set id. This is left up to the client or the database to set.
 */
fun RESTMailtemplate.toMailTemplateWithoutID() =
    MailTemplate().apply {
        mail = this@toMailTemplateWithoutID.mail
        mailTemplateNaam = this@toMailTemplateWithoutID.mailTemplateNaam.trim()
        onderwerp = stripHtmlParagraphTags(this@toMailTemplateWithoutID.onderwerp)
        body = this@toMailTemplateWithoutID.body
        isDefaultMailtemplate = this@toMailTemplateWithoutID.defaultMailtemplate
    }
