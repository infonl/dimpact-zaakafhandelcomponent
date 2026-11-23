/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.log

import nl.info.zac.log.LogEvent

class ZakenAssigned(
    val zaakCount: Int,
    val assignedCount: Int,
    val screenEventResourceId: String?
) : LogEvent("Assigned zaken")
