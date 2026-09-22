/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.annotation.Isolate
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Records how long the phases of an integration test run take (starting the Docker Compose stack,
 * ZAC becoming healthy, the specs themselves and the teardown) and writes them as a Markdown report
 * to [REPORT_FILE], so that CI can publish the report in its job summary.
 */
@Suppress("TooManyFunctions")
object ItestTimingReport : BeforeSpecListener, AfterSpecListener {
    const val PHASE_RUN_STARTED = "Integration test run started"
    const val PHASE_COMPOSE_STARTED = "Docker Compose stack started"
    const val PHASE_KEYCLOAK_HEALTHY = "Keycloak healthy"
    const val PHASE_ZAC_HEALTHY = "ZAC healthy"
    const val PHASE_TEST_SETUP_DATA_CREATED = "Test setup data created"
    const val PHASE_FIRST_SPEC_STARTED = "First spec started"
    const val PHASE_LAST_SPEC_FINISHED = "Last spec finished"
    const val PHASE_ZAC_STOPPED = "ZAC container stopped"
    const val PHASE_COMPOSE_REMOVED = "Docker Compose stack removed"

    val REPORT_FILE = File("build/reports/itest/timings.md")

    private const val COMPOSE_PROJECT_LABEL = "com.docker.compose.project"
    private const val COMPOSE_SERVICE_LABEL = "com.docker.compose.service"
    private const val NUMBER_OF_SLOWEST_SPECS = 15
    private const val MILLIS_PER_SECOND = 1000.0
    private const val MILLIS_PER_MINUTE = 60_000

    private val logger = KotlinLogging.logger {}

    /**
     * The log line that signals that a container is ready to serve requests, for the containers that
     * the integration tests wait for or that are on the critical path towards a started ZAC.
     */
    private val readinessLogMarkers = mapOf(
        "keycloak" to Regex("Listening on:"),
        "openzaak-app.local" to Regex("spawned uWSGI worker 2"),
        "pabc-api" to Regex("Application started"),
        "solr" to Regex("Started Server@"),
        "zac" to Regex("WildFly .* started"),
        "greenmail" to Regex("Starting GreenMail API server")
    )

    private val shard: String? = System.getProperty("zac.itest.shard")
    private val phases = LinkedHashMap<String, Instant>()
    private val isolatedSpecs = ConcurrentHashMap.newKeySet<String>()
    private val specStartTimes = ConcurrentHashMap<String, Instant>()
    private val specDurations = ConcurrentHashMap<String, Duration>()
    private var containerTimings = emptyList<ContainerTiming>()

    private data class ContainerTiming(val serviceName: String, val startedAt: Instant?, val readyAt: Instant?)

    fun markPhase(phase: String) {
        synchronized(phases) { phases[phase] = Instant.now() }
    }

