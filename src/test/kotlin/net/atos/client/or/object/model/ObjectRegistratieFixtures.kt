/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

@file:Suppress("PackageName")

package net.atos.client.or.`object`.model

import net.atos.client.or.objects.model.generated.ModelObject
import net.atos.client.or.objects.model.generated.ObjectRecord
import java.net.URI
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
    data: Map<String, Any> = mapOf("dummyKey" to "dummyValue")
) = ObjectRecord().apply {
    this.data = data
}
