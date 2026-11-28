/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification.log

import nl.info.zac.log.LogEvent
import nl.info.zac.notification.Action
import nl.info.zac.notification.Channel
import nl.info.zac.notification.Resource
import java.net.URI

class NotificationHandlingFailed(
    val handler: String,
    val channel: Channel,
    val resource: Resource,
    val resourceUrl: URI,
    val action: Action
) : LogEvent("Failed to handle notification")
