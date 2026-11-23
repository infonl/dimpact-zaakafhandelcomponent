/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent
import java.util.UUID

class ZaakAssigning(
    val zaakUuid: UUID,
    val groupId: String?,
    val behandelaar: Employee?
) : LogEvent("Assigning zaak")
