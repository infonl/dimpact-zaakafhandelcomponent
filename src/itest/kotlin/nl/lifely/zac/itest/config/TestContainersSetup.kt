/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.config

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.provided.KEYCLOAK_CLIENT
import io.kotest.provided.KEYCLOAK_CLIENT_SECRET
import io.kotest.provided.KEYCLOAK_REALM
import org.json.JSONObject
import org.slf4j.Logger
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

private val logger = KotlinLogging.logger {}

class ZACContainer(
    private val postgresqlHostAndPort: String,
    private val network: Network
) {
    companion object {
        const val CONTAINER_PORT = 8080
        const val CONTAINER_MANAGEMENT_PORT = 9990
        const val THREE_MINUTES = 3L
        const val OPEN_NOTIFICATIONS_API_SECRET_KEY = "openNotificatiesApiSecretKey"
    }

    private val zacDockerImage = System.getProperty("zacDockerImage", DOCKER_IMAGE_ZAC_DEV)
    private lateinit var bagApiClientMpRestUrl: String
    private lateinit var bagApiKey: String
    private lateinit var container: GenericContainer<*>
    lateinit var apiUrl: String
    lateinit var managementUrl: String

    fun start() {
        setVariablesFromEnvironment()
        container = createContainer()

        logger.info { "Starting ZAC Docker image: '$zacDockerImage' using Postgresql JDBC URL: '$postgresqlHostAndPort'" }
        container.start()

        apiUrl = "http://${container.host}:$CONTAINER_PORT/rest"
        managementUrl = "http://${container.host}:$CONTAINER_MANAGEMENT_PORT"
        logger.info { "ZAC Docker container is running and accessible on: $apiUrl and management URL: $managementUrl" }
    }

    fun stop() = if (this::container.isInitialized) container.stop() else Unit

    // these environment variables need to be set or else the container will not start
    private fun setVariablesFromEnvironment() {
        bagApiClientMpRestUrl = System.getenv("BAG_API_CLIENT_MP_REST_URL")
        bagApiKey = System.getenv("BAG_API_KEY")
    }

    @Suppress("LongMethod")
    private fun createContainer(): KGenericContainer {
        val env = mapOf(
            "AUTH_REALM" to KEYCLOAK_REALM,
            "AUTH_RESOURCE" to KEYCLOAK_CLIENT,
            "AUTH_SECRET" to KEYCLOAK_CLIENT_SECRET,
            "AUTH_SERVER" to "http://keycloak:8080",
            "BAG_API_CLIENT_MP_REST_URL" to bagApiClientMpRestUrl,
            "BAG_API_KEY" to bagApiKey,
            "BRP_API_CLIENT_MP_REST_URL" to "http://brpproxy:5000/haalcentraal/api/brp",
            "BRP_API_KEY" to "dummyKey", // not used when using the BRP proxy
            "CONTACTMOMENTEN_API_CLIENT_MP_REST_URL" to "http://openklant:8000/contactmomenten",
            "CONTACTMOMENTEN_API_CLIENTID" to "zac_client",
            "CONTACTMOMENTEN_API_SECRET" to "openklantZaakhandelcomponentClientSecret",
            "CONTEXT_URL" to "http://localhost:8080",
            "DB_HOST" to postgresqlHostAndPort,
            "DB_NAME" to "zac",
            "DB_PASSWORD" to "password",
            "DB_USER" to "zac",
            "GEMEENTE_CODE" to "9999",
            "GEMEENTE_MAIL" to "gemeente-itest@example.com",
            "GEMEENTE_NAAM" to "Gemeente ITest",
            "KLANTEN_API_CLIENT_MP_REST_URL" to "http://openklant:8000/klanten",
            "KLANTEN_API_CLIENTID" to "zac_client",
            "KLANTEN_API_SECRET" to "openklantZaakhandelcomponentClientSecret",
            "KVK_API_CLIENT_MP_REST_URL" to "dummyKvkApiUrl", // dummy for now
            "KVK_API_KEY" to "dummyKvkApiKey",
            "LDAP_DN" to "ou=people,dc=example,dc=org",
            "LDAP_PASSWORD" to "admin",
            "LDAP_URL" to "ldap://openldap:1389",
            "LDAP_USER" to "cn=admin,dc=example,dc=org",
            "MAILJET_API_KEY" to "dummyMailjetApiKey",
            "MAILJET_API_SECRET_KEY" to "dummyMailjetApiSecretKey",
            "MAX_FILE_SIZE_MB" to "80",
            "OFFICE_CONVERTER_CLIENT_MP_REST_URL" to "http://localhost:9999", // dummy for now
            "OBJECTS_API_CLIENT_MP_REST_URL" to "http://objecten-api:8000",
            "OBJECTS_API_TOKEN" to "182c13e2209161852c53cef53a879f7a2f923430",
            "OBJECTTYPES_API_CLIENT_MP_REST_URL" to "http://objecttypen-api:8000",
            "OBJECTTYPES_API_TOKEN" to "dummyZacObjectTypesToken",
            "OPA_API_CLIENT_MP_REST_URL" to "http://opa:8181",
            "OPEN_FORMS_URL" to "http://localhost:9999", // dummy for now
            "OPEN_NOTIFICATIONS_API_SECRET_KEY" to OPEN_NOTIFICATIONS_API_SECRET_KEY,
            "SD_AUTHENTICATION" to "dummySmartDocumentsAuthentication",
            "SD_CLIENT_MP_REST_URL" to "dummySmartDocumentsClientUrl",
            "SOLR_URL" to "http://solr:8983",
            // OpenZaak does not accept internal Docker container hostnames for URLs
            // as workaround we use the default 'host.docker.internal' hostname
            "VRL_API_CLIENT_MP_REST_URL" to "http://host.docker.internal:8020/",
            "ZGW_API_CLIENT_MP_REST_URL" to "http://host.docker.internal:8001/",
            "ZGW_API_CLIENTID" to "zac_client",
            "ZGW_API_SECRET" to "openzaakZaakafhandelcomponentClientSecret",
            "ZGW_API_URL_EXTERN" to "http://localhost:8001/"
        )

        @Suppress("SpreadOperator")
        return KGenericContainer(zacDockerImage)
            .withFixedExposedPort(CONTAINER_PORT, CONTAINER_PORT)
            .withFixedExposedPort(CONTAINER_MANAGEMENT_PORT, CONTAINER_MANAGEMENT_PORT)
            .withEnv(env)
            .withNetwork(network)
            .withLogConsumer(
                Slf4jLogConsumer((logger as DelegatingKLogger<Logger>).underlyingLogger).withPrefix(
                    "ZAC"
                )
            )
            .waitingFor(
                Wait.forLogMessage(".* WildFly Full .* started .*", 1)
            )
            .waitingFor(
                Wait.forHttp("/health/ready")
                    .forPort(CONTAINER_MANAGEMENT_PORT)
                    .forStatusCode(HttpStatus.SC_OK)
                    .forResponsePredicate { response ->
                        JSONObject(response).getString("status") == "UP"
                    }
                    .withStartupTimeout(Duration.ofMinutes(THREE_MINUTES))
            )
    }
}

// for now, we use the deprecated FixedHostPortGenericContainer because our Keycloak configuration
// is only compatible with ZAC running on a fixed port (8080)
// note that this may result in port conflicts
class KGenericContainer(imageName: String) :
    FixedHostPortGenericContainer<KGenericContainer>(imageName)
