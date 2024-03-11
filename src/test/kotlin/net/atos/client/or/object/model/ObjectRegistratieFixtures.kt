@file:Suppress("PackageName")

package net.atos.client.or.`object`.model

import net.atos.client.or.objecttype.model.Objecttype
import java.net.URI
import java.util.UUID

fun createObjectRegistratieObject() =
    ORObject().apply {
        url = URI("https://example.com/objects/1")
        uuid = UUID.randomUUID()
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
