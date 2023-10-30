/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package io.kotest.provided

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.matchers.shouldBe
import khttp.requests.GenericRequest.Companion.DEFAULT_FORM_HEADERS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.lifely.zac.itest.config.ZACContainer
import nl.lifely.zac.itest.config.getTestContainersDockerNetwork
import org.slf4j.Logger
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.io.File

private val logger = KotlinLogging.logger {}

const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID = "448356ff-dcfb-4504-9501-7fe929077c4f"
const val PRODUCT_AANVRAAG_TYPE_TERUG_BEL_NOTITIIE = "terugbelnotitie"
const val KEYCLOAK_HOSTNAME_URL = "http://localhost:8081"
const val KEYCLOAK_REALM = "zaakafhandelcomponent"
const val KEYCLOAK_CLIENT = "zaakafhandelcomponent"
const val KEYCLOAK_CLIENT_SECRET = "keycloakZaakafhandelcomponentClientSecret"
const val OBJECTS_API_HOSTNAME_URL = "http://objecten-api.local:8000"

object ProjectConfig : AbstractProjectConfig() {
    private const val ZAC_DATABASE_CONTAINER = "zac-database"
    private const val ZAC_DATABASE_PORT = 5432
    private const val TWENTY_SECONDS = 20_000L

    private lateinit var dockerComposeContainer: ComposeContainer
    lateinit var zacContainer: ZACContainer
    lateinit var accessToken: String

    @Suppress("UNCHECKED_CAST")
    override suspend fun beforeProject() {
        try {
            deleteLocalDockerVolumeData()

            dockerComposeContainer = ComposeContainer(File("docker-compose.yaml"))
                .withLocalCompose(true)
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
            dockerComposeContainer.start()

            val zacDatabaseContainer =
                dockerComposeContainer.getContainerByServiceName(ZAC_DATABASE_CONTAINER).get()

            zacContainer = ZACContainer(
                postgresqlHostAndPort = "$ZAC_DATABASE_CONTAINER:$ZAC_DATABASE_PORT",
                // run ZAC container in same Docker network as Docker Compose, so we can access the
                // other containers internally
                network = zacDatabaseContainer.containerInfo.networkSettings.networks.keys.first()
                    .let { getTestContainersDockerNetwork(it) }
            )

            // Wait for a while to give the ZAC database container time to start.
            // We would like to wait more explicitly but so far cannot get the TestContainers
            // wait strategies to work in our Docker Compose context for some reason.
            logger.info { "Waiting a while to be sure the ZAC database has finished starting up" }
            withContext(Dispatchers.IO) {
                Thread.sleep(TWENTY_SECONDS)
            }
            logger.info { "Started ZAC Docker Compose containers" }

            zacContainer.start()
            accessToken = requestAccessTokenFromKeycloak()
            createZaakAfhandelParameters()
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Failed to start Docker containers" }
            dockerComposeContainer.stop()
        }
    }

