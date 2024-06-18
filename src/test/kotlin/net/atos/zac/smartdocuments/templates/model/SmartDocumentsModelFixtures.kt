/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.templates.model

import java.time.ZonedDateTime
import java.util.UUID

fun createSmartDocumentsTemplateGroup(
    id: String = UUID.randomUUID().toString(),
    name: String = "group"
) = SmartDocumentsTemplateGroup().apply {
    smartDocumentsId = id
    this.name = name
    creationDate = ZonedDateTime.now()
}

fun createSmartDocumentsTemplate(
    id: String = UUID.randomUUID().toString(),
    name: String = "template"
) = SmartDocumentsTemplate().apply {
    smartDocumentsId = id
    this.name = name
}
