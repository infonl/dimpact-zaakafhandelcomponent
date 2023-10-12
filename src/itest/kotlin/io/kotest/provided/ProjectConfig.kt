/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package io.kotest.provided

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.config.AbstractProjectConfig
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import java.io.File

private val logger = KotlinLogging.logger {}

object ProjectConfig : AbstractProjectConfig() {
    private lateinit var dockerComposeContainer: ComposeContainer

    override suspend fun beforeProject() {
        try {
            dockerComposeContainer = ComposeContainer(File("docker-compose.yaml"))
                .withLocalCompose(true)
                .withOptions("--profile zac")
                .withEnv(
                    mapOf(
                        "AUTH_SERVER" to "localhost:8081"
                    )
                )

            dockerComposeContainer.start()

            logger.info { "Started ZAC Docker Compose container: $dockerComposeContainer" }

            // .withExposedService("postgres", 5432)
        } catch (exception: ContainerLaunchException) {
            logger.error(exception) { "Exception while starting docker containers" }
            dockerComposeContainer.stop()
        }
    }

    override suspend fun afterProject() {
        dockerComposeContainer.stop()
    }
}
