/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.documentcreation.model

import jakarta.validation.constraints.NotNull
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
class RestDocumentCreationUnattendedData(
    var documentTitel: String? = null,

    @field:NotNull
    var documentTemplateGroupUuid: UUID,

    @field:NotNull
    var documentTemplateUuid: UUID,

    @field:NotNull
    var zaakUuid: UUID,

    var taskId: String? = null

)
