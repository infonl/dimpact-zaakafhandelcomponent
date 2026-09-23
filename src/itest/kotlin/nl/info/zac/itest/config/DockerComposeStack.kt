/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.exception.ConflictException
import com.github.dockerjava.api.exception.NotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.info.zac.itest.config.ItestConfiguration.ZAC_CONTAINER_SERVICE_NAME
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget

const val TESTCONTAINERS_COMPOSE_PROJECT_PREFIX = "zac-itest-"

private const val COMPOSE_PROJECT_LABEL = "com.docker.compose.project"
private const val COMPOSE_SERVICE_LABEL = "com.docker.compose.service"
private const val COMPOSE_PROJECT_NAME_ENV_VAR = "COMPOSE_PROJECT_NAME"

private val PROJECT_NAME_LOOKUP_SERVICE_NAMES = listOf(ZAC_CONTAINER_SERVICE_NAME, "keycloak", "solr")

private val logger = KotlinLogging.logger {}

/**
 * Resolves the containers of the Docker Compose stack that the integration tests run against by their
 * Compose labels, so that a spec reaches the same container whether the tests started the stack themselves
 * or it was started for them.
 */
object DockerComposeStack {
    val dockerClient: DockerClient by lazy { DockerClientFactory.instance().client() }

    private val projectNameFromEnvironment = System.getenv(COMPOSE_PROJECT_NAME_ENV_VAR)?.takeIf { it.isNotBlank() }

    /**
     * Testcontainers names the Compose project it starts after [TESTCONTAINERS_COMPOSE_PROJECT_PREFIX] plus a
     * random suffix, so when the environment does not name the project it is read back from the labels of the
     * running containers. A stack Testcontainers started wins from a stack that was already running, which
     * would otherwise answer for a service of the same name.
     */
    val projectName: String? by lazy {
        projectNameFromEnvironment ?: PROJECT_NAME_LOOKUP_SERVICE_NAMES
            .flatMap(::findProjectNamesOfService)
            .let { projectNames ->
                projectNames.firstOrNull { it.startsWith(TESTCONTAINERS_COMPOSE_PROJECT_PREFIX) }
                    ?: projectNames.firstOrNull()
            }
    }

    fun findContainerOfService(serviceName: String) =
        dockerClient.listContainersCmd()
            .withLabelFilter(
                buildMap {
                    put(COMPOSE_SERVICE_LABEL, serviceName)
                    projectName?.let { put(COMPOSE_PROJECT_LABEL, it) }
                }
            )
            .exec()
            .firstOrNull()
            ?.id
            ?.let(::ComposeServiceContainer)

    fun readContainerOfService(serviceName: String) =
        findContainerOfService(serviceName)
            ?: error("No running container found for Docker Compose service '$serviceName'")

    fun killRunningContainers(composeProjectName: String) {
        dockerClient.listContainersCmd()
            .withLabelFilter(mapOf(COMPOSE_PROJECT_LABEL to composeProjectName))
            .exec()
            .forEach { container ->
                try {
                    dockerClient.killContainerCmd(container.id).exec()
                } catch (conflictException: ConflictException) {
                    logger.debug { "Container '${container.id}' was no longer running: ${conflictException.message}" }
                } catch (notFoundException: NotFoundException) {
                    logger.debug { "Container '${container.id}' was already removed: ${notFoundException.message}" }
                }
            }
        logger.info { "Killed the remaining Docker Compose containers of project '$composeProjectName'" }
    }

    private fun findProjectNamesOfService(serviceName: String) =
        dockerClient.listContainersCmd()
            .withLabelFilter(mapOf(COMPOSE_SERVICE_LABEL to serviceName))
            .exec()
            .mapNotNull { it.labels[COMPOSE_PROJECT_LABEL] }
}

/**
 * A container of the Compose stack, addressed by its ID. Implementing [WaitStrategyTarget] gives the
 * integration tests the Testcontainers container API, including its wait strategies, for containers that
 * Testcontainers itself did not start.
 */
class ComposeServiceContainer(private val id: String) : WaitStrategyTarget {
    override fun getContainerId() = id

    override fun getContainerInfo(): InspectContainerResponse =
        DockerComposeStack.dockerClient.inspectContainerCmd(id).exec()

    override fun getExposedPorts(): MutableList<Int> = mutableListOf()
}
