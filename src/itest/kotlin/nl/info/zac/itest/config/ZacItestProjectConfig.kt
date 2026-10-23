/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

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
import nl.info.zac.itest.config.ItestConfiguration.ZAC_DEFAULT_DOCKER_IMAGE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_HEALTH_READY_URL
import okhttp3.Headers
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.net.SocketException
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
class ZacItestProjectConfig : AbstractProjectConfig() {
    companion object {
        private const val USE_RUNNING_STACK_ENV_VAR = "ZAC_ITEST_USE_RUNNING_STACK"
        private const val KEEP_STACK_ENV_VAR = "ZAC_ITEST_KEEP_STACK"
        private const val SPEC_CONCURRENCY_SYSTEM_PROPERTY = "zac.itest.specConcurrency"
        private const val DEFAULT_SPEC_CONCURRENCY = 3

        private val logger = KotlinLogging.logger {}
        private val itestHttpClient = ItestHttpClient()
        private val zacClient = ZacClient()
        private val zacDockerImage = System.getProperty("zacDockerImage") ?: ZAC_DEFAULT_DOCKER_IMAGE
        private val shouldUseRunningStack = System.getenv(USE_RUNNING_STACK_ENV_VAR)?.toBoolean() ?: false
        private val shouldKeepStack = System.getenv(KEEP_STACK_ENV_VAR)?.toBoolean() ?: false
        private val specConcurrency = System.getProperty(SPEC_CONCURRENCY_SYSTEM_PROPERTY)?.toInt() ?: DEFAULT_SPEC_CONCURRENCY
        private val jacocoExecFile = File("build/jacoco/itest/jacoco-report/jacoco-it.exec")
    }

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
     * Run the specs concurrently against the shared kind stack, each spec on its own thread.
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
        if (shouldUseRunningStack) {
            logger.warn {
                "$USE_RUNNING_STACK_ENV_VAR environment variable is set to true, using the running kind cluster " +
                    "'${ItestKindStack.clusterName}'"
            }
        } else {
            ItestKindStack.start(zacDockerImage)
            ItestTimingReport.markPhase(ItestTimingReport.PHASE_KIND_STACK_STARTED)
            logger.info { "Started the kind cluster '${ItestKindStack.clusterName}'" }
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
        if (!shouldUseRunningStack) {
            createTestSetupData()
            ItestTimingReport.markPhase(ItestTimingReport.PHASE_TEST_SETUP_DATA_CREATED)
        }
    }

    override suspend fun afterProject() {
        try {
            if (shouldUseRunningStack) {
                logger.warn {
                    "$USE_RUNNING_STACK_ENV_VAR environment variable is set to true, leaving the kind cluster as it is"
                }
                return
            }
            ItestTimingReport.collectPodTimings()
            ItestKindStack.dumpCoverage(jacocoExecFile)
            ItestTimingReport.markPhase(ItestTimingReport.PHASE_COVERAGE_COLLECTED)
            logger.info { "Wrote the coverage of ZAC to '${jacocoExecFile.path}'" }
            if (shouldKeepStack) {
                logger.warn {
                    "$KEEP_STACK_ENV_VAR environment variable is set to true, not deleting the kind cluster " +
                        "'${ItestKindStack.clusterName}'"
                }
            } else {
                ItestKindStack.delete()
                ItestTimingReport.markPhase(ItestTimingReport.PHASE_KIND_STACK_DELETED)
            }
        } finally {
            ItestTimingReport.writeReport()
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
        checkNotNull(Thread.currentThread().contextClassLoader.getResource(resourcePath)) {
            "Resource '$resourcePath' not found"
        }.let { File(it.path) }.readText(Charsets.UTF_8)
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
