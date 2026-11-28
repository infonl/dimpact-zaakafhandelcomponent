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
import java.time.ZonedDateTime

class NotificationReceived(
    val channel: Channel,
    val mainResourceUrl: URI,
    val resource: Resource,
    val resourceUrl: URI,
    val action: Action,
    val creationDateTime: ZonedDateTime
) : LogEvent("Received notification")
