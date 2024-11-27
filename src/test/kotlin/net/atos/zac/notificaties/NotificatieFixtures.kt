/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.notificaties

import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun createNotificatie(
    channel: Channel = Channel.OBJECTEN,
    creationDateTime: ZonedDateTime = ZonedDateTime.now(),
    properties: Map<String, String> = mapOf(
        "objectType" to "http://example.com/dummyproducttype/${UUID.randomUUID()}"
    ),
    resource: Resource = Resource.OBJECT,
    action: Action = Action.CREATE,
    resourceUrl: URI = URI("http://example.com/dummyresourceurl"),
    mainResourceUrl: URI = resourceUrl
) = Notificatie().apply {
    this.channel = channel
    this.creationDateTime = creationDateTime
    this.properties = properties
    this.resource = resource
    this.action = action
    this.resourceUrl = resourceUrl
    this.mainResourceUrl = mainResourceUrl
}
