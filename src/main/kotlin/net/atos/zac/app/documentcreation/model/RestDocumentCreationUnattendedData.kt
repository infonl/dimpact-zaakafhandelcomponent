/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.documentcreation.model

import jakarta.validation.constraints.NotNull
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@NoArgConstructor
@Suppress("LongParameterList")
class RestDocumentCreationUnattendedData(
    /**
     * Not used yet but will be used in future when storing the document in the zaakregister.
     */
    var author: String? = null,

    /**
     * Not used yet but will be used in future when storing the document in the zaakregister.
     */
    var creationDate: LocalDate? = null,

    /**
     * Not used yet but will be used in future when storing the document in the zaakregister.
     */
    var documentTitle: String? = null,

    @field:NotNull
    var smartDocumentsTemplateGroupName: String,

    @field:NotNull
    var smartDdocumentsTemplateName: String,

    @field:NotNull
    var zaakUuid: UUID,

    var taskId: String? = null

)
