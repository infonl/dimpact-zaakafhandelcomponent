/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.util

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.config.ItestConfiguration.ZAC_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.dockerComposeContainer
import kotlin.time.Duration.Companion.seconds

fun zacContainerLogs(): String =
    dockerComposeContainer.getContainerByServiceName(ZAC_CONTAINER_SERVICE_NAME).get().logs

/**
 * ZAC's own startup already triggers a full `reindexAll()` when the Solr schema is not yet at
 * its latest version (e.g. a fresh Solr core, such as in CI). Asserting against the container's
 * entire accumulated log would then make these assertions pass regardless of whether the request
 * fired by this test actually worked, since the startup reindex's log lines are already present
 * before that request is ever sent. Diffing against a log snapshot taken right before the request
 * ensures each assertion only matches lines the request under test actually caused.
 */
fun String.shouldContainLogLineMatching(regex: Regex) {
    withClue(
        "expected the ZAC container log lines logged since the request to contain a line matching: " +
            "${regex.pattern}\n\nLog lines logged since the request:\n$this"
    ) {
        regex.containsMatchIn(this) shouldBe true
    }
}

/**
 * Unlike [String.removePrefix], which silently returns the receiver unchanged when [previousLogs] is
 * not actually a prefix, this fails loudly - so a future change that breaks the prefix guarantee (e.g.
 * a Testcontainers upgrade reordering log frames) surfaces as a test failure instead of the assertions
 * below silently matching against the full accumulated container log again.
 */
fun String.newLogsSince(previousLogs: String): String {
    this shouldStartWith previousLogs
    return substring(previousLogs.length)
}

fun reindexingStartedRegex(zoekObjectType: String) = Regex(
    """\[$zoekObjectType] Reindexing started\. Solr index currently contains (?:\d+|unknown) """ +
        """documents of type '$zoekObjectType'\."""
)

fun reindexingFinishedRegex(zoekObjectType: String) = Regex(
    """\[$zoekObjectType] Reindexing finished\. Reindexed: \d+ / \d+, skipped: \d+, """ +
        """not reindexed because of errors: \d+\. Solr index contains (?:\d+|unknown) """ +
        """documents of type '$zoekObjectType'\."""
)

/**
 * Reindexing now runs asynchronously (the reindex REST endpoints respond before it finishes), so a
 * caller that needs the reindex to have actually completed before proceeding (e.g. to avoid racing a
 * still-running full reindex that would re-add an entity a later step removes) must wait for the
 * "Reindexing finished" log line rather than relying on the HTTP response.
 */
suspend fun waitForReindexToFinish(zoekObjectType: String, logsBeforeReindex: String) {
    // a reindex's duration scales with how many entities already exist in the shared itest environment,
    // which grows as more of the itest suite runs before this call - 10 seconds is too tight once enough
    // zaken have accumulated (e.g. NotificationZaakDestroyTest running late in the suite)
    eventually(60.seconds) {
        zacContainerLogs().newLogsSince(logsBeforeReindex).shouldContainLogLineMatching(
            reindexingFinishedRegex(zoekObjectType)
        )
    }
}
