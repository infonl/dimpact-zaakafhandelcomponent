/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package io.kotest.provided

import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_HEALTH_READY_URL
import nl.lifely.zac.itest.config.ItestConfiguration.SMARTDOCUMENTS_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_CONTAINER_SERVICE_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_DEFAULT_DOCKER_IMAGE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_HEALTH_READY_URL
import okhttp3.Headers
import org.awaitility.kotlin.await
import org.json.JSONObject
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
    private val itestHttpClient = ItestHttpClient()

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
                        itestHttpClient.performGetRequest(
                            headers = Headers.headersOf("Content-Type", "application/json"),
                            url = KEYCLOAK_HEALTH_READY_URL
                        ).isSuccessful
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
                    itestHttpClient.performGetRequest(
                        headers = Headers.headersOf("Content-Type", "application/json"),
                        url = ZAC_HEALTH_READY_URL
                    ).use { response ->
                        response.isSuccessful && JSONObject(response.body!!.string()).getString("status") == "UP"
                    }
                }
            logger.info { "ZAC is healthy" }

            KeycloakClient.authenticate()

            ZacClient().createZaakAfhandelParameters().use { response ->
                response.isSuccessful shouldBe true
            }
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Failed to start Docker containers" }
            dockerComposeContainer.stop()
        }
    }

    override suspend fun afterProject() {
        // stop ZAC Docker Container gracefully to give JaCoCo a change to generate the code coverage report
        with(dockerComposeContainer.getContainerByServiceName(ZAC_CONTAINER_SERVICE_NAME).get()) {
            logger.info { "Stopping ZAC Docker container" }
            dockerClient
                .stopContainerCmd(containerId)
                .withTimeout(THIRTY_SECONDS.toSecondsPart())
                .exec()
            logger.info { "Stopped ZAC Docker container" }
        }
        // now stop the rest of the Docker Compose containers (TestContainers just kills and removes the containers)
        dockerComposeContainer.stop()
    }

    override val specExecutionOrder = SpecExecutionOrder.Annotated

    @Suppress("UNCHECKED_CAST")
    private fun createDockerComposeContainer(): ComposeContainer {
        val zacDockerImage = System.getProperty("zacDockerImage") ?: run {
            ZAC_DEFAULT_DOCKER_IMAGE
        }
        logger.info { "Using ZAC Docker image: '$zacDockerImage'" }

        return ComposeContainer(File("docker-compose.yaml"))
            .withLocalCompose(true)
            .withEnv(
                mapOf(
                    // override default entrypoint for ZAC Docker container to add JaCoCo agent
                    "ZAC_DOCKER_ENTRYPOINT" to
                        "java" +
                        " -javaagent:/jacoco-agent/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec" +
                        " -Xms1024m" +
                        " -Xmx1024m" +
                        " -jar zaakafhandelcomponent.jar",
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
                "openzaak.local",
                Slf4jLogConsumer((logger as DelegatingKLogger<Logger>).underlyingLogger).withPrefix(
                    "OPENZAAK"
                )
            )
            .withLogConsumer(
                ZAC_CONTAINER_SERVICE_NAME,
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
