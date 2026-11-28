/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent
import java.util.UUID

class DefaultBehandelaarNotInGroup(
    val behandelaar: Employee,
    val groupId: String,
    val zaakUuid: UUID
) : LogEvent("Default behandelaar is not a member of the default group; the zaak is assigned to the group only")
