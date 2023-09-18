@file:Suppress("PackageName")

package net.atos.client.or.`object`.model

import java.net.URI
import java.util.UUID

fun createObjectRegistratieObject() =
    ORObject().apply {
        url = URI("https://example.com/objects/1")
        uuid = UUID.randomUUID()
    }
