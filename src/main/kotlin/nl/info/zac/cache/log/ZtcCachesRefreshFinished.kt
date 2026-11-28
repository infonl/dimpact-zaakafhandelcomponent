/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.cache.log

import nl.info.zac.log.LogEvent

class ZtcCachesRefreshFinished(
    val durationMs: Long
) : LogEvent("Finished refreshing the ZTC caches")
