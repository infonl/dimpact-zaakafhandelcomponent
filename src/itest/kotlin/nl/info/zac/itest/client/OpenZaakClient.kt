/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm.HMAC256
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_ID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_SECRET
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import java.util.Date

fun generateToken(): String =
    JWT.create().withIssuer(OPEN_ZAAK_CLIENT_ID)
        .withIssuedAt(Date())
        .withHeader(mapOf("client_identifier" to OPEN_ZAAK_CLIENT_ID))
        .withClaim("client_id", OPEN_ZAAK_CLIENT_ID)
        .withClaim("user_id", TEST_USER_1_USERNAME)
        .withClaim("user_representation", TEST_USER_1_NAME)
        .sign(HMAC256(OPEN_ZAAK_CLIENT_SECRET))
