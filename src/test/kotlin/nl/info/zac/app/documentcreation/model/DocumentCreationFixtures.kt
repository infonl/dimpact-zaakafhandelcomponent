/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.documentcreation.model

import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun createRestDocumentCreationAttendedData(
    zaakUuid: UUID = UUID.randomUUID(),
    taskId: String? = null,
    smartDocumentsTemplateGroupId: String = "fakeGroupId",
    smartDocumentsTemplateId: String = "fakeTtemplateId",
    smartDocumentsTemplateGroupName: String? = null,
    smartDocumentsTemplateName: String? = null,
    informatieobjecttypeUuid: UUID? = null,
    title: String = "fakeTitle",
    author: String = "fakeAuthor",
    creationDate: ZonedDateTime = ZonedDateTime.now()
) = RestDocumentCreationAttendedData(
    zaakUuid = zaakUuid,
    taskId = taskId,
    smartDocumentsTemplateGroupId = smartDocumentsTemplateGroupId,
    smartDocumentsTemplateId = smartDocumentsTemplateId,
    title = title,
    author = author,
    creationDate = creationDate,
    informatieobjecttypeUuid = informatieobjecttypeUuid,
    smartDocumentsTemplateName = smartDocumentsTemplateName,
    smartDocumentsTemplateGroupName = smartDocumentsTemplateGroupName
)
