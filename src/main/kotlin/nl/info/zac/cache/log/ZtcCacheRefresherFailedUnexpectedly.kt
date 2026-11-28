/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.cache.log

import nl.info.zac.log.LogEvent

data object ZtcCacheRefresherFailedUnexpectedly : LogEvent("Unexpected failure while refreshing the ZTC caches")
