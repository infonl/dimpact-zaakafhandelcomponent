/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.model.getFullName
import java.util.Date

/**
 * Generates a ZGW JWT token for authenticating with ZGW services.
 *
 * @param clientId The client identifier.
 * @param secret The secret used to sign the token.
 * @param loggedInUser The logged-in user, if any. If a logged-in user is available, it should always be provided.
 * @return The generated JWT token as a Bearer token string.
 */
fun generateZgwJwtToken(clientId: String, secret: String, loggedInUser: LoggedInUser?): String {
    val jwtToken = JWT.create()
        .withIssuer(clientId)
        .withIssuedAt(Date())
        .withHeader(mapOf("client_identifier" to clientId))
        .withClaim("client_id", clientId)
        .apply {
            loggedInUser?.let {
                withClaim("user_id", it.id)
                withClaim("user_representation", it.getFullName())
            }
        }
        .sign(Algorithm.HMAC256(secret))
    return "Bearer $jwtToken"
}

