/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

@file:Suppress("PackageName")

package net.atos.client.or.`object`.model

import net.atos.client.or.objects.model.generated.ObjectRecord
import net.atos.client.or.objecttype.model.Objecttype
import java.net.URI
import java.util.UUID

fun createORObject(
    url: URI = URI("https://example.com/objects/1"),
    uuid: UUID = UUID.randomUUID(),
    record: ObjectRecord? = null
) =
    ORObject().apply {
        this.url = url
        this.uuid = uuid
        this.record = record
    }

fun createObjectRecord(
    data: Map<String, Any> = mapOf("dummyKey" to "dummyValue")
) = ObjectRecord().apply {
    this.data = data
}

fun createObjecttype(
    url: URI = URI("https://example.com/objecttypes/1"),
    uuid: UUID = UUID.randomUUID(),
    name: String = "dummyName"
) =
    Objecttype().apply {
        this.url = url
        this.uuid = uuid
        this.name = name
    }
