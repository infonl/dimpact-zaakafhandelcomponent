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
import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.BAG_MOCK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.BPMN_DOCUMENT_SIGN_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.BPMN_DOCUMENT_SIGN_PROCESS_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_DOCUMENT_SIGN_SELECT_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_DOCUMENT_SIGN_SUMMARY_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_PERMISSION_CHECK_PROCESS_CHOOSE_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_PERMISSION_CHECK_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.BPMN_PERMISSION_CHECK_PROCESS_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUMMARY_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_EXTEND_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_PROCESS_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_RESUME_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_SUSPEND_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_COPY_USER_GROUP_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_DEFAULT_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_HARDCODED_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_NEW_ZAAK_DEFAULTS_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_PROCESS_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_USER_GROUP_SELECTION_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.GREENMAIL_API_URI
import nl.info.zac.itest.config.ItestConfiguration.KEYCLOAK_HEALTH_READY_URL
import nl.info.zac.itest.config.ItestConfiguration.KVK_MOCK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OFFICE_CONVERTER_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.PABC_API_KEY
import nl.info.zac.itest.config.ItestConfiguration.PABC_CLIENT_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_1
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_2
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_3
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_4
import nl.info.zac.itest.config.ItestConfiguration.SMTP_SERVER_PORT
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_3_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_3_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_DEFAULT_DOCKER_IMAGE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_HEALTH_READY_URL
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import okhttp3.Headers
import org.json.JSONObject
import org.slf4j.Logger
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.net.SocketException
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

// global variable so that it can be referenced elsewhere
lateinit var dockerComposeContainer: ComposeContainer

