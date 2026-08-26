/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import nl.info.zac.app.admin.model.RestMailtemplate
import nl.info.zac.app.planitems.model.PlanItemType
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplateVariables

fun createRESTPlanItemDefinition(
    id: String = "fakeId",
    naam: String = "fakePlanItemNaam",
    type: PlanItemType = PlanItemType.HUMAN_TASK
) = RESTPlanItemDefinition(id, naam, type)

@Suppress("LongParameterList")
fun createRestMailTemplate(
    id: Long = 1234L,
    mailTemplateName: String = "fakeTemplateName",
    subject: String = "fakeSubject",
    body: String = "fakeBody",
    mail: Mail = Mail.ZAAK_ALGEMEEN,
    mailTemplateVariables: Set<MailTemplateVariables> = emptySet(),
    defaultTemplate: Boolean = false
) = RestMailtemplate(
    id = id,
    mailTemplateNaam = mailTemplateName,
    onderwerp = subject,
    body = body,
    mail = mail,
    variabelen = mailTemplateVariables,
    isDefaultMailtemplate = defaultTemplate
)
