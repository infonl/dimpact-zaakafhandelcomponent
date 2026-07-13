/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_DATABASE_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.dockerComposeContainer
import java.net.HttpURLConnection.HTTP_OK

/**
 * Verifies that the `ZaakafhandelcomponentDS` WildFly datasource recovers from a stale/dead
 * PostgreSQL connection by validating connections on match (see `configure-wildfly.cli`).
 * Without this, a request reusing a pooled connection whose underlying PostgreSQL backend
 * was terminated would fail with a "This connection has been closed." SQL exception.
 */
class DataSourceConnectionValidationTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("ZAC is running and has an established connection to its database in the connection pool") {
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/referentietabellen",
            testUser = BEHEERDER_1,
        ).code shouldBe HTTP_OK

        When(
            """all PostgreSQL backend connections to the zac database are terminated
                and the reference tables are requested again"""
        ) {
            val databaseContainer = dockerComposeContainer
                .getContainerByServiceName(ZAC_DATABASE_CONTAINER_SERVICE_NAME)
                .get()
            val execResult = databaseContainer.execInContainer(
                "psql",
                "-U",
                "zac",
                "-d",
                "zac",
                "-c",
                "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'zac' AND pid <> pg_backend_pid();"
            )
            logger.info { "pg_terminate_backend stdout: ${execResult.stdout}, stderr: ${execResult.stderr}" }

            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/referentietabellen",
                testUser = BEHEERDER_1,
            )

            Then("the terminate command should succeed") {
                execResult.exitCode shouldBe 0
            }

            Then(
                """the request still succeeds because the connection pool detects the
                    stale connection and transparently replaces it with a new one"""
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
            }
        }
    }
})