    override suspend fun afterProject() {
        zacContainer.stop()
        dockerComposeContainer.stop()
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

    private fun requestAccessTokenFromKeycloak() =
        khttp.post(
            url = "$KEYCLOAK_HOSTNAME_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token",
            headers = DEFAULT_FORM_HEADERS,
            data = mapOf(
                "client_id" to KEYCLOAK_CLIENT,
                "grant_type" to "password",
                "username" to "testuser1",
                "password" to "testuser1",
                "client_secret" to KEYCLOAK_CLIENT_SECRET
            )
        ).jsonObject.getString("access_token")

    @Suppress("LongMethod")
    private fun createZaakAfhandelParameters() {
        logger.info {
            "Creating zaakafhandelparameters in ZAC for zaaktype with UUID: $ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
        }

        khttp.put(
            url = "${zacContainer.apiUrl}/zaakafhandelParameters",
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $accessToken"
            ),
            data = "{\n" +
                "  \"humanTaskParameters\": [\n" +
                "    {\n" +
                "      \"planItemDefinition\": {\n" +
                "        \"defaultFormulierDefinitie\": \"AANVULLENDE_INFORMATIE\",\n" +
                "        \"id\": \"AANVULLENDE_INFORMATIE\",\n" +
                "        \"naam\": \"Aanvullende informatie\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      \"defaultGroepId\": null,\n" +
                "      \"formulierDefinitieId\": \"AANVULLENDE_INFORMATIE\",\n" +
                "      \"referentieTabellen\": [],\n" +
                "      \"actief\": true,\n" +
                "      \"doorlooptijd\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"planItemDefinition\": {\n" +
                "        \"defaultFormulierDefinitie\": \"GOEDKEUREN\",\n" +
                "        \"id\": \"GOEDKEUREN\",\n" +
                "        \"naam\": \"Goedkeuren\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      \"defaultGroepId\": null,\n" +
                "      \"formulierDefinitieId\": \"GOEDKEUREN\",\n" +
                "      \"referentieTabellen\": [],\n" +
                "      \"actief\": true,\n" +
                "      \"doorlooptijd\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"planItemDefinition\": {\n" +
                "        \"defaultFormulierDefinitie\": \"ADVIES\",\n" +
                "        \"id\": \"ADVIES_INTERN\",\n" +
                "        \"naam\": \"Advies intern\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      \"defaultGroepId\": null,\n" +
                "      \"formulierDefinitieId\": \"ADVIES\",\n" +
                "      \"referentieTabellen\": [\n" +
                "        {\n" +
                "          \"veld\": \"ADVIES\",\n" +
                "          \"tabel\": {\n" +
                "            \"aantalWaarden\": 5,\n" +
                "            \"code\": \"ADVIES\",\n" +
                "            \"id\": 1,\n" +
                "            \"naam\": \"Advies\",\n" +
                "            \"systeem\": true\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"actief\": true,\n" +
                "      \"doorlooptijd\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"planItemDefinition\": {\n" +
                "        \"defaultFormulierDefinitie\": \"EXTERN_ADVIES_VASTLEGGEN\",\n" +
                "        \"id\": \"ADVIES_EXTERN\",\n" +
                "        \"naam\": \"Advies extern\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      \"defaultGroepId\": null,\n" +
                "      \"formulierDefinitieId\": \"EXTERN_ADVIES_VASTLEGGEN\",\n" +
                "      \"referentieTabellen\": [],\n" +
                "      \"actief\": true,\n" +
                "      \"doorlooptijd\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"planItemDefinition\": {\n" +
                "        \"defaultFormulierDefinitie\": \"DOCUMENT_VERZENDEN_POST\",\n" +
                "        \"id\": \"DOCUMENT_VERZENDEN_POST\",\n" +
                "        \"naam\": \"Document verzenden\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      \"defaultGroepId\": null,\n" +
                "      \"formulierDefinitieId\": \"DOCUMENT_VERZENDEN_POST\",\n" +
                "      \"referentieTabellen\": [],\n" +
                "      \"actief\": true,\n" +
                "      \"doorlooptijd\": null\n" +
                "    }\n" +
                "  ],\n" +
                "  \"mailtemplateKoppelingen\": [],\n" +
                "  \"userEventListenerParameters\": [\n" +
                "    {\n" +
                "      \"id\": \"INTAKE_AFRONDEN\",\n" +
                "      \"naam\": \"Intake afronden\",\n" +
                "      \"toelichting\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"ZAAK_AFHANDELEN\",\n" +
                "      \"naam\": \"Zaak afhandelen\",\n" +
                "      \"toelichting\": null\n" +
                "    }\n" +
                "  ],\n" +
                "  \"valide\": false,\n" +
                "  \"zaakAfzenders\": [],\n" +
                "  \"zaakbeeindigParameters\": [],\n" +
                "  \"zaaktype\": {\n" +
                "    \"beginGeldigheid\": \"2023-09-21\",\n" +
                "    \"doel\": \"Melding evenement organiseren behandelen\",\n" +
                "    \"identificatie\": \"melding-evenement-organiseren-behandelen\",\n" +
                "    \"nuGeldig\": true,\n" +
                "    \"omschrijving\": \"Melding evenement organiseren behandelen\",\n" +
                "    \"servicenorm\": false,\n" +
                "    \"uuid\": \"448356ff-dcfb-4504-9501-7fe929077c4f\",\n" +
                "    \"versiedatum\": \"2023-09-21\",\n" +
                "    \"vertrouwelijkheidaanduiding\": \"openbaar\"\n" +
                "  },\n" +
                "  \"intakeMail\": \"BESCHIKBAAR_UIT\",\n" +
                "  \"afrondenMail\": \"BESCHIKBAAR_UIT\",\n" +
                "  \"caseDefinition\": {\n" +
                "    \"humanTaskDefinitions\": [\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"AANVULLENDE_INFORMATIE\",\n" +
                "        \"id\": \"AANVULLENDE_INFORMATIE\",\n" +
                "        \"naam\": \"Aanvullende informatie\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"GOEDKEUREN\",\n" +
                "        \"id\": \"GOEDKEUREN\",\n" +
                "        \"naam\": \"Goedkeuren\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"ADVIES\",\n" +
                "        \"id\": \"ADVIES_INTERN\",\n" +
                "        \"naam\": \"Advies intern\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"EXTERN_ADVIES_VASTLEGGEN\",\n" +
                "        \"id\": \"ADVIES_EXTERN\",\n" +
                "        \"naam\": \"Advies extern\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"DOCUMENT_VERZENDEN_POST\",\n" +
                "        \"id\": \"DOCUMENT_VERZENDEN_POST\",\n" +
                "        \"naam\": \"Document verzenden\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"key\": \"melding-klein-evenement\",\n" +
                "    \"naam\": \"Melding klein evenement\",\n" +
                "    \"userEventListenerDefinitions\": [\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"DEFAULT_TAAKFORMULIER\",\n" +
                "        \"id\": \"INTAKE_AFRONDEN\",\n" +
                "        \"naam\": \"Intake afronden\",\n" +
                "        \"type\": \"USER_EVENT_LISTENER\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"DEFAULT_TAAKFORMULIER\",\n" +
                "        \"id\": \"ZAAK_AFHANDELEN\",\n" +
                "        \"naam\": \"Zaak afhandelen\",\n" +
                "        \"type\": \"USER_EVENT_LISTENER\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"domein\": null,\n" +
                "  \"defaultGroepId\": \"test-group-a\",\n" +
                "  \"defaultBehandelaarId\": null,\n" +
                "  \"einddatumGeplandWaarschuwing\": null,\n" +
                "  \"uiterlijkeEinddatumAfdoeningWaarschuwing\": null,\n" +
                "  \"productaanvraagtype\": \"$PRODUCT_AANVRAAG_TYPE_TERUG_BEL_NOTITIIE\",\n" +
                "  \"zaakNietOntvankelijkResultaattype\": {\n" +
                "    \"archiefNominatie\": \"VERNIETIGEN\",\n" +
                "    \"archiefTermijn\": \"5 jaren\",\n" +
                "    \"besluitVerplicht\": false,\n" +
                "    \"id\": \"dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6\",\n" +
                "    \"naam\": \"Geweigerd\",\n" +
                "    \"naamGeneriek\": \"Geweigerd\",\n" +
                "    \"toelichting\": \"Het door het orgaan behandelen van een aanvraag, melding of verzoek om " +
                "toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen\",\n" +
                "    \"vervaldatumBesluitVerplicht\": false\n" +
                "  }\n" +
                "}\n"
        ).apply {
            logger.info { "response: $text" }
            // check contents
            statusCode shouldBe HttpStatus.SC_OK
        }
    }
}
