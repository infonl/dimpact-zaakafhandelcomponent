/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.log

import nl.info.zac.log.LogEvent

class ZtcCacheRefreshFailed(
    val typeName: String
) : LogEvent("Failed to refresh the ZTC cache")