    override suspend fun beforeSpec(spec: Spec) {
        specStartTimes[spec.specName()] = Instant.now()
        if (spec::class.java.isAnnotationPresent(Isolate::class.java)) {
            isolatedSpecs.add(spec.specName())
        }
        synchronized(phases) {
            if (PHASE_FIRST_SPEC_STARTED !in phases) {
                phases[PHASE_FIRST_SPEC_STARTED] = Instant.now()
            }
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        specStartTimes.remove(spec.specName())?.let { startTime ->
            specDurations[spec.specName()] = Duration.between(startTime, Instant.now())
        }
        markPhase(PHASE_LAST_SPEC_FINISHED)
    }

    /**
     * Reads the start and readiness times of all containers of the Docker Compose project.
     * Must be called before the containers are removed because it reads their logs.
     */
    fun collectContainerTimings(dockerClient: DockerClient, composeProjectName: String) {
        containerTimings = dockerClient.listContainersCmd()
            .withShowAll(true)
            .withLabelFilter(mapOf(COMPOSE_PROJECT_LABEL to composeProjectName))
            .exec()
            .mapNotNull { container ->
                container.labels[COMPOSE_SERVICE_LABEL]?.let { serviceName ->
                    ContainerTiming(
                        serviceName = serviceName,
                        startedAt = dockerClient.inspectContainerCmd(container.id).exec().state.startedAt?.toInstant(),
                        readyAt = readinessLogMarkers[serviceName]?.let { marker ->
                            findFirstLogLineTime(dockerClient, container.id, marker)
                        }
                    )
                }
            }
            .sortedWith(compareBy(nullsLast()) { it.startedAt })
    }

    fun writeReport() {
        val report = renderMarkdown()
        REPORT_FILE.parentFile.mkdirs()
        REPORT_FILE.writeText(report)
        logger.info { "Integration test timing report written to '${REPORT_FILE.path}':\n$report" }
    }

    private fun Spec.specName() = this::class.simpleName ?: this::class.toString()

    private fun findFirstLogLineTime(dockerClient: DockerClient, containerId: String, marker: Regex): Instant? {
        val logOutput = StringBuilder()
        dockerClient.logContainerCmd(containerId)
            .withStdOut(true)
            .withStdErr(true)
            .withTimestamps(true)
            .exec(
                object : ResultCallback.Adapter<Frame>() {
                    override fun onNext(frame: Frame) {
                        logOutput.append(String(frame.payload, Charsets.UTF_8))
                    }
                }
            )
            .awaitCompletion()
        return logOutput.lineSequence()
            .firstOrNull { marker.containsMatchIn(it) }
            ?.substringBefore(' ')
            ?.toInstant()
    }

    private fun String.toInstant(): Instant? =
        try {
            Instant.parse(this)
        } catch (dateTimeParseException: DateTimeParseException) {
            logger.warn { "Could not parse Docker timestamp '$this': ${dateTimeParseException.message}" }
            null
        }

    private fun renderMarkdown(): String {
        val phaseSnapshot = synchronized(phases) { LinkedHashMap(phases) }
        val runStart = phaseSnapshot.values.firstOrNull() ?: Instant.now()
        return buildString {
            appendLine("## Integration test timings${shard?.let { " (shard $it)" }.orEmpty()}")
            appendLine()
            appendPhaseTable(phaseSnapshot, runStart)
            appendContainerTable(runStart)
            appendSlowestSpecsTable()
        }
    }

    private fun StringBuilder.appendPhaseTable(phaseSnapshot: Map<String, Instant>, runStart: Instant) {
        appendLine("| Phase | Reached after | Took |")
        appendLine("|---|---|---|")
        var previous = runStart
        phaseSnapshot.forEach { (phase, reachedAt) ->
            appendLine(
                "| $phase | ${Duration.between(runStart, reachedAt).format()} | " +
                    "${Duration.between(previous, reachedAt).format()} |"
            )
            previous = reachedAt
        }
    }

    private fun StringBuilder.appendContainerTable(runStart: Instant) {
        if (containerTimings.isEmpty()) return
        appendLine()
        appendLine("### Docker Compose containers")
        appendLine()
        appendLine("Times are relative to the start of the integration test run.")
        appendLine()
        appendLine("| Service | Started after | Ready after |")
        appendLine("|---|---|---|")
        containerTimings.forEach { containerTiming ->
            appendLine(
                "| ${containerTiming.serviceName} | ${containerTiming.startedAt.formatOffsetFrom(runStart)} | " +
                    "${containerTiming.readyAt.formatOffsetFrom(runStart)} |"
            )
        }
    }

    private fun StringBuilder.appendSlowestSpecsTable() {
        if (specDurations.isEmpty()) return
        val slowestSpecs = specDurations.entries
            .sortedByDescending { it.value }
            .take(NUMBER_OF_SLOWEST_SPECS)
        val summedSpecDuration = specDurations.values.fold(Duration.ZERO, Duration::plus)
        val wallClockSpecDuration = synchronized(phases) {
            phases[PHASE_FIRST_SPEC_STARTED]?.let { firstSpecStarted ->
                phases[PHASE_LAST_SPEC_FINISHED]?.let { lastSpecFinished ->
                    Duration.between(firstSpecStarted, lastSpecFinished)
                }
            }
        }
        appendLine()
        appendLine(
            "### Slowest specs (${slowestSpecs.size} of ${specDurations.size}, ${isolatedSpecs.size} isolated; " +
                "the specs took ${wallClockSpecDuration?.format() ?: "-"} on the clock, " +
                "${summedSpecDuration.format()} summed)"
        )
        appendLine()
        appendLine("| Spec | Duration | Isolated |")
        appendLine("|---|---|---|")
        slowestSpecs.forEach { (specName, duration) ->
            appendLine("| $specName | ${duration.format()} | ${if (specName in isolatedSpecs) "yes" else "-"} |")
        }
    }

    private fun Instant?.formatOffsetFrom(runStart: Instant) =
        this?.takeIf { it.isAfter(runStart) }?.let { Duration.between(runStart, it).format() } ?: "-"

    private fun Duration.format(): String {
        val minutes = toMillis() / MILLIS_PER_MINUTE
        val seconds = (toMillis() % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND
        return if (minutes > 0) {
            String.format(Locale.ROOT, "%dm %.0fs", minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%.1fs", seconds)
        }
    }
}
