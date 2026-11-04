/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.annotation.Isolate
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import org.json.JSONObject
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Records how long the phases of an integration test run take (starting the kind stack,
 * ZAC becoming healthy, the specs themselves and the teardown) and writes them as a Markdown report
 * to [REPORT_FILE], so that CI can publish the report in its job summary.
 */
@Suppress("TooManyFunctions")
object ItestTimingReport : BeforeSpecListener, AfterSpecListener {
    const val PHASE_RUN_STARTED = "Integration test run started"
    const val PHASE_KIND_STACK_STARTED = "kind stack started"
    const val PHASE_KEYCLOAK_HEALTHY = "Keycloak healthy"
    const val PHASE_ZAC_HEALTHY = "ZAC healthy"
    const val PHASE_TEST_SETUP_DATA_CREATED = "Test setup data created"
    const val PHASE_FIRST_SPEC_STARTED = "First spec started"
    const val PHASE_LAST_SPEC_FINISHED = "Last spec finished"
    const val PHASE_COVERAGE_COLLECTED = "Coverage collected"
    const val PHASE_KIND_STACK_DELETED = "kind stack deleted"

    val REPORT_FILE = File("build/reports/itest/timings.md")

    private const val NUMBER_OF_SLOWEST_SPECS = 15
    private const val MILLIS_PER_SECOND = 1000.0
    private const val MILLIS_PER_MINUTE = 60_000

    private val logger = KotlinLogging.logger {}

    private val shard: String? = System.getProperty("zac.itest.shard")
    private val phases = LinkedHashMap<String, Instant>()
    private val isolatedSpecs = ConcurrentHashMap.newKeySet<String>()
    private val specStartTimes = ConcurrentHashMap<String, Instant>()
    private val specDurations = ConcurrentHashMap<String, Duration>()
    private var podTimings = emptyList<PodTiming>()

    private data class PodTiming(val podName: String, val startedAt: Instant?, val readyAt: Instant?)

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
     * Reads the start and readiness times of the pods of the kind stack. Must be called before the cluster is deleted.
     */
    fun collectPodTimings() {
        podTimings = try {
            ItestKindStack.pods().let { pods -> (0 until pods.length()).map(pods::getJSONObject) }
                .map { pod ->
                    val status = pod.getJSONObject("status")
                    PodTiming(
                        podName = pod.getJSONObject("metadata").getString("name"),
                        startedAt = status.optString("startTime").takeIf { it.isNotEmpty() }?.toInstant(),
                        readyAt = status.readyAt()
                    )
                }
                .sortedWith(compareBy(nullsLast()) { it.startedAt })
        } catch (illegalStateException: IllegalStateException) {
            logger.warn { "Could not collect the pod timings: ${illegalStateException.message}" }
            emptyList()
        }
    }

    fun writeReport() {
        val report = renderMarkdown()
        REPORT_FILE.parentFile.mkdirs()
        REPORT_FILE.writeText(report)
        logger.info { "Integration test timing report written to '${REPORT_FILE.path}':\n$report" }
    }

    private fun Spec.specName() = this::class.simpleName ?: this::class.toString()

    private fun JSONObject.readyAt() =
        optJSONArray("conditions")?.let { conditions -> (0 until conditions.length()).map(conditions::getJSONObject) }
            ?.firstOrNull { it.getString("type") == "Ready" && it.getString("status") == "True" }
            ?.optString("lastTransitionTime")
            ?.toInstant()

    private fun String.toInstant(): Instant? =
        try {
            Instant.parse(this)
        } catch (dateTimeParseException: DateTimeParseException) {
            logger.warn { "Could not parse Kubernetes timestamp '$this': ${dateTimeParseException.message}" }
            null
        }

    private fun renderMarkdown(): String {
        val phaseSnapshot = synchronized(phases) { LinkedHashMap(phases) }
        val runStart = phaseSnapshot.values.firstOrNull() ?: Instant.now()
        return buildString {
            appendLine("## Integration test timings${shard?.let { " (shard $it)" }.orEmpty()}")
            appendLine()
            appendPhaseTable(phaseSnapshot, runStart)
            appendPodTable(runStart)
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

    private fun StringBuilder.appendPodTable(runStart: Instant) {
        if (podTimings.isEmpty()) return
        appendLine()
        appendLine("### Pods of the kind stack")
        appendLine()
        appendLine("Times are relative to the start of the integration test run; '-' is before it.")
        appendLine()
        appendLine("| Pod | Started after | Ready after |")
        appendLine("|---|---|---|")
        podTimings.forEach { podTiming ->
            appendLine(
                "| ${podTiming.podName} | ${podTiming.startedAt.formatOffsetFrom(runStart)} | " +
                    "${podTiming.readyAt.formatOffsetFrom(runStart)} |"
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
