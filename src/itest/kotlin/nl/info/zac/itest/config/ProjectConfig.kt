/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.ItestConfiguration.ADDITIONAL_ALLOWED_FILE_TYPES
import nl.info.zac.itest.config.ItestConfiguration.BAG_MOCK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.KEYCLOAK_HEALTH_READY_URL
import nl.info.zac.itest.config.ItestConfiguration.KVK_MOCK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OFFICE_CONVERTER_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_MOCK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.SMTP_SERVER_PORT
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_DEFAULT_DOCKER_IMAGE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_HEALTH_READY_URL
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import okhttp3.Headers
import org.json.JSONObject
import org.slf4j.Logger
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.net.HttpURLConnection.HTTP_OK
import java.net.SocketException
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

// global variable so that it can be referenced elsewhere
lateinit var dockerComposeContainer: ComposeContainer

class ProjectConfig : AbstractProjectConfig() {
    private val logger = KotlinLogging.logger {}
    private val itestHttpClient = ItestHttpClient()

    private val zacDockerImage = System.getProperty("zacDockerImage") ?: run {
        ZAC_DEFAULT_DOCKER_IMAGE
    }
    private val dockerComposeEnvironment = mapOf(
        "AUTH_SSL_REQUIRED" to "none",
        "ADDITIONAL_ALLOWED_FILE_TYPES" to ADDITIONAL_ALLOWED_FILE_TYPES,
        "BAG_API_CLIENT_MP_REST_URL" to "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/",
        "FEATURE_FLAG_BPMN_SUPPORT" to "true",
        "KVK_API_CLIENT_MP_REST_URL" to KVK_MOCK_BASE_URI,
        "OFFICE_CONVERTER_CLIENT_MP_REST_URL" to OFFICE_CONVERTER_BASE_URI,
        "SMARTDOCUMENTS_ENABLED" to "true",
        "SMARTDOCUMENTS_CLIENT_MP_REST_URL" to SMART_DOCUMENTS_MOCK_BASE_URI,
        "SMTP_SERVER" to "greenmail",
        "SMTP_PORT" to SMTP_SERVER_PORT.toString(),
        "SIGNALERINGEN_DELETE_OLDER_THAN_DAYS" to "0",
        // override default entrypoint for ZAC Docker container to add JaCoCo agent for recording integration test coverage
        "ZAC_DOCKER_ENTRYPOINT" to
            "java" +
            " -javaagent:/jacoco-agent/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec" +
            // make sure that the WildFly management port is accessible from outside the container
            " -Djboss.bind.address.management=0.0.0.0" +
            " -Xms1024m" +
            " -Xmx1024m" +
            " -jar zaakafhandelcomponent.jar",
        "ZAC_DOCKER_IMAGE" to zacDockerImage,
        "ZAC_INTERNAL_ENDPOINTS_API_KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
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
                ).code shouldBe HTTP_OK
            }
            logger.info { "Keycloak is healthy" }
            logger.info { "Waiting until ZAC is healthy by calling the health endpoint and checking the response" }
            eventually(30.seconds) {
                itestHttpClient.performGetRequest(
                    headers = Headers.headersOf("Content-Type", "application/json"),
                    url = ZAC_HEALTH_READY_URL,
                    addAuthorizationHeader = false
                ).use { response ->
                    response.code shouldBe HTTP_OK
                    JSONObject(response.body!!.string()).getString("status") shouldBe "UP"
                }
            }
            logger.info { "ZAC is healthy" }
            authenticate(
                username = TEST_USER_1_USERNAME,
                password = TEST_USER_1_PASSWORD
            )
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Failed to start Docker containers" }
            dockerComposeContainer.stop()
        }
    }

    override suspend fun afterProject() {
        // stop ZAC Docker Container gracefully to give JaCoCo a change to generate the code coverage report
        dockerComposeContainer.getContainerByServiceName(ZAC_CONTAINER_SERVICE_NAME).getOrNull()?.let { zacContaner ->
            logger.info { "Stopping ZAC Docker container" }
            zacContaner.dockerClient
                .stopContainerCmd(zacContaner.containerId)
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
                "opa-tests",
                OneShotStartupWaitStrategy()
                    .withStartupTimeout(10.seconds.toJavaDuration())
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
