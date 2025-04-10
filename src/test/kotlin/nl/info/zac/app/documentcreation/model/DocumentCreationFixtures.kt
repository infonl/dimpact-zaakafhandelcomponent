/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.documentcreation.model

import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun createRestDocumentCreationAttendedData(
    zaakUuid: UUID = UUID.randomUUID(),
    taskId: String? = null,
    smartDocumentsTemplateGroupId: String = "dummyGroupId",
    smartDocumentsTemplateId: String = "dummyTtemplateId",
    title: String = "dummyTitle",
    author: String = "dummyAuthor",
    creationDate: ZonedDateTime = ZonedDateTime.now()
) = RestDocumentCreationAttendedData(
    zaakUuid = zaakUuid,
    taskId = taskId,
    smartDocumentsTemplateGroupId = smartDocumentsTemplateGroupId,
    smartDocumentsTemplateId = smartDocumentsTemplateId,
    title = title,
    author = author,
    creationDate = creationDate
)
