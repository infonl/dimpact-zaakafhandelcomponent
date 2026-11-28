/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.log

import nl.info.zac.log.LogEvent
import nl.info.zac.search.model.zoekobject.ZoekObjectType

class ReindexProgress(
    val objectType: ZoekObjectType,
    val progress: Long,
    val totalCount: Int
) : LogEvent("Reindexed")
