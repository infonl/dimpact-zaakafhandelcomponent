/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.log

import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Simple wrapper function around [Logger.log] to make it easier to handle exception logging, for example in unit tests.
 */
fun log(logger: Logger, level: Level, message: String, throwable: Throwable) = logger.log(level, message, throwable)

/**
 * Execute a block of code with logging context that is automatically cleaned up.
 *
 * This function adds the provided context key-value pairs to OpenTelemetry Baggage before executing the block,
 * and removes them afterwards. This ensures that the context appears in structured JSON logs
 * without risk of memory leaks. The context automatically propagates across thread boundaries and service calls.
 *
 * Example:
 * ```kotlin
 * withLoggingContext(
 *     "zaakUuid" to zaak.uuid.toString(),
 *     "operation" to "process_zaak"
 * ) {
 *     log.info("Processing zaak")
 *     // zaakUuid and operation will appear in the JSON log output
 * }
 * ```
 *
 * @param context Key-value pairs to add to the logging context. Null values are ignored.
 * @param block The code block to execute with the logging context.
 * @return The result of executing the block.
 */
inline fun <T> withLoggingContext(vararg context: Pair<String, String?>, block: () -> T): T {
    var baggage = Baggage.current()
    context.forEach { (key, value) ->
        if (value != null) {
            baggage = baggage.toBuilder().put(key, value).build()
        }
    }
    val ctx = Context.current().with(baggage)
    return ctx.makeCurrent().use { block() }
}
