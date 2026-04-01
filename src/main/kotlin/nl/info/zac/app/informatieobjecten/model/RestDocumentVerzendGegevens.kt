/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestDocumentVerzendGegevens(
    var zaakUuid: UUID,

    var verzenddatum: LocalDate,

    var informatieobjecten: List<UUID>,

    var toelichting: String? = null
)
