/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package io.kotest.provided

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.config.AbstractProjectConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.info.zac.ZACContainer
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.Network
import java.io.File

private val logger = KotlinLogging.logger {}

object ProjectConfig : AbstractProjectConfig() {
    const val DOCKER_COMPOSE_ITEST_PROJECT = "zac-itest"
    private const val DOCKER_CONTAINER_ZAC_DATABASE = "zac-database"
    private const val ZAC_DATABASE_PORT = 5432
    private const val TWENTY_SECONDS = 20_000L

    private lateinit var dockerComposeContainer: ComposeContainer
    private lateinit var zacContainer: ZACContainer

    override suspend fun beforeProject() {
        try {
            dockerComposeContainer = ComposeContainer(File("docker-compose.yaml"))
                .withOptions("--project-name $DOCKER_COMPOSE_ITEST_PROJECT")
                .withLocalCompose(true)

            zacContainer = ZACContainer(
                postgresqlHostAndPort = "$DOCKER_CONTAINER_ZAC_DATABASE:$ZAC_DATABASE_PORT",
                // run ZAC container in same Docker network as Docker Compose, so we can access the
                // other containers internally
                network = object : Network {
                    override fun apply(base: Statement, description: Description): Statement = base

                    override fun getId() = "${DOCKER_COMPOSE_ITEST_PROJECT}_default"

                    override fun close() {
                        // noop
                    }
                }
            )

            dockerComposeContainer.start()
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
            logger.error(exception) { "Exception while starting docker containers" }
            stopDockerComposeContainers()
        }
    }

    override suspend fun afterProject() {
        zacContainer.stop()
        stopDockerComposeContainers()
    }

    private fun stopDockerComposeContainers() {
        // dockerComposeContainer.stop() does not take into account the --project-name option
        // we used when starting Docker Compose, and therefore it does not stop our containers.
        // Therefore, we stop all running containers ourselves.
        val dockerClient = DockerClientFactory.lazyClient()
        logger.info { "Stopping all running Docker Compose containers." }
        dockerClient.listContainersCmd().exec().toList()
            .filter { container ->
                container.names.first().startsWith("/$DOCKER_COMPOSE_ITEST_PROJECT") ||
                    container.names.first().startsWith("/testcontainers-ryuk")
            }
            .forEach { container ->
                logger.debug { "Stopping container: ${container.id}" }
                dockerClient.stopContainerCmd(container.id).exec()
            }
    }
}
