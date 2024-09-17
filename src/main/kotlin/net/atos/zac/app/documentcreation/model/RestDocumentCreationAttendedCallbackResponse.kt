/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.documentcreation.model

import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
data class RestDocumentCreationAttendedCallbackResponse(
    val zaakUuid: UUID,
    val zaakInformatieobjectUuid: UUID,
    val taskId: String? = null
)
