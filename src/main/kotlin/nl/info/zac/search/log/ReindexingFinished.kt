/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.log

import nl.info.zac.log.LogEvent
import nl.info.zac.search.model.zoekobject.ZoekObjectType

@Suppress("LongParameterList")
class ReindexingFinished(
    val objectType: ZoekObjectType,
    val reindexedCount: Int,
    val totalCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val solrDocumentCount: Long?
) : LogEvent("Reindexing finished")
