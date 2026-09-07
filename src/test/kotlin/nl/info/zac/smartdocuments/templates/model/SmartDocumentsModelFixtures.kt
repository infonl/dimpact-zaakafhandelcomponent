/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments.templates.model

import java.time.ZonedDateTime
import java.util.UUID

fun createSmartDocumentsTemplateGroup(
    id: String = UUID.randomUUID().toString(),
) = SmartDocumentsTemplateGroup().apply {
    smartDocumentsId = id
    creationDate = ZonedDateTime.now()
}

fun createSmartDocumentsTemplate(
    id: String = UUID.randomUUID().toString(),
    informatieObjectTypeUUID: UUID = UUID.randomUUID(),
) = SmartDocumentsTemplate().apply {
    smartDocumentsId = id
    this.informatieObjectTypeUUID = informatieObjectTypeUUID
}
