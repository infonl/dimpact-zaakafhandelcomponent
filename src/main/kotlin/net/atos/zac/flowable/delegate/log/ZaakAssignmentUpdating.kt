/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent

class ZaakAssignmentUpdating(
    val zaakIdentificatie: String?,
    val groupId: String,
    val assignee: Employee?
) : LogEvent("Updating zaak assignment")
