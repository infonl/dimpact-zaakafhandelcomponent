/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import java.time.LocalDate
import java.util.UUID

class RestDocumentVerzendGegevens {
    var zaakUuid: UUID? = null

    var verzenddatum: LocalDate? = null

    var informatieobjecten: List<UUID>? = null

    var toelichting: String? = null
}
