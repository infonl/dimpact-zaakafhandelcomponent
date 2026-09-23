/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.annotation.Isolate
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.engine.concurrency.ConcurrencyOrder
import io.kotest.engine.concurrency.SpecExecutionMode
import io.kotest.engine.coroutines.ThreadPerSpecCoroutineContextFactory
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
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
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_1
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_2
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_3
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_4
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
import okhttp3.Headers
import org.json.JSONObject
import org.slf4j.Logger
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.wait.strategy.WaitStrategy
import java.io.File
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.net.SocketException
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Suppress("TooManyFunctions")
class ZacItestProjectConfig : AbstractProjectConfig() {
    companion object {
        private const val DO_NOT_START_DOCKER_COMPOSE_ENV_VAR = "DO_NOT_START_DOCKER_COMPOSE"
        private const val DO_NOT_CREATE_TEST_SETUP_DATA_ENV_VAR = "DO_NOT_CREATE_ITEST_SETUP_DATA"
        private const val TESTCONTAINERS_RYUK_DISABLED_ENV_VAR = "TESTCONTAINERS_RYUK_DISABLED"
        private const val DOCKER_USE_ARM64_CONTAINERS_ENV_VAR = "DOCKER_USE_ARM64_CONTAINERS"
        private const val SPEC_CONCURRENCY_SYSTEM_PROPERTY = "zac.itest.specConcurrency"
        private const val DEFAULT_SPEC_CONCURRENCY = 3
        private const val COMPOSE_ENV_FILE = "src/itest/docker-compose-itest.env"

        private val externallyStartedContainerLookup = eventuallyConfig {
            duration = 3.minutes
            interval = 2.seconds
        }

        private val logger = KotlinLogging.logger {}
        private val itestHttpClient = ItestHttpClient()
        private val zacClient = ZacClient()
        private val zacDockerImage = System.getProperty("zacDockerImage") ?: ZAC_DEFAULT_DOCKER_IMAGE
        private val skipDockerComposeStart = System.getenv(DO_NOT_START_DOCKER_COMPOSE_ENV_VAR)?.toBoolean() ?: false
        private val skipContainerCleanup = System.getenv(TESTCONTAINERS_RYUK_DISABLED_ENV_VAR)?.toBoolean() ?: false
        private val skipTestSetupDataCreation = System.getenv(DO_NOT_CREATE_TEST_SETUP_DATA_ENV_VAR)?.toBoolean() ?: false
        private val specConcurrency = System.getProperty(SPEC_CONCURRENCY_SYSTEM_PROPERTY)?.toInt() ?: DEFAULT_SPEC_CONCURRENCY

        /**
         * The Docker Compose environment of the integration test stack is shared with the CI workflow in
         * [COMPOSE_ENV_FILE]. Only the ZAC image is not known up front, so it is passed as an environment
         * variable, which Docker Compose gives precedence over the env file.
         */
        private val dockerComposeOverrideEnvironment = mapOf("ZAC_DOCKER_IMAGE" to zacDockerImage)

        /**
         * Waiting for these log lines is what tells the integration tests that the containers that have no
         * health check of their own are ready. They are applied to the containers the tests start themselves
         * as well as to the containers of a stack that was started for them.
         */
        private val composeWaitStrategies: Map<String, WaitStrategy> = mapOf(
            "openzaak-app.local" to Wait.forLogMessage(".*spawned uWSGI worker 2.*", 1)
                .withStartupTimeout(3.minutes.toJavaDuration()),
            "pabc-api" to Wait.forLogMessage(".* Application started.*", 1)
                .withStartupTimeout(3.minutes.toJavaDuration()),
            ZAC_CONTAINER_SERVICE_NAME to Wait.forLogMessage(".* WildFly .* started .*", 1)
                .withStartupTimeout(3.minutes.toJavaDuration()),
            "greenmail" to Wait.forLogMessage(".*Starting GreenMail API server.*", 1)
                .withStartupTimeout(2.minutes.toJavaDuration())
        )
    }

    private var dockerComposeContainer: ComposeContainer? = null

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
     * Run the specs concurrently against the shared Docker Compose stack, each spec on its own thread.
     * Specs that touch global state (mail, reindexing, container logs, admin jobs, shared configuration)
     * carry Kotest's `@Isolate` annotation and run one by one before the concurrent specs.
     * The number of concurrent specs can be overridden with the `zac.itest.specConcurrency` system property.
     */
    override val specExecutionMode = SpecExecutionMode.LimitedConcurrency(specConcurrency)

    override val concurrencyOrder = ConcurrencyOrder.IsolateFirst

    override val coroutineDispatcherFactory = ThreadPerSpecCoroutineContextFactory

    /**
     * Purge GreenMail's email store before each isolated spec, so that a spec that asserts on mail sent to
     * a shared mailbox starts from an empty store. Isolated specs run one by one, so this never removes mail
     * that a running spec still needs. A spec that asserts on mail while running concurrently has to use
     * recipient addresses that no other spec uses.
     */
    override val extensions: List<Extension> = listOf(
        ItestTimingReport,
        object : BeforeSpecListener {
            override suspend fun beforeSpec(spec: Spec) {
                if (spec::class.java.isAnnotationPresent(Isolate::class.java)) {
                    logger.info { "Purging GreenMail email store before isolated spec '${spec::class.simpleName}'" }
                    itestHttpClient.performDeleteRequest(url = "$GREENMAIL_API_URI/service")
                }
            }
        }
    )

