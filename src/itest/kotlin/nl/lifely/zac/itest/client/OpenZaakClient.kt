package nl.lifely.zac.itest.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm.HMAC256
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_ID
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_SECRET
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import java.util.Date

fun generateToken(): String =
    JWT.create().withIssuer(OPEN_ZAAK_CLIENT_ID)
        .withIssuedAt(Date())
        .withHeader(mapOf("client_identifier" to OPEN_ZAAK_CLIENT_ID))
        .withClaim("client_id", OPEN_ZAAK_CLIENT_ID)
        .withClaim("user_id", TEST_USER_1_ID)
        .withClaim("user_representation", TEST_USER_1_NAME)
        .sign(HMAC256(OPEN_ZAAK_CLIENT_SECRET))
