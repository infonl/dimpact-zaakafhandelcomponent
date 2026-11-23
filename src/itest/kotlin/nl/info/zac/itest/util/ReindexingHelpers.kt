/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.util

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.config.ItestKindStack
import org.json.JSONException
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

fun zacContainerLogs() = ItestKindStack.zacLogs()

/**
 * A ZAC log record, written by the JSON formatter of WildFly, that a test expects: [description] names it in the
 * failure message, and [matches] checks the `message` and the `mdc` fields of one record.
 */
class ExpectedLogRecord(val description: String, val matches: (JSONObject) -> Boolean)

/**
 * ZAC's own startup already triggers a full `reindexAll()` when the Solr schema is not yet at
 * its latest version (e.g. a fresh Solr core, such as in CI). Asserting against the container's
 * entire accumulated log would then make these assertions pass regardless of whether the request
 * fired by this test actually worked, since the startup reindex's log lines are already present
 * before that request is ever sent. Diffing against a log snapshot taken right before the request
 * ensures each assertion only matches lines the request under test actually caused.
 */
fun String.shouldContainLogRecord(expectedLogRecord: ExpectedLogRecord) {
    withClue(
        "expected the ZAC container log lines logged since the request to contain a record of " +
            "${expectedLogRecord.description}\n\nLog lines logged since the request:\n$this"
    ) {
        jsonLogRecords().any(expectedLogRecord.matches) shouldBe true
    }
}

private fun String.jsonLogRecords() =
    lines()
        .filter { it.trimStart().startsWith("{") }
        .mapNotNull {
            try {
                JSONObject(it)
            } catch (_: JSONException) {
                null
            }
        }

private fun JSONObject.mdcField(name: String): String? = optJSONObject("mdc")?.optString(name, null)

private fun JSONObject.isEvent(event: String) = mdcField("event") == event

private val COUNT = Regex("""\d+""")
private val COUNT_OR_NULL = Regex("""\d+|null""")

private fun JSONObject.hasCount(name: String) = mdcField(name)?.matches(COUNT) == true

fun reindexingStarted(zoekObjectType: String) = ExpectedLogRecord("ReindexingStarted for $zoekObjectType") {
    it.isEvent("ReindexingStarted") &&
        it.mdcField("objectType") == zoekObjectType &&
        it.mdcField("solrDocumentCount")?.matches(COUNT_OR_NULL) == true
}

fun reindexingFinished(zoekObjectType: String) = ExpectedLogRecord("ReindexingFinished for $zoekObjectType") {
    it.isEvent("ReindexingFinished") &&
        it.mdcField("objectType") == zoekObjectType &&
        listOf("reindexedCount", "totalCount", "skippedCount", "errorCount").all(it::hasCount) &&
        it.mdcField("solrDocumentCount")?.matches(COUNT_OR_NULL) == true
}

fun completeReindexing(event: String, objectTypes: String) = ExpectedLogRecord("$event for $objectTypes") {
    it.isEvent(event) && it.mdcField("objectTypes") == objectTypes
}

/**
 * Unlike [String.removePrefix], which silently returns the receiver unchanged when [previousLogs] is
 * not actually a prefix, this fails loudly - so a future change that breaks the prefix guarantee (e.g.
 * a rotation of the container log) surfaces as a test failure instead of the assertions
 * below silently matching against the full accumulated container log again.
 */
fun String.newLogsSince(previousLogs: String): String {
    this shouldStartWith previousLogs
    return substring(previousLogs.length)
}

/**
 * Reindexing now runs asynchronously (the reindex REST endpoints respond before it finishes), so a
 * caller that needs the reindex to have actually completed before proceeding (e.g. to avoid racing a
 * still-running full reindex that would re-add an entity a later step removes) must wait for the
 * "Reindexing finished" log record rather than relying on the HTTP response.
 */
suspend fun waitForReindexToFinish(zoekObjectType: String, logsBeforeReindex: String) {
    // a reindex's duration scales with how many entities already exist in the shared itest environment,
    // which grows as more of the itest suite runs before this call - 10 seconds is too tight once enough
    // zaken have accumulated (e.g. NotificationZaakDestroyTest running late in the suite)
    eventually(60.seconds) {
        zacContainerLogs().newLogsSince(logsBeforeReindex).shouldContainLogRecord(reindexingFinished(zoekObjectType))
    }
}
