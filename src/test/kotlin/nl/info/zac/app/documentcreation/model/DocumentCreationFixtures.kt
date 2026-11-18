/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.documentcreation.model

import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createRestDocumentCreationAttendedData(
    zaakUuid: UUID = UUID.randomUUID(),
    taskId: String? = null,
    smartDocumentsTemplateGroupId: String = "fakeGroupId",
    smartDocumentsTemplateId: String = "fakeTtemplateId",
    title: String = "fakeTitle",
    author: String = "fakeAuthor",
    creationDate: LocalDate = LocalDate.of(2026, 1, 30)
) = RestDocumentCreationAttendedData(
    zaakUuid = zaakUuid,
    taskId = taskId,
    smartDocumentsTemplateGroupId = smartDocumentsTemplateGroupId,
    smartDocumentsTemplateId = smartDocumentsTemplateId,
    title = title,
    author = author,
    creationDate = creationDate,
)
