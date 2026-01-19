/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.model.getFullName
import java.util.Date

/**
 * Generates a JWT token for authenticating with ZGW APIs.
 *
 * @param clientId The client identifier.
 * @param secret The secret used to sign the token.
 * @param loggedInUser The logged-in user. The user id and full name are used for auditing purposes by the ZGW APIs.
 * @return The generated JWT token as a Bearer token string.
 */
fun generateZgwJwtToken(clientId: String, secret: String, loggedInUser: LoggedInUser): String {
    val jwtToken = JWT.create()
        .withIssuer(clientId)
        .withIssuedAt(Date())
        .withHeader(mapOf("client_identifier" to clientId))
        .withClaim("client_id", clientId)
        .apply {
            loggedInUser.let {
                withClaim("user_id", it.id)
                withClaim("user_representation", it.getFullName())
            }
        }
        .sign(Algorithm.HMAC256(secret))
    return "Bearer $jwtToken"
}
