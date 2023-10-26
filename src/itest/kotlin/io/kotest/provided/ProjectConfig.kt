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

const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID = "021f685e-9482-4620-b157-34cd4003da6b"

object ProjectConfig : AbstractProjectConfig() {
    private const val ZAC_DATABASE_CONTAINER = "zac-database"
    private const val ZAC_DATABASE_PORT = 5432
    private const val TWENTY_SECONDS = 20_000L

    private lateinit var dockerComposeContainer: ComposeContainer
    lateinit var zacContainer: ZACContainer

    @Suppress("UNCHECKED_CAST")
    override suspend fun beforeProject() {
        try {
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

    private fun createZaakAfhandelParameters() {
        logger.info {
            "Creating zaakafhandelparameters in ZAC Docker Container for zaaktype with UUID: $ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
        }

        // TODO: authentication...
        khttp.put(
            url = "${zacContainer.apiUrl}/zaakafhandelParameters",
            headers = mapOf(
                "Content-Type" to "application/json"
            ),
            data = "{\"humanTaskParameters\":[{\"planItemDefinition\":{\"defaultFormulierDefinitie\":\"AANVULLENDE_INFORMATIE\",\"id\":\"AANVULLENDE_INFORMATIE\",\"naam\":\"Aanvullende informatie\",\"type\":\"HUMAN_TASK\"},\"defaultGroepId\":null,\"formulierDefinitieId\":\"AANVULLENDE_INFORMATIE\",\"referentieTabellen\":[],\"actief\":true,\"doorlooptijd\":null},{\"planItemDefinition\":{\"defaultFormulierDefinitie\":\"GOEDKEUREN\",\"id\":\"GOEDKEUREN\",\"naam\":\"Goedkeuren\",\"type\":\"HUMAN_TASK\"},\"defaultGroepId\":null,\"formulierDefinitieId\":\"GOEDKEUREN\",\"referentieTabellen\":[],\"actief\":true,\"doorlooptijd\":null},{\"planItemDefinition\":{\"defaultFormulierDefinitie\":\"ADVIES\",\"id\":\"ADVIES_INTERN\",\"naam\":\"Advies intern\",\"type\":\"HUMAN_TASK\"},\"defaultGroepId\":null,\"formulierDefinitieId\":\"ADVIES\",\"referentieTabellen\":[{\"veld\":\"ADVIES\",\"tabel\":{\"aantalWaarden\":5,\"code\":\"ADVIES\",\"id\":1,\"naam\":\"Advies\",\"systeem\":true}}],\"actief\":true,\"doorlooptijd\":null},{\"planItemDefinition\":{\"defaultFormulierDefinitie\":\"EXTERN_ADVIES_VASTLEGGEN\",\"id\":\"ADVIES_EXTERN\",\"naam\":\"Advies extern\",\"type\":\"HUMAN_TASK\"},\"defaultGroepId\":null,\"formulierDefinitieId\":\"EXTERN_ADVIES_VASTLEGGEN\",\"referentieTabellen\":[],\"actief\":true,\"doorlooptijd\":null},{\"planItemDefinition\":{\"defaultFormulierDefinitie\":\"DOCUMENT_VERZENDEN_POST\",\"id\":\"DOCUMENT_VERZENDEN_POST\",\"naam\":\"Document verzenden\",\"type\":\"HUMAN_TASK\"},\"defaultGroepId\":null,\"formulierDefinitieId\":\"DOCUMENT_VERZENDEN_POST\",\"referentieTabellen\":[],\"actief\":true,\"doorlooptijd\":null}],\"mailtemplateKoppelingen\":[],\"userEventListenerParameters\":[{\"id\":\"INTAKE_AFRONDEN\",\"naam\":\"Intake afronden\",\"toelichting\":null},{\"id\":\"ZAAK_AFHANDELEN\",\"naam\":\"Zaak afhandelen\",\"toelichting\":null}],\"valide\":false,\"zaakAfzenders\":[],\"zaakbeeindigParameters\":[],\"zaaktype\":{\"beginGeldigheid\":\"2023-09-21\",\"doel\":\"Melding evenement organiseren behandelen\",\"identificatie\":\"melding-evenement-organiseren-behandelen\",\"nuGeldig\":true,\"omschrijving\":\"Melding evenement organiseren behandelen\",\"servicenorm\":false,\"uuid\":\"448356ff-dcfb-4504-9501-7fe929077c4f\",\"versiedatum\":\"2023-09-21\",\"vertrouwelijkheidaanduiding\":\"openbaar\"},\"intakeMail\":\"BESCHIKBAAR_UIT\",\"afrondenMail\":\"BESCHIKBAAR_UIT\",\"caseDefinition\":{\"humanTaskDefinitions\":[{\"defaultFormulierDefinitie\":\"AANVULLENDE_INFORMATIE\",\"id\":\"AANVULLENDE_INFORMATIE\",\"naam\":\"Aanvullende informatie\",\"type\":\"HUMAN_TASK\"},{\"defaultFormulierDefinitie\":\"GOEDKEUREN\",\"id\":\"GOEDKEUREN\",\"naam\":\"Goedkeuren\",\"type\":\"HUMAN_TASK\"},{\"defaultFormulierDefinitie\":\"ADVIES\",\"id\":\"ADVIES_INTERN\",\"naam\":\"Advies intern\",\"type\":\"HUMAN_TASK\"},{\"defaultFormulierDefinitie\":\"EXTERN_ADVIES_VASTLEGGEN\",\"id\":\"ADVIES_EXTERN\",\"naam\":\"Advies extern\",\"type\":\"HUMAN_TASK\"},{\"defaultFormulierDefinitie\":\"DOCUMENT_VERZENDEN_POST\",\"id\":\"DOCUMENT_VERZENDEN_POST\",\"naam\":\"Document verzenden\",\"type\":\"HUMAN_TASK\"}],\"key\":\"melding-klein-evenement\",\"naam\":\"Melding klein evenement\",\"userEventListenerDefinitions\":[{\"defaultFormulierDefinitie\":\"DEFAULT_TAAKFORMULIER\",\"id\":\"INTAKE_AFRONDEN\",\"naam\":\"Intake afronden\",\"type\":\"USER_EVENT_LISTENER\"},{\"defaultFormulierDefinitie\":\"DEFAULT_TAAKFORMULIER\",\"id\":\"ZAAK_AFHANDELEN\",\"naam\":\"Zaak afhandelen\",\"type\":\"USER_EVENT_LISTENER\"}]},\"domein\":null,\"defaultGroepId\":\"test-group-a\",\"defaultBehandelaarId\":null,\"einddatumGeplandWaarschuwing\":null,\"uiterlijkeEinddatumAfdoeningWaarschuwing\":null,\"productaanvraagtype\":null,\"zaakNietOntvankelijkResultaattype\":{\"archiefNominatie\":\"VERNIETIGEN\",\"archiefTermijn\":\"5 jaren\",\"besluitVerplicht\":false,\"id\":\"dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6\",\"naam\":\"Geweigerd\",\"naamGeneriek\":\"Geweigerd\",\"toelichting\":\"Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen\",\"vervaldatumBesluitVerplicht\":false}}"
        ).apply {
            logger.info { "response: $text" }
            statusCode shouldBe HttpStatus.SC_OK
        }
    }
}
