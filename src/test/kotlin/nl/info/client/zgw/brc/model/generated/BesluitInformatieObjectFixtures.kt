/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.brc.model.generated

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
