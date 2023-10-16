/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package io.kotest.provided

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.config.AbstractProjectConfig
import nl.info.zac.ZACContainer
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.Network
import java.io.File

private val logger = KotlinLogging.logger {}

object ProjectConfig : AbstractProjectConfig() {
    private lateinit var dockerComposeContainer: ComposeContainer
    private lateinit var zacContainer: ZACContainer

    override suspend fun beforeProject() {
        try {
            dockerComposeContainer = ComposeContainer(File("docker-compose.yaml"))
                .withLocalCompose(true)
                .withOptions("-p zac-itest")
                // TODO: java.lang.IllegalStateException: Services named [zac-itest-zac-database-1] do not exist,
                //                .waitingFor(
                //                    "zac-itest-zac-database-1",
                //                    Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(60))
                //                )

            zacContainer = ZACContainer(
                postgresqlHostAndPort = "zac-database:5432",
                // run ZAC container in same Docker network as Docker Compose, so we can access the
                // other containers internally
                network = getTestContainersNetwork("zac-itest_default")
            )

            dockerComposeContainer.start()
            zacContainer.start()

            logger.info { "Started ZAC Docker Compose container: $dockerComposeContainer" }

            // .withExposedService("postgres", 5432)
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Exception while starting docker containers" }
            dockerComposeContainer.stop()
        }
    }

    override suspend fun afterProject() {
        zacContainer.stop()
        dockerComposeContainer.stop()
    }
}

private fun getTestContainersNetwork(networkName: String): Network =
    object : Network {
        override fun apply(base: Statement, description: Description): Statement = base

        override fun getId() = networkName

        override fun close() {
            // noop
        }
    }

