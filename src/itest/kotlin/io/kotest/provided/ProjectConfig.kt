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
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_HEALTH_READY_URL
import nl.lifely.zac.itest.config.ItestConfiguration.SMARTDOCUMENTS_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_DEFAULT_DOCKER_IMAGE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_HEALTH_READY_URL
import org.awaitility.kotlin.await
import org.slf4j.Logger
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.net.SocketException
import java.time.Duration

private val logger = KotlinLogging.logger {}

object ProjectConfig : AbstractProjectConfig() {
    @Suppress("MagicNumber")
    val THREE_MINUTES = Duration.ofMinutes(3)

    @Suppress("MagicNumber")
    val THIRTY_SECONDS = Duration.ofSeconds(30)

    lateinit var dockerComposeContainer: ComposeContainer

    override suspend fun beforeProject() {
        try {
            deleteLocalDockerVolumeData()

            dockerComposeContainer = createDockerComposeContainer()
            dockerComposeContainer.start()
            logger.info { "Started ZAC Docker Compose containers" }
            logger.info { "Waiting until Keycloak is healthy by calling the health endpoint and checking the response" }
            await.atMost(THIRTY_SECONDS)
                .until {
                    try {
                        khttp.get(
                            url = KEYCLOAK_HEALTH_READY_URL,
                            headers = mapOf(
                                "Content-Type" to "application/json",
                            )
                        ).let {
                            it.statusCode == HttpStatus.SC_OK
                        }
                    } catch (socketException: SocketException) {
                        logger.info(socketException) {
                            "SocketException while requesting Keycloak health endpoint. Ignoring."
                        }
                        false
                    }
                }
            logger.info { "Keycloak is healthy" }
            logger.info { "Waiting until ZAC is healthy by calling the health endpoint and checking the response" }
            await.atMost(THIRTY_SECONDS)
                .until {
                    khttp.get(
                        url = ZAC_HEALTH_READY_URL,
                        headers = mapOf(
                            "Content-Type" to "application/json",
                        )
                    ).let {
                        it.statusCode == HttpStatus.SC_OK && it.jsonObject.getString("status") == "UP"
                    }
                }
            logger.info { "ZAC is healthy" }

            KeycloakClient.authenticate()

            val response = ZacClient().createZaakAfhandelParameters()
            response.statusCode shouldBe HttpStatus.SC_OK
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Failed to start Docker containers" }
            dockerComposeContainer.stop()
        }
    }

    override suspend fun afterProject() {
        dockerComposeContainer.stop()
    }

    override val specExecutionOrder = SpecExecutionOrder.Annotated

    private fun createDockerComposeContainer(): ComposeContainer {
        val zacDockerImage = System.getProperty("zacDockerImage") ?: run {
            ZAC_DEFAULT_DOCKER_IMAGE
        }
        logger.info { "Using ZAC Docker image: '$zacDockerImage'" }

        return ComposeContainer(File("docker-compose.yaml"))
            .withLocalCompose(true)
            .withEnv(
                mapOf(
                    "ZAC_DOCKER_IMAGE" to zacDockerImage,
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
    }

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
