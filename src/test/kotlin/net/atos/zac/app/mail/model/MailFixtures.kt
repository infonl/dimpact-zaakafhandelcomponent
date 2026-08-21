/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.mail.model

import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding

fun createRESTMailGegevens(
    verzender: String = "from@example.com",
    ontvanger: String = "to@example.com",
    onderwerp: String = "fakeOnderwerp",
    body: String = "fakeBody",
    vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding? = RestVertrouwelijkheidaanduiding.OPENBAAR,
) = RESTMailGegevens(
    verzender = verzender,
    ontvanger = ontvanger,
    onderwerp = onderwerp,
    body = body,
    vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding,
)
