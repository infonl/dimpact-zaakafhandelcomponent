/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.documentcreation.model

import jakarta.validation.constraints.NotNull
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
class RestDocumentCreationAttendedData(
    @field:NotNull
    var zaakUUID: UUID,

    var taskId: String? = null
)
