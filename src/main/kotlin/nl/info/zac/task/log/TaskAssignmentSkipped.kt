/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task.log

import nl.info.zac.log.LogEvent

class TaskAssignmentSkipped(
    val taakId: String
) : LogEvent(
    "Task belongs to a zaakspecifiek geautoriseerde zaak whose zaaktype cannot authorise its taakbehandelaar; " +
        "it is skipped and not assigned"
)
