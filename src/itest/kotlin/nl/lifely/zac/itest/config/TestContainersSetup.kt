/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.config

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_CLIENT
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_CLIENT_SECRET
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_REALM
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECTS_API_HOSTNAME_URL
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import org.json.JSONObject
import org.slf4j.Logger
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

private val logger = KotlinLogging.logger {}

object ItestConfiguration {
    // These values need to correspond to the test data in the databases of the various services
    // used in the entire integration test flow such as: Keycloak, Objecten, Objecttypen, Open Zaak, ZAC.
    const val KEYCLOAK_HOSTNAME_URL = "http://localhost:8081"
    const val KEYCLOAK_REALM = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT_SECRET = "keycloakZaakafhandelcomponentClientSecret"
    const val OBJECT_PRODUCTAANVRAAG_UUID = "9dbed186-89ca-48d7-8c6c-f9995ceb8e27"
    const val OBJECTS_API_HOSTNAME_URL = "http://objecten-api.local:8000"
    const val OBJECTTYPE_UUID_PRODUCTAANVRAAG_DENHAAG = "021f685e-9482-4620-b157-34cd4003da6b"
    const val OPEN_NOTIFICATIONS_API_SECRET_KEY = "openNotificatiesApiSecretKey"
    const val PRODUCT_AANVRAAG_TYPE = "productaanvraag"
    const val ZAAK_1_IDENTIFICATION = "ZAAK-2023-0000000001"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID = "448356ff-dcfb-4504-9501-7fe929077c4f"
}

class ZACContainer(
    private val postgresqlHostAndPort: String,
    private val network: Network
) {
    companion object {
        const val CONTAINER_PORT = 8080
        const val CONTAINER_MANAGEMENT_PORT = 9990
        const val THREE_MINUTES = 3L
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
            "OBJECTS_API_CLIENT_MP_REST_URL" to OBJECTS_API_HOSTNAME_URL,
            "OBJECTS_API_TOKEN" to "182c13e2209161852c53cef53a879f7a2f923430",
            "OBJECTTYPES_API_CLIENT_MP_REST_URL" to "http://objecttypen-api:8000",
            "OBJECTTYPES_API_TOKEN" to "dummyZacObjectTypesToken",
            "OPA_API_CLIENT_MP_REST_URL" to "http://opa:8181",
            "OPEN_FORMS_URL" to "http://localhost:9999", // dummy for now
            "OPEN_NOTIFICATIONS_API_SECRET_KEY" to OPEN_NOTIFICATIONS_API_SECRET_KEY,
            "SD_AUTHENTICATION" to "dummySmartDocumentsAuthentication",
            "SD_CLIENT_MP_REST_URL" to "dummySmartDocumentsClientUrl",
            "SOLR_URL" to "http://solr:8983",
            "VRL_API_CLIENT_MP_REST_URL" to "http://zgw-referentielijsten.local:8000/",
            "ZGW_API_CLIENT_MP_REST_URL" to "http://openzaak.local:8000/",
            "ZGW_API_CLIENTID" to "zac_client",
            "ZGW_API_SECRET" to "openzaakZaakafhandelcomponentClientSecret",
            "ZGW_API_URL_EXTERN" to "http://localhost:8001/"
        )

        @Suppress("UNCHECKED_CAST")
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

// For now, we use the deprecated FixedHostPortGenericContainer because our current Keycloak realm configuration
// is only compatible with the ZAC frontend running on a fixed port (e.g. allowed redirect uris is set to http://localhost:8080).
// This makes it easy to test and troubleshoot the ZAC frontend in a browser while running the integration tests.
private class KGenericContainer(imageName: String) :
    FixedHostPortGenericContainer<KGenericContainer>(imageName)
