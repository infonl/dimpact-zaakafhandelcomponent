/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.ItestConfiguration.ADDITIONAL_ALLOWED_FILE_TYPES
import nl.info.zac.itest.config.ItestConfiguration.BAG_MOCK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.BRP_PROTOCOLLERING_ICONNECT
import nl.info.zac.itest.config.ItestConfiguration.DOMEIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DOMEIN_TEST_2
import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION
import nl.info.zac.itest.config.ItestConfiguration.KEYCLOAK_HEALTH_READY_URL
import nl.info.zac.itest.config.ItestConfiguration.KVK_MOCK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OFFICE_CONVERTER_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.PABC_API_KEY
import nl.info.zac.itest.config.ItestConfiguration.PABC_CLIENT_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_1
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_2
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_3
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_DOMEIN_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_DOMEIN_INDEX
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_DOMEIN_NAME
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_MOCK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.SMTP_SERVER_PORT
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_DEFAULT_DOCKER_IMAGE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_HEALTH_READY_URL
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import okhttp3.Headers
import org.json.JSONArray
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
    private val zacClient = ZacClient()
    private val zacDockerImage = System.getProperty("zacDockerImage") ?: ZAC_DEFAULT_DOCKER_IMAGE

    // All variables below have to be overridable in the docker-compose.yaml file
    private val dockerComposeOverrideEnvironment = mapOf(
        "APP_ENV" to "itest",
        "AUTH_SSL_REQUIRED" to "none",
        "ADDITIONAL_ALLOWED_FILE_TYPES" to ADDITIONAL_ALLOWED_FILE_TYPES,
        "BAG_API_CLIENT_MP_REST_URL" to "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/",
        "FEATURE_FLAG_BPMN_SUPPORT" to "true",
        "FEATURE_FLAG_PABC_INTEGRATION" to FEATURE_FLAG_PABC_INTEGRATION.toString(),
        "KVK_API_CLIENT_MP_REST_URL" to KVK_MOCK_BASE_URI,
        "OFFICE_CONVERTER_CLIENT_MP_REST_URL" to OFFICE_CONVERTER_BASE_URI,
        "PABC_API_CLIENT_MP_REST_URL" to PABC_CLIENT_BASE_URI,
        "PABC_API_KEY" to PABC_API_KEY,
        "BRP_PROTOCOLLERING" to BRP_PROTOCOLLERING_ICONNECT,
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
                ).let { response ->
                    response.code shouldBe HTTP_OK
                    JSONObject(response.bodyAsString).getString("status") shouldBe "UP"
                }
            }
            logger.info { "ZAC is healthy" }
            createTestSetupData()
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Failed to start Docker Compose containers" }
            dockerComposeContainer.stop()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun afterProject() {
        // stop ZAC Docker Container gracefully to give JaCoCo a change to generate the code coverage report
        dockerComposeContainer.getContainerByServiceName(ZAC_CONTAINER_SERVICE_NAME).getOrNull()?.let { zacContainer ->
            logger.info { "Stopping ZAC Docker container" }
            zacContainer.dockerClient
                .stopContainerCmd(zacContainer.containerId)
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
        logger.info { "Using Docker Compose environment variables: $dockerComposeOverrideEnvironment" }

        return ComposeContainer(File("docker-compose.yaml"))
            .withRemoveVolumes(System.getenv("REMOVE_DOCKER_COMPOSE_VOLUMES")?.toBoolean() ?: true)
            .withEnv(dockerComposeOverrideEnvironment)
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
                "pabc-api",
                Wait.forLogMessage(".* Application started.*", 1)
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
     * Creates overal test setup data in ZAC, required for running the integration tests.
     */
    private fun createTestSetupData() {
        authenticate(BEHEERDER_ELK_ZAAKTYPE)
        createDomainReferenceTableData()
        createZaaktypeConfigurations()
    }

    private fun createDomainReferenceTableData() {
        val domeinReferenceTableId = itestHttpClient.performGetRequest(
            "$ZAC_API_URI/referentietabellen"
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
            JSONArray(responseBody)
                .map { it as JSONObject }
                .firstOrNull { it.getString("code") == REFERENCE_TABLE_DOMEIN_CODE }
                ?.getInt("id")
                ?: error("Reference table with code '$REFERENCE_TABLE_DOMEIN_CODE' not found")

        }
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/referentietabellen/$domeinReferenceTableId",
            requestBodyAsString = """
                  {
                        "aantalWaarden" : 0,
                        "code" : "$REFERENCE_TABLE_DOMEIN_CODE",
                        "id" : $domeinReferenceTableId,
                        "naam" : "$REFERENCE_TABLE_DOMEIN_NAME",
                        "systeem" : true,
                        "waarden": [ { "naam" : "$DOMEIN_TEST_1" } ]
                    }
            """.trimIndent()
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
            with(JSONObject(responseBody).toString()) {
                shouldEqualJsonIgnoringExtraneousFields(
                    """
                        {
                            "code": "$REFERENCE_TABLE_DOMEIN_CODE",
                            "naam": "$REFERENCE_TABLE_DOMEIN_NAME",
                            "systeem": true,
                            "aantalWaarden": 1,
                            "waarden": [
                                { "naam": "$DOMEIN_TEST_1", "systemValue": false }                               
                            ]
                        }
                    """.trimIndent()
                )
                shouldContainJsonKey("id")
            }
        }
    }

    private fun createZaaktypeConfigurations() {
        zacClient.createZaaktypeBpmnConfiguration(
            zaakTypeUuid = ZAAKTYPE_BPMN_TEST_UUID,
            zaakTypeDescription = ZAAKTYPE_BPMN_TEST_DESCRIPTION,
            bpmnProcessDefinitionKey = BPMN_TEST_PROCESS_DEFINITION_KEY,
            productaanvraagType = ZAAKTYPE_BPMN_PRODUCTAANVRAAG_TYPE,
            defaultGroupName = BEHANDELAARS_DOMAIN_TEST_1.description
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
        zacClient.createZaaktypeCmmnConfiguration(
            zaakTypeIdentificatie = ZAAKTYPE_TEST_1_IDENTIFICATIE,
            zaakTypeUuid = ZAAKTYPE_TEST_1_UUID,
            zaakTypeDescription = ZAAKTYPE_TEST_1_DESCRIPTION,
            productaanvraagType = PRODUCTAANVRAAG_TYPE_3,
            // Note that these domains are no longer used in the new IAM architecture and will be removed in the future
            domein = DOMEIN_TEST_2
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
        zacClient.createZaaktypeCmmnConfiguration(
            zaakTypeIdentificatie = ZAAKTYPE_TEST_2_IDENTIFICATIE,
            zaakTypeUuid = ZAAKTYPE_TEST_2_UUID,
            zaakTypeDescription = ZAAKTYPE_TEST_2_DESCRIPTION,
            productaanvraagType = PRODUCTAANVRAAG_TYPE_2,
            // Note that these domains are no longer used in the new IAM architecture and will be removed in the future
            domein = DOMEIN_TEST_1
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
        zacClient.createZaaktypeCmmnConfiguration(
            zaakTypeIdentificatie = ZAAKTYPE_TEST_3_IDENTIFICATIE,
            zaakTypeUuid = ZAAKTYPE_TEST_3_UUID,
            zaakTypeDescription = ZAAKTYPE_TEST_3_DESCRIPTION,
            productaanvraagType = PRODUCTAANVRAAG_TYPE_1,
            automaticEmailConfirmationSender = "GEMEENTE"
        ).let { response ->
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK
        }
    }
}
