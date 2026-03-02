/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
@file:Suppress("PackageName")

package nl.info.client.or.`object`.model

import nl.info.client.or.objects.model.generated.ModelObject
import nl.info.client.or.objects.model.generated.ObjectRecord
import java.net.URI
import java.time.LocalDate
import java.util.UUID

fun createORObject(
    url: URI = URI("https://example.com/objects/1"),
    uuid: UUID = UUID.randomUUID(),
    record: ObjectRecord? = null
) =
    ModelObject(url).apply {
        this.uuid = uuid
        this.record = record
    }

fun createObjectRecord(
    data: Map<String, Any> = mapOf("fakeKey" to "fakeValue"),
    startAt: LocalDate? = null
) = ObjectRecord().apply {
    this.data = data
    this.startAt = startAt
}

fun createModelObject(
    uuid: UUID = UUID.randomUUID(),
    url: URI = URI("https://example.com/objects/1"),
    type: URI = URI("https://example.com/objecttypes/1"),
) = ModelObject(url).apply {
    this.uuid = uuid
    this.type = type
}
