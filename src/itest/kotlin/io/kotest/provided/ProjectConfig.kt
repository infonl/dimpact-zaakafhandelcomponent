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
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_CLIENT
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_CLIENT_SECRET
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_HOSTNAME_URL
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_REALM
import nl.lifely.zac.itest.config.ItestConfiguration.PRODUCT_AANVRAAG_TYPE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_MANAGEMENT_URI
import org.awaitility.kotlin.await
import org.slf4j.Logger
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

object ProjectConfig : AbstractProjectConfig() {
    private const val THREE_MINUTES = 3L
    private const val TEN_SECONDS = 10L

    private lateinit var dockerComposeContainer: ComposeContainer

    lateinit var accessToken: String

    @Suppress("UNCHECKED_CAST")
    override suspend fun beforeProject() {
        try {
            deleteLocalDockerVolumeData()

            dockerComposeContainer = ComposeContainer(File("docker-compose.yaml"))
                .withLocalCompose(true)
                .withOptions(
                    "--profile zac",
                    "--env-file .env.itest"
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
                    "zac",
                    Slf4jLogConsumer((logger as DelegatingKLogger<Logger>).underlyingLogger).withPrefix(
                        "ZAC"
                    )
                )
                .waitingFor(
                    "openzaak.local",
                    Wait.forLogMessage(".*spawned uWSGI worker 2.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(THREE_MINUTES))
                )
                .waitingFor(
                    "zac",
                    Wait.forLogMessage(".* WildFly Full .* started .*", 1)
                        .withStartupTimeout(Duration.ofMinutes(THREE_MINUTES))
                )
            dockerComposeContainer.start()
            logger.info { "Started ZAC Docker Compose containers" }
            logger.info { "Waiting until ZAC is healthy by calling the health endpoint and checking the response" }
            await.atMost(TEN_SECONDS, TimeUnit.SECONDS)
                .until {
                    khttp.get(
                        url = "${ZAC_MANAGEMENT_URI}/health/ready",
                        headers = mapOf(
                            "Content-Type" to "application/json",
                        )
                    ).let {
                        it.statusCode == HttpStatus.SC_OK && it.jsonObject.getString("status") == "UP"
                    }
                }
            accessToken = requestAccessTokenFromKeycloak()
            createZaakAfhandelParameters()
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Failed to start Docker containers" }
            dockerComposeContainer.stop()
        }
    }

    override suspend fun afterProject() {
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
            url = "${ZAC_API_URI}/zaakafhandelParameters",
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
                "    \"identificatie\": \"$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE\",\n" +
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
                "  \"productaanvraagtype\": \"$PRODUCT_AANVRAAG_TYPE\",\n" +
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
