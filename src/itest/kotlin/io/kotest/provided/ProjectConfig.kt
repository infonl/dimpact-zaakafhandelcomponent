/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package io.kotest.provided

import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.config.AbstractProjectConfig
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

const val ZAAKTYPE_PRODUCTAANVRAAG_UUID = "021f685e-9482-4620-b157-34cd4003da6b"

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
            zacContainer.start()

            logger.info { "Started ZAC Docker Compose container: $dockerComposeContainer" }
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
        // TODO: use ZaakafhandelParametersRESTService to create zaakafhandelparameters

    }
}
