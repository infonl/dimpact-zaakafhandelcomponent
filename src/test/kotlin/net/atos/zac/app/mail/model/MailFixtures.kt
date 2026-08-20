/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.mail.model

import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding

fun createRESTMailGegevens(
    verzender: String = "from@example.com",
    ontvanger: String = "to@example.com",
    vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding? = RestVertrouwelijkheidaanduiding.OPENBAAR,
) = RESTMailGegevens().apply {
    this.verzender = verzender
    this.ontvanger = ontvanger
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
}
