/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.log

import nl.info.zac.log.LogEvent
import nl.info.zac.search.model.zoekobject.ZoekObjectType

class ReindexingFailed(
    val objectTypes: Collection<ZoekObjectType>
) : LogEvent("Reindexing failed, continuing with remaining object types")
