/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.documentcreation.model

import jakarta.validation.constraints.NotNull
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

@NoArgConstructor
data class RestDocumentCreationAttendedData(
    @field:NotNull
    var zaakUuid: UUID,

    var taskId: String? = null,

    @field:NotNull
    var title: String,

    var description: String? = null,

    @field:NotNull
    var author: String,

    @field:NotNull
    var creationDate: ZonedDateTime,

    @field:NotNull
    var smartDocumentsTemplateGroupId: String,

    @field:NotNull
    var smartDocumentsTemplateId: String
)
