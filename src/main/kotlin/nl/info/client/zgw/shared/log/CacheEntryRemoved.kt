/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.log

import nl.info.zac.log.LogEvent

class CacheEntryRemoved(
    val cacheName: String,
    val key: String?,
    val cause: String? = null
) : LogEvent("Removed from cache")
