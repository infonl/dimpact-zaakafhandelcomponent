/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.rest

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestMappedSmartDocumentsTemplate(
    var id: String,
    var name: String,
    var informatieObjectTypeUUID: UUID,
)