@Suppress("TooManyFunctions")
class ZacItestProjectConfig : AbstractProjectConfig() {
    companion object {
        private const val DO_NOT_START_DOCKER_COMPOSE_ENV_VAR = "DO_NOT_START_DOCKER_COMPOSE"
        private const val KEEP_ITEST_CONTAINERS_RUNNING_ENV_VAR = "KEEP_ITEST_CONTAINERS_RUNNING"
        private const val DOCKER_USE_ARM64_CONTAINERS_ENV_VAR = "DOCKER_USE_ARM64_CONTAINERS"
        private const val COMPOSE_PROJECT_PREFIX = "zac-itest-"
        private const val COMPOSE_SERVICE_LABEL = "com.docker.compose.service"
        private const val COMPOSE_PROJECT_LABEL = "com.docker.compose.project"
        private const val DEBUG_LOG_TAIL_LINES = 300
        private const val DEBUG_LOG_FETCH_TIMEOUT_SECONDS = 10L

        private val logger = KotlinLogging.logger {}
        private val itestHttpClient = ItestHttpClient()
        private val zacClient = ZacClient()
        private val zacDockerImage = System.getProperty("zacDockerImage") ?: ZAC_DEFAULT_DOCKER_IMAGE
        private val skipDockerComposeStart = System.getenv(DO_NOT_START_DOCKER_COMPOSE_ENV_VAR)?.toBoolean() ?: false

        // Opt-in flag for developers who want to inspect or reuse the Compose stack after a local itest run,
        // instead of it being torn down automatically. Deliberately independent of TESTCONTAINERS_RYUK_DISABLED:
        // that variable is now unconditionally true under Podman (rootless Podman can't run the privileged Ryuk
        // resource-reaper container - see design.md), so it can no longer double as "skip our own teardown too"
        // the way it used to under Docker, or every single itest run would leave its containers running.
        private val keepContainersRunning = System.getenv(KEEP_ITEST_CONTAINERS_RUNNING_ENV_VAR)?.toBoolean() ?: false

        // All variables below have to be overridable in the docker-compose.yaml file
        private val dockerComposeOverrideEnvironment = mapOf(
            "APP_ENV" to "itest",
            "AUTH_SSL_REQUIRED" to "none",
            "BAG_API_CLIENT_MP_REST_URL" to "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/",
            "KVK_API_CLIENT_MP_REST_URL" to KVK_MOCK_BASE_URI,
            "OFFICE_CONVERTER_CLIENT_MP_REST_URL" to OFFICE_CONVERTER_BASE_URI,
            "PABC_API_CLIENT_MP_REST_URL" to PABC_CLIENT_BASE_URI,
            "PABC_API_KEY" to PABC_API_KEY,
            "BRP_PROTOCOLLERING_ENABLED" to "true",
            "BRP_DOELBINDING_PER_ZAAKTYPE" to "true",
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
    }

    private var emptyEnvFile: File? = null

    /**
     * Set a random order seed so that the tests run is reproducible.
     */
    override val randomOrderSeed = Random.nextLong()

    /**
     * Run the integration tests in random order to make sure they remain isolated
     * and do not depend on each other's side effects.
     */
    override val specExecutionOrder = SpecExecutionOrder.Random

    /**
     * Purge GreenMail's email store before each spec to prevent emails sent by one spec
     * from leaking into another spec's assertions.
     */
    override val extensions: List<Extension> = listOf(
        object : BeforeSpecListener {
            override suspend fun beforeSpec(spec: Spec) {
                logger.info { "Purging GreenMail email store before spec '${spec::class.simpleName}'" }
                itestHttpClient.performDeleteRequest(url = "$GREENMAIL_API_URI/service")
            }
        }
    )

    override suspend fun beforeProject() {
        logger.info { "Starting integration tests with random seed: '$randomOrderSeed'" }
        try {
            if (!skipDockerComposeStart) {
                deleteLocalDockerVolumeData()
                dockerComposeContainer = createDockerComposeContainer()
                dockerComposeContainer.start()
                logger.info { "Started ZAC Docker Compose containers" }
            } else {
                logger.warn {
                    "$DO_NOT_START_DOCKER_COMPOSE_ENV_VAR environment variable is set to true, not starting Docker Compose containers"
                }
            }

            logger.info { "Waiting until Keycloak is healthy by calling the health endpoint and checking the response" }
            eventually(
                eventuallyConfig {
                    duration = 30.seconds
                    expectedExceptions = setOf(SocketException::class)
                }
            ) {
                itestHttpClient.performGetRequest(
                    headers = Headers.headersOf("Content-Type", "application/json"),
                    url = KEYCLOAK_HEALTH_READY_URL
                ).code shouldBe HTTP_OK
            }
            logger.info { "Keycloak is healthy" }
            logger.info { "Waiting until ZAC is healthy by calling the health endpoint and checking the response" }
            eventually(60.seconds) {
                itestHttpClient.performGetRequest(
                    headers = Headers.headersOf("Content-Type", "application/json"),
                    url = ZAC_HEALTH_READY_URL
                ).let { response ->
                    response.code shouldBe HTTP_OK
                    JSONObject(response.bodyAsString).getString("status") shouldBe "UP"
                }
            }
            logger.info { "ZAC is healthy" }
            if (!skipDockerComposeStart) {
                createTestSetupData()
            }
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Failed to start Docker Compose containers" }
            // Log the state and recent output of every container in this Compose project before tearing it
            // down, since the teardown below removes the containers and their logs would otherwise be lost.
            // This is what lets us diagnose *why* a container failed to become healthy in CI, instead of only
            // seeing the generic "dependency failed to start: container is unhealthy" Compose error.
            dumpContainerLogsForDebugging()
            dockerComposeContainer.stop()
        }
    }

    /**
     * Logs the state and last output of every container belonging to this Compose project. Best-effort: any
     * failure here must not prevent the Compose-wide stop that runs immediately after this in the caller.
     */
    private fun dumpContainerLogsForDebugging() {
        runCatching {
            val dockerClient = DockerClientFactory.instance().client()
            dockerClient.listContainersCmd().withShowAll(true).exec()
                .filter { container -> container.labels?.get(COMPOSE_PROJECT_LABEL)?.startsWith(COMPOSE_PROJECT_PREFIX) == true }
                .forEach { container ->
                    val serviceName = container.labels?.get(COMPOSE_SERVICE_LABEL) ?: "unknown"
                    val containerName = container.names?.firstOrNull() ?: container.id
                    logger.error { "--- Logs for '$containerName' (service '$serviceName', state '${container.state}') ---" }
                    runCatching {
                        dockerClient.logContainerCmd(container.id)
                            .withStdOut(true)
                            .withStdErr(true)
                            .withTail(DEBUG_LOG_TAIL_LINES)
                            .exec(
                                object : ResultCallback.Adapter<Frame>() {
                                    override fun onNext(frame: Frame) {
                                        logger.error { "[$serviceName] ${String(frame.payload).trimEnd()}" }
                                    }
                                }
                            ).awaitCompletion(DEBUG_LOG_FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    }.onFailure { throwable ->
                        logger.warn(throwable) { "Failed to fetch logs for '$containerName'" }
                    }
                }
        }.onFailure { throwable ->
            logger.warn(throwable) { "Failed to dump container logs for debugging" }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun afterProject() {
        try {
            if (skipDockerComposeStart) {
                logger.warn {
                    "$DO_NOT_START_DOCKER_COMPOSE_ENV_VAR environment variable is set to true, not stopping Docker Compose containers"
                }
                return
            }
            if (keepContainersRunning) {
                logger.warn {
                    "$KEEP_ITEST_CONTAINERS_RUNNING_ENV_VAR environment variable is set to true, not stopping Docker Compose containers"
                }
                Runtime.getRuntime().halt(0)
            }

            val zacContainer = dockerComposeContainer.getContainerByServiceName(ZAC_CONTAINER_SERVICE_NAME).getOrNull()

            // Stop the ZAC Docker container gracefully to give JaCoCo a chance to generate the code coverage
            // report. This is a best-effort step: if the Docker/Podman API call itself fails (e.g. a transient
            // socket timeout under load), it must not prevent the Compose-wide stop below from running, or every
            // container is left running.
            runCatching {
                zacContainer?.let {
                    logger.info { "Stopping ZAC Docker container" }
                    it.dockerClient
                        .stopContainerCmd(it.containerId)
                        .withTimeout(30.seconds.inWholeSeconds.toInt())
                        .exec()
                    logger.info { "Stopped ZAC Docker container" }
                }
            }.onFailure { throwable ->
                logger.warn(throwable) {
                    "Failed to stop the ZAC Docker container gracefully; the JaCoCo coverage report for this " +
                        "run may be incomplete. Continuing with the Compose-wide stop regardless."
                }
            }
            // now stop the rest of the Docker Compose containers (TestContainers just kills and removes the containers)
            dockerComposeContainer.withOptions("--profile itest").stop()

            // Confirmed empirically under Podman: when the graceful stop above races with a slow/timed-out
            // Docker API response, Compose's own teardown can end up skipping that one container entirely
            // (no "Stopping"/"Removing" logged for it) even though every other service in the stack is torn
            // down correctly. Force it away as a last resort so no itest container is ever left running.
            zacContainer?.let {
                runCatching {
                    if (it.isRunning) {
                        logger.warn { "ZAC Docker container is still running after the Compose-wide stop; forcing removal" }
                        it.dockerClient.removeContainerCmd(it.containerId).withForce(true).exec()
                    }
                }.onFailure { throwable ->
                    logger.warn(throwable) { "Failed to force-remove the ZAC Docker container" }
                }
            }
        } finally {
            emptyEnvFile?.delete()
        }
    }

    // TestContainers' ComposeContainer shells out to a "docker compose"-compatible executable on PATH.
    // Under Podman, install the podman-docker compatibility package (or an equivalent `docker` shim/alias
    // that forwards to `podman`) so this resolves to Podman; DOCKER_HOST then points TestContainers'
    // Docker-API client at the Podman socket. See design.md for details.
    @Suppress("UNCHECKED_CAST", "LongMethod")
    private fun createDockerComposeContainer(): ComposeContainer {
        logger.info { "Using Docker/Podman Compose environment variables: $dockerComposeOverrideEnvironment" }

        // Create a temporary empty env file so Compose does not load any local .env file,
        // which could override variables required by the integration tests.
        val envFile = Files.createTempFile("zac-itest", ".env").toFile()
        emptyEnvFile = envFile

        val composeFiles: MutableList<File> = mutableListOf(File("docker-compose.yaml"))
        System.getenv(DOCKER_USE_ARM64_CONTAINERS_ENV_VAR)
            ?.takeIf { it.isNotBlank() }
            ?.let {
                composeFiles.add(File("docker-compose.arm64-override.yaml"))
                logger.info { "Using arm64 containers" }
            }

        return ComposeContainer(COMPOSE_PROJECT_PREFIX, composeFiles)
            .withEnv(dockerComposeOverrideEnvironment)
            // do not pull images first because this will cause _all_ Docker images to be pulled,
            // and not just the ones we need for our profiles
            .withPull(false)
            .withOptions(
                "--profile zac",
                "--profile itest",
                "--env-file ${envFile.absolutePath}"
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
                "pabc-api",
                Wait.forLogMessage(".* Application started.*", 1)
                    .withStartupTimeout(3.minutes.toJavaDuration())
            )
            .waitingFor(
                "zac",
                Wait.forLogMessage(".* WildFly .* started .*", 1)
                    .withStartupTimeout(3.minutes.toJavaDuration())
            )
            .waitingFor(
                "greenmail",
                Wait.forLogMessage(".*Starting GreenMail API server.*", 1)
                    .withStartupTimeout(2.minutes.toJavaDuration())
            )
    }

    /**
     * The integration tests assume a clean environment.
     * For that reason, we first need to remove any local Docker volume data that may have been created
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

    /**
     * Creates overall test setup data in ZAC, required for running the integration tests.
     */
    private fun createTestSetupData() {
        createBpmnProcessDefinitions()
        createBpmnProcessTaskForms()
        createZaaktypeConfigurations()
    }

    private fun createBpmnProcessDefinitions() {
        arrayOf(
            BPMN_TEST_PROCESS_RESOURCE_PATH,
            BPMN_TEST_USER_MANAGEMENT_PROCESS_RESOURCE_PATH,
            BPMN_DOCUMENT_SIGN_PROCESS_RESOURCE_PATH,
            BPMN_SUSPEND_RESUME_PROCESS_RESOURCE_PATH,
            BPMN_PERMISSION_CHECK_PROCESS_RESOURCE_PATH
        ).forEach {
            itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/bpmn-process-definitions",
                requestBodyAsString = """
                    {
                        "filename": "$it",
                        "content": "${readResourceFile(it)}"
                    }
                """.trimIndent(),
                testUser = BEHEERDER_1
            ).let { response ->
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_CREATED
            }
        }
    }

    private fun createBpmnProcessTaskForms() {
        mapOf(
            BPMN_TEST_PROCESS_DEFINITION_KEY to listOf(
                BPMN_TEST_FORM_RESOURCE_PATH,
                BPMN_SUMMARY_FORM_RESOURCE_PATH
            ),
            BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY to listOf(
                BPMN_TEST_USER_MANAGEMENT_DEFAULT_FORM_RESOURCE_PATH,
                BPMN_TEST_USER_MANAGEMENT_HARDCODED_FORM_RESOURCE_PATH,
                BPMN_TEST_USER_MANAGEMENT_USER_GROUP_SELECTION_FORM_RESOURCE_PATH,
                BPMN_TEST_USER_MANAGEMENT_NEW_ZAAK_DEFAULTS_FORM_RESOURCE_PATH,
                BPMN_TEST_USER_MANAGEMENT_COPY_USER_GROUP_FORM_RESOURCE_PATH
            ),
            BPMN_DOCUMENT_SIGN_PROCESS_DEFINITION_KEY to listOf(
                BPMN_DOCUMENT_SIGN_SELECT_FORM_RESOURCE_PATH,
                BPMN_DOCUMENT_SIGN_SUMMARY_FORM_RESOURCE_PATH
            ),
            BPMN_SUSPEND_RESUME_PROCESS_DEFINITION_KEY to listOf(
                BPMN_SUSPEND_RESUME_SUSPEND_FORM_RESOURCE_PATH,
                BPMN_SUSPEND_RESUME_RESUME_FORM_RESOURCE_PATH,
                BPMN_SUSPEND_RESUME_EXTEND_FORM_RESOURCE_PATH
            ),
            BPMN_PERMISSION_CHECK_PROCESS_DEFINITION_KEY to listOf(
                BPMN_PERMISSION_CHECK_PROCESS_CHOOSE_FORM_RESOURCE_PATH
            )
        ).forEach { (processDefinitionKey, formResourcePaths) ->
            formResourcePaths.forEach { formResourcePath ->
                itestHttpClient.performJSONPostRequest(
                    url = "$ZAC_API_URI/bpmn-process-definitions/$processDefinitionKey/forms",
                    requestBodyAsString = """
                    {
                        "filename": "$formResourcePath",
                        "content": "${readResourceFile(formResourcePath)}"
                    }
                    """.trimIndent(),
                    testUser = BEHEERDER_1
                ).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_CREATED
                }
            }
        }
    }

    private fun readResourceFile(resourcePath: String): String =
        Thread.currentThread().contextClassLoader.getResource(
            resourcePath
        )?.let {
            File(it.path)
        }!!.readText(Charsets.UTF_8).replace("\"", "\\\"").replace("\n", "\\n")

    @Suppress("LongMethod")
    private fun createZaaktypeConfigurations() {
        zacClient.createZaaktypeBpmnConfiguration(
            zaakTypeUuid = ZAAKTYPE_BPMN_TEST_1_UUID,
            zaakTypeDescription = ZAAKTYPE_BPMN_TEST_1_DESCRIPTION,
            bpmnProcessDefinitionKey = BPMN_TEST_PROCESS_DEFINITION_KEY,
            productaanvraagType = ZAAKTYPE_BPMN_TEST_1_PRODUCTAANVRAAG_TYPE,
            defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
            defaultBehandelaarId = BEHANDELAAR_1.username,
            testUser = BEHEERDER_1,
            nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
        zacClient.createZaaktypeBpmnConfiguration(
            zaakTypeUuid = ZAAKTYPE_BPMN_TEST_2_UUID,
            zaakTypeDescription = ZAAKTYPE_BPMN_TEST_2_DESCRIPTION,
            bpmnProcessDefinitionKey = BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY,
            productaanvraagType = ZAAKTYPE_BPMN_TEST_2_PRODUCTAANVRAAG_TYPE,
            defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
            defaultBehandelaarId = BEHANDELAAR_1.username,
            testUser = BEHEERDER_1,
            nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_2_RESULTAATTYPE_AFGEBROKEN_UUID
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
        zacClient.createZaaktypeBpmnConfiguration(
            zaakTypeUuid = ZAAKTYPE_BPMN_TEST_3_UUID,
            zaakTypeDescription = ZAAKTYPE_BPMN_TEST_3_DESCRIPTION,
            bpmnProcessDefinitionKey = BPMN_DOCUMENT_SIGN_PROCESS_DEFINITION_KEY,
            productaanvraagType = ZAAKTYPE_BPMN_TEST_3_PRODUCTAANVRAAG_TYPE,
            defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
            defaultBehandelaarId = BEHANDELAAR_1.username,
            testUser = BEHEERDER_1,
            nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_3_RESULTAATTYPE_AFGEBROKEN_UUID
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
        zacClient.createZaaktypeBpmnConfiguration(
            zaakTypeUuid = ZAAKTYPE_BPMN_TEST_4_UUID,
            zaakTypeDescription = ZAAKTYPE_BPMN_TEST_4_DESCRIPTION,
            bpmnProcessDefinitionKey = BPMN_SUSPEND_RESUME_PROCESS_DEFINITION_KEY,
            productaanvraagType = ZAAKTYPE_BPMN_TEST_4_PRODUCTAANVRAAG_TYPE,
            defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
            defaultBehandelaarId = BEHANDELAAR_1.username,
            testUser = BEHEERDER_1,
            nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_4_RESULTAATTYPE_AFGEBROKEN_UUID
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
        }
        zacClient.createZaaktypeBpmnConfiguration(
            zaakTypeUuid = ZAAKTYPE_BPMN_TEST_5_UUID,
            zaakTypeDescription = ZAAKTYPE_BPMN_TEST_5_DESCRIPTION,
            bpmnProcessDefinitionKey = BPMN_PERMISSION_CHECK_PROCESS_DEFINITION_KEY,
            productaanvraagType = ZAAKTYPE_BPMN_TEST_5_PRODUCTAANVRAAG_TYPE,
            defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
            defaultBehandelaarId = BEHANDELAAR_1.username,
            testUser = BEHEERDER_1,
            nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_5_RESULTAATTYPE_AFGEBROKEN_UUID
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
        }
        zacClient.createZaaktypeCmmnConfiguration(
            zaakTypeIdentificatie = ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE,
            zaakTypeUuid = ZAAKTYPE_CMMN_TEST_1_UUID,
            zaakTypeDescription = ZAAKTYPE_CMMN_TEST_1_DESCRIPTION,
            productaanvraagType = PRODUCTAANVRAAG_TYPE_3,
            testUser = BEHEERDER_1
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
        zacClient.createZaaktypeCmmnConfiguration(
            zaakTypeIdentificatie = ZAAKTYPE_CMMN_TEST_2_IDENTIFICATIE,
            zaakTypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            zaakTypeDescription = ZAAKTYPE_CMMN_TEST_2_DESCRIPTION,
            productaanvraagType = PRODUCTAANVRAAG_TYPE_2,
            fatalDateWarningWindow = 1,
            testUser = BEHEERDER_1
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
        zacClient.createZaaktypeCmmnConfiguration(
            zaakTypeIdentificatie = ZAAKTYPE_CMMN_TEST_3_IDENTIFICATIE,
            zaakTypeUuid = ZAAKTYPE_CMMN_TEST_3_UUID,
            zaakTypeDescription = ZAAKTYPE_CMMN_TEST_3_DESCRIPTION,
            productaanvraagType = PRODUCTAANVRAAG_TYPE_1,
            testUser = BEHEERDER_1
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
        // beware that the required SmartDocuments template mapping data must be available
        // in our SmartDocuments WireMock setup for this zaaktype
        createZaaktypeSmartDocumentsTemplateMappings(ZAAKTYPE_CMMN_TEST_3_UUID)
        zacClient.createZaaktypeCmmnConfiguration(
            zaakTypeIdentificatie = ZAAKTYPE_CMMN_TEST_4_IDENTIFICATIE,
            zaakTypeUuid = ZAAKTYPE_CMMN_TEST_4_UUID,
            zaakTypeDescription = ZAAKTYPE_CMMN_TEST_4_DESCRIPTION,
            productaanvraagType = PRODUCTAANVRAAG_TYPE_4,
            testUser = BEHEERDER_1
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
    }

    fun createZaaktypeSmartDocumentsTemplateMappings(zaaktypeUuid: UUID) {
        val smartDocumentsZaakafhandelParametersUrl = "$ZAC_API_URI/zaakafhandelparameters/" +
            "$zaaktypeUuid/smartdocuments-templates-mapping"
        val response = itestHttpClient.performJSONPostRequest(
            url = smartDocumentsZaakafhandelParametersUrl,
            requestBodyAsString = SMART_DOCUMENTS_TEMPLATE_MAPPINGS,
            testUser = BEHEERDER_1
        )
        logger.info { "Response: ${response.bodyAsString}" }
        response.code shouldBe HTTP_NO_CONTENT
    }
}
