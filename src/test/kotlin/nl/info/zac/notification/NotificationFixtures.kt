/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.notification

import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun createNotificatie(
    channel: Channel = Channel.OBJECTEN,
    creationDateTime: ZonedDateTime = ZonedDateTime.now(),
    properties: MutableMap<String, String> = mutableMapOf(
        "objectType" to "http://example.com/fakeproducttype/${UUID.randomUUID()}"
    ),
    resource: Resource = Resource.OBJECT,
    action: Action = Action.CREATE,
    resourceUrl: URI = URI("http://example.com/fakeresourceurl"),
    mainResourceUrl: URI = resourceUrl
) = Notification(
    channel = channel,
    creationDateTime = creationDateTime,
    properties = properties,
    resource = resource,
    action = action,
    resourceUrl = resourceUrl,
    mainResourceUrl = mainResourceUrl
)
