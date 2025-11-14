/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.log

import org.jboss.logging.MDC
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Simple wrapper function around [Logger.log] to make it easier to unit test classes that use logging.
 */
fun log(logger: Logger, level: Level, message: String, throwable: Throwable) = logger.log(level, message, throwable)

/**
 * Execute a block of code with MDC (Mapped Diagnostic Context) that is automatically cleaned up.
 *
 * This function adds the provided context key-value pairs to the MDC before executing the block,
 * and removes them afterwards. This ensures that the context appears in structured JSON logs
 * without risk of memory leaks.
 *
 * Example:
 * ```kotlin
 * withMDC(
 *     "zaakUuid" to zaak.uuid.toString(),
 *     "operation" to "process_zaak"
 * ) {
 *     log.info("Processing zaak")
 *     // zaakUuid and operation will appear in the JSON log output
 * }
 * ```
 *
 * @param context Key-value pairs to add to the MDC. Null values are ignored.
 * @param block The code block to execute with the MDC context.
 * @return The result of executing the block.
 */
inline fun <T> withMDC(vararg context: Pair<String, String?>, block: () -> T): T {
    val keysToRemove = mutableSetOf<String>()

    // Add context to MDC
    context.forEach { (key, value) ->
        if (value != null) {
            MDC.put(key, value)
            keysToRemove.add(key)
        }
    }

    try {
        return block()
    } finally {
        // Clean up MDC
        keysToRemove.forEach { MDC.remove(it) }
    }
}