    override suspend fun beforeProject() {
        ItestTimingReport.markPhase(ItestTimingReport.PHASE_RUN_STARTED)
        logger.info {
            "Starting integration tests with random seed: '$randomOrderSeed' and up to $specConcurrency concurrent specs"
        }
        try {
            if (!skipDockerComposeStart) {
                dockerComposeContainer = createDockerComposeContainer().apply { start() }
                logger.info { "Started ZAC Docker Compose containers" }
            } else {
                logger.warn {
                    "$DO_NOT_START_DOCKER_COMPOSE_ENV_VAR environment variable is set to true, not starting Docker Compose containers"
                }
                waitUntilExternallyStartedContainersAreReady()
            }
            ItestTimingReport.markPhase(ItestTimingReport.PHASE_COMPOSE_STARTED)

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
            ItestTimingReport.markPhase(ItestTimingReport.PHASE_KEYCLOAK_HEALTHY)
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
            ItestTimingReport.markPhase(ItestTimingReport.PHASE_ZAC_HEALTHY)
            logger.info { "ZAC is healthy" }
            if (!skipTestSetupDataCreation) {
                createTestSetupData()
                ItestTimingReport.markPhase(ItestTimingReport.PHASE_TEST_SETUP_DATA_CREATED)
            }
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Failed to start Docker Compose containers" }
            dockerComposeContainer?.stop()
        }
    }

    /**
     * Applies the wait strategies of the stack to the containers of a stack that was started outside the
     * integration tests, which Testcontainers does not know about. The containers are looked up in a retry
     * loop because Docker Compose only creates a container once the containers it depends on are healthy.
     */
    private suspend fun waitUntilExternallyStartedContainersAreReady() =
        composeWaitStrategies.forEach { (serviceName, waitStrategy) ->
            logger.info { "Waiting until the externally started '$serviceName' container is ready" }
            eventually(externallyStartedContainerLookup) {
                DockerComposeStack.readContainerOfService(serviceName)
            }.let(waitStrategy::waitUntilReady)
        }

    override suspend fun afterProject() {
        try {
            val composeProjectName = DockerComposeStack.projectName
            composeProjectName?.let {
                ItestTimingReport.collectContainerTimings(DockerComposeStack.dockerClient, it)
            }
            if (skipContainerCleanup) {
                logger.warn {
                    "$TESTCONTAINERS_RYUK_DISABLED_ENV_VAR environment variable is set to true, not stopping Docker Compose containers"
                }
                ItestTimingReport.writeReport()
                Runtime.getRuntime().halt(0)
            }

            // stop ZAC Docker Container gracefully to give JaCoCo a change to generate the code coverage report
            DockerComposeStack.findContainerOfService(ZAC_CONTAINER_SERVICE_NAME)?.let { zacContainer ->
                logger.info { "Stopping ZAC Docker container" }
                DockerComposeStack.dockerClient
                    .stopContainerCmd(zacContainer.containerId)
                    .withTimeout(30.seconds.inWholeSeconds.toInt())
                    .exec()
                logger.info { "Stopped ZAC Docker container" }
            }
            ItestTimingReport.markPhase(ItestTimingReport.PHASE_ZAC_STOPPED)
            // the other containers hold no state worth preserving, so kill them instead of waiting
            // for each of them to handle a stop signal, and then let Docker Compose remove them
            composeProjectName?.let(DockerComposeStack::killRunningContainers)
            dockerComposeContainer?.withOptions("--profile itest")?.stop()
            ItestTimingReport.markPhase(ItestTimingReport.PHASE_COMPOSE_REMOVED)
        } finally {
            ItestTimingReport.writeReport()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createDockerComposeContainer(): ComposeContainer {
        logger.info { "Using Docker Compose environment variables: $dockerComposeOverrideEnvironment" }

        val composeFiles: MutableList<File> = mutableListOf(File("docker-compose.yaml"))
        System.getenv(DOCKER_USE_ARM64_CONTAINERS_ENV_VAR)
            ?.takeIf { it.isNotBlank() }
            ?.let {
                composeFiles.add(File("docker-compose.arm64-override.yaml"))
                logger.info { "Using arm64 containers" }
            }

        return ComposeContainer(TESTCONTAINERS_COMPOSE_PROJECT_PREFIX, composeFiles)
            .withEnv(dockerComposeOverrideEnvironment)
            // do not pull images first because this will cause _all_ Docker images to be pulled,
            // and not just the ones we need for our profiles
            .withPull(false)
            .withOptions(
                "--profile zac",
                "--profile itest",
                "--env-file $COMPOSE_ENV_FILE"
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
                "openzaak-app.local",
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
            .apply {
                composeWaitStrategies.forEach { (serviceName, waitStrategy) -> waitingFor(serviceName, waitStrategy) }
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
        }!!.readText(Charsets.UTF_8)
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

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
