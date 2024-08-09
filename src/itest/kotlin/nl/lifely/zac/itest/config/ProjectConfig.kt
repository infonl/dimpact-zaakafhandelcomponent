/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest.config

import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_HEALTH_READY_URL
import nl.lifely.zac.itest.config.ItestConfiguration.KVK_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_1
import nl.lifely.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_2
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.SMTP_SERVER_PORT
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_CONTAINER_SERVICE_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_DEFAULT_DOCKER_IMAGE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_HEALTH_READY_URL
import okhttp3.Headers
import org.json.JSONObject
import org.slf4j.Logger
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.net.SocketException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val logger = KotlinLogging.logger {}

// global variable so that it can be referenced elsewhere
lateinit var dockerComposeContainer: ComposeContainer

class ProjectConfig : AbstractProjectConfig() {
    private val itestHttpClient = ItestHttpClient()

    private val zacDockerImage = System.getProperty("zacDockerImage") ?: run {
        ZAC_DEFAULT_DOCKER_IMAGE
    }
    private val dockerComposeEnvironment = mapOf(
        "KVK_API_CLIENT_MP_REST_URL" to KVK_MOCK_BASE_URI,
        "SD_CLIENT_MP_REST_URL" to SMART_DOCUMENTS_MOCK_BASE_URI,
        "SMTP_SERVER" to "greenmail",
        "SMTP_PORT" to SMTP_SERVER_PORT.toString(),
        "SIGNALERINGEN_DELETE_OLDER_THAN_DAYS" to "0",
        // override default entrypoint for ZAC Docker container to add JaCoCo agent
        "ZAC_DOCKER_ENTRYPOINT" to
            "java" +
            " -javaagent:/jacoco-agent/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec" +
            " -Xms1024m" +
            " -Xmx1024m" +
            " -jar zaakafhandelcomponent.jar",
        "ZAC_DOCKER_IMAGE" to zacDockerImage
    )

    override suspend fun beforeProject() {
        try {
            deleteLocalDockerVolumeData()

            dockerComposeContainer = createDockerComposeContainer()
            dockerComposeContainer.start()
            logger.info { "Started ZAC Docker Compose containers" }
            logger.info { "Waiting until Keycloak is healthy by calling the health endpoint and checking the response" }
            eventually(
                eventuallyConfig {
                    duration = 30.seconds
                    expectedExceptions = setOf(SocketException::class)
                }
            ) {
                itestHttpClient.performGetRequest(
                    headers = Headers.headersOf("Content-Type", "application/json"),
                    url = KEYCLOAK_HEALTH_READY_URL,
                    addAuthorizationHeader = false
                ).code shouldBe HTTP_STATUS_OK
            }
            logger.info { "Keycloak is healthy" }
            logger.info { "Waiting until ZAC is healthy by calling the health endpoint and checking the response" }
            eventually(30.seconds) {
                itestHttpClient.performGetRequest(
                    headers = Headers.headersOf("Content-Type", "application/json"),
                    url = ZAC_HEALTH_READY_URL,
                    addAuthorizationHeader = false
                ).use { response ->
                    response.code shouldBe HTTP_STATUS_OK
                    JSONObject(response.body!!.string()).getString("status") shouldBe "UP"
                }
            }
            logger.info { "ZAC is healthy" }

            KeycloakClient.authenticate()

            ZacClient().createZaakAfhandelParameters(
                ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE,
                ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID,
                ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION,
                PRODUCTAANVRAAG_TYPE_1
            ).use { response ->
                response.isSuccessful shouldBe true
            }
            ZacClient().createZaakAfhandelParameters(
                ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_IDENTIFICATIE,
                ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
                ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION,
                PRODUCTAANVRAAG_TYPE_2
            ).use { response ->
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
                .withTimeout(30.seconds.inWholeSeconds.toInt())
                .exec()
            logger.info { "Stopped ZAC Docker container" }
        }
        // now stop the rest of the Docker Compose containers (TestContainers just kills and removes the containers)
        dockerComposeContainer.withOptions("--profile itest").stop()
    }

    override val specExecutionOrder = SpecExecutionOrder.Annotated

    @Suppress("UNCHECKED_CAST")
    private fun createDockerComposeContainer(): ComposeContainer {
        logger.info { "Using ZAC Docker image: '$zacDockerImage'" }

        return ComposeContainer(File("docker-compose.yaml"))
            .withLocalCompose(true)
            .withRemoveVolumes(System.getenv("REMOVE_DOCKER_COMPOSE_VOLUMES")?.toBoolean() ?: true)
            .withEnv(dockerComposeEnvironment)
            .withOptions(
                "--profile zac",
                "--profile itest"
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
                    .withStartupTimeout(3.minutes.toJavaDuration())
            )
            .waitingFor(
                "zac",
                Wait.forLogMessage(".* WildFly .* started .*", 1)
                    .withStartupTimeout(3.minutes.toJavaDuration())
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
