/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task.log

import nl.info.zac.log.LogEvent

class TakenReleased(
    val taskCount: Int,
    val releasedCount: Int,
    val screenEventResourceId: String?
) : LogEvent("Released tasks")
