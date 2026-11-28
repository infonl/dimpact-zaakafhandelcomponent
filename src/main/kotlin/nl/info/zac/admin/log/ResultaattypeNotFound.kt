/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ResultaattypeNotFound(
    val zaaktypeUuid: UUID,
    val resultaattypeOmschrijving: String
) : LogEvent("Zaaktype has no resultaattype with the omschrijving")
