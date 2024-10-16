package net.atos.client.zgw.brc.model.generated

import java.net.URI
import java.util.UUID

fun createBesluitInformatieObject(
    url: URI = URI("https://example.com/besluitInformatieObject/${UUID.randomUUID()}"),
    informatieobject: URI = URI("https://example.com/informatieObject/${UUID.randomUUID()}"),
    besluit: URI = URI("https://example.com/besluit/${UUID.randomUUID()}"),
) = BesluitInformatieObject(url).apply {
    this.informatieobject = informatieobject
    this.besluit = besluit
}
