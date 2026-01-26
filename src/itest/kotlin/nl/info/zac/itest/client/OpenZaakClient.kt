/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm.HMAC256
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_ID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_SECRET
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_URI
import java.util.Date
import java.util.UUID

class OpenZaakClient(
    val itestHttpClient: ItestHttpClient
) {
    fun getRolesForZaak(zaakUUID: UUID): ResponseContent =
        itestHttpClient.performZgwApiGetRequest(
            url = "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/rollen?zaak=$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/zaken/$zaakUUID"
        )
}

/**
 * Generates a JWT token for OpenZaak client authentication from our integration tests.
 * Note that no user claims are added, as this is not required for these requests from
 * our integration tests.
 */
fun generateOpenZaakJwtToken(): String =
    JWT.create().withIssuer(OPEN_ZAAK_CLIENT_ID)
        .withIssuedAt(Date())
        .withHeader(mapOf("client_identifier" to OPEN_ZAAK_CLIENT_ID))
        .withClaim("client_id", OPEN_ZAAK_CLIENT_ID)
        .sign(HMAC256(OPEN_ZAAK_CLIENT_SECRET))
