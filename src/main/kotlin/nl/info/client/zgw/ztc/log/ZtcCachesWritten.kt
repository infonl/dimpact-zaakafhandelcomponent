/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.log

import nl.info.zac.log.LogEvent

data object ZtcCachesWritten : LogEvent("Wrote the refreshed types to the ZTC caches, except the ones invalidated during the refresh")
