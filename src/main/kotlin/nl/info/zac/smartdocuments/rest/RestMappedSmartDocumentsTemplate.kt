/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments.rest

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
