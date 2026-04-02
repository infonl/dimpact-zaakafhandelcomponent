/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestInformatieobjectZoekParameters(
    var informatieobjectUUIDs: List<UUID>? = null,
    var zaakUUID: UUID? = null,
    var besluittypeUUID: UUID? = null,
    var gekoppeldeZaakDocumenten: Boolean = false
)
