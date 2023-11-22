/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package io.kotest.provided

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.client.createZaakAfhandelParameters
import nl.lifely.zac.itest.config.ItestConfiguration.SMARTDOCUMENTS_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_MANAGEMENT_URI
import org.awaitility.kotlin.await
import org.slf4j.Logger
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Duration

private val logger = KotlinLogging.logger {}

object ProjectConfig : AbstractProjectConfig() {
    @Suppress("MagicNumber")
    private val THREE_MINUTES = Duration.ofMinutes(3)

    @Suppress("MagicNumber")
    private val TWENTY_SECONDS = Duration.ofSeconds(20)

    private lateinit var dockerComposeContainer: ComposeContainer

    @Suppress("UNCHECKED_CAST")
    override suspend fun beforeProject() {
        try {
            deleteLocalDockerVolumeData()

            dockerComposeContainer = createDockerComposeContainer()
            dockerComposeContainer.start()
            logger.info { "Started ZAC Docker Compose containers" }
            logger.info { "Waiting until ZAC is healthy by calling the health endpoint and checking the response" }
            await.atMost(TWENTY_SECONDS)
                .until {
                    khttp.get(
                        url = "${ZAC_MANAGEMENT_URI}/health/ready",
                        headers = mapOf(
                            "Content-Type" to "application/json",
                        )
                    ).let {
                        it.statusCode == HttpStatus.SC_OK && it.jsonObject.getString("status") == "UP"
                    }
                }
            KeycloakClient.authenticate()
            createZaakAfhandelParameters()
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Failed to start Docker containers" }
            dockerComposeContainer.stop()
        }
    }

    override suspend fun afterProject() {
        dockerComposeContainer.stop()
    }

    override val specExecutionOrder = SpecExecutionOrder.Annotated

    private fun createDockerComposeContainer() =
        ComposeContainer(File("docker-compose.yaml"))
            .withLocalCompose(true)
            .withEnv(
                mapOf(
                    "ZAC_DOCKER_IMAGE" to "ghcr.io/infonl/zaakafhandelcomponent:dev",
                    "SD_CLIENT_MP_REST_URL" to SMARTDOCUMENTS_MOCK_BASE_URI
                )
            )
            .withOptions(
                "--profile zac"
            )
            .withLogConsumer(
                "solr",
                Slf4jLogConsumer((logger as DelegatingKLogger<Logger>).underlyingLogger).withPrefix(
                    "SOLR"
                )
            )
            .withLogConsumer(
                "keycloak",
                Slf4jLogConsumer((logger as DelegatingKLogger<Logger>).underlyingLogger).withPrefix(
                    "KEYCLOAK"
                )
            )
            .withLogConsumer(
                "zac",
                Slf4jLogConsumer((logger as DelegatingKLogger<Logger>).underlyingLogger).withPrefix(
                    "ZAC"
                )
            )
            .waitingFor(
                "openzaak.local",
                Wait.forLogMessage(".*spawned uWSGI worker 2.*", 1)
                    .withStartupTimeout(THREE_MINUTES)
            )
            .waitingFor(
                "zac",
                Wait.forLogMessage(".* WildFly Full .* started .*", 1)
                    .withStartupTimeout(THREE_MINUTES)
            )

    /**
     * The integration tests assume a clean environment.
     * For that reason we first need to remove any local Docker volume data that may have been created
     *  by a previous run.
     * Local Docker volume data is created because we reuse the same Docker Compose file that we also
     * use for running ZAC locally.
     */
    private fun deleteLocalDockerVolumeData() {
        val file = File("${System.getProperty("user.dir")}/scripts/docker-compose/volume-data")
        if (file.exists()) {
            logger.info { "Deleting existing folder '$file' because the integration tests assume a clean environment" }
            file.deleteRecursively().let { deleted ->
                if (deleted) {
                    logger.info { "Deleted folder '$file'" }
                } else {
                    logger.error { "Failed to delete folder '$file'" }
                }
            }
        }
    }
}
