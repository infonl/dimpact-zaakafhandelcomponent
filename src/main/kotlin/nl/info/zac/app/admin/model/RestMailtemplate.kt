/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.mailtemplates.model.MailTemplateVariables
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.stripHtmlParagraphTags

@NoArgConstructor
@AllOpen
data class RestMailtemplate(
    // ID is optional for POST requests (will be ignored) and required for responses
    var id: Long? = null,

    @field:NotBlank(message = "Mail template name is required")
    var mailTemplateNaam: String,

    @field:NotBlank(message = "Subject is required")
    var onderwerp: String,

    @field:NotBlank(message = "Body is required")
    var body: String,

    @field:NotNull(message = "Mail type is required")
    var mail: Mail,

    var variabelen: Set<MailTemplateVariables> = emptySet(),

    @get:JsonbProperty("defaultMailtemplate")
    var isDefaultMailtemplate: Boolean = false
)

fun MailTemplate.toRestMailtemplate() = RestMailtemplate(
    id = id,
    mailTemplateNaam = mailTemplateNaam,
    onderwerp = onderwerp,
    body = body,
    mail = mail,
    variabelen = mail.mailTemplateVariables,
    isDefaultMailtemplate = isDefaultMailtemplate
)

fun RestMailtemplate.toMailTemplate() =
    MailTemplate().apply {
        // id is copied only when present:
        // it only matters to callers that need to reference an already-persisted MailTemplate by id, e.g. to
        // populate a JPA @ManyToOne relationship.
        this@toMailTemplate.id?.let { id = it }
        mail = this@toMailTemplate.mail
        mailTemplateNaam = this@toMailTemplate.mailTemplateNaam.trim()
        onderwerp = stripHtmlParagraphTags(this@toMailTemplate.onderwerp)
        body = this@toMailTemplate.body
        isDefaultMailtemplate = this@toMailTemplate.isDefaultMailtemplate
    }
