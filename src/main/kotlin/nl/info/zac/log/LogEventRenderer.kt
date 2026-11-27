/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.lang.reflect.Field
import java.lang.reflect.Modifier

const val REDACTED_VALUE = "*****"
const val UNKNOWN_EMPLOYEE = "unknown"

private const val NULL_VALUE = "null"

object LogEventRenderer {
    /**
     * The fields of [logEvent] as name and value, in declaration order. A [Redacted] field is written as
     * [REDACTED_VALUE], and an [Employee] as the result of [renderEmployee].
     */
    fun fields(logEvent: LogEvent, renderEmployee: (Employee) -> String = { UNKNOWN_EMPLOYEE }): Map<String, String> =
        logEvent.javaClass.eventFields().associate { field ->
            field.name to if (field.isAnnotationPresent(Redacted::class.java)) {
                field.get(logEvent)?.let { REDACTED_VALUE } ?: NULL_VALUE
            } else {
                field.get(logEvent).renderValue(renderEmployee)
            }
        }

    /**
     * The description and fields of [logEvent] as one text, for unit tests only. [ZacLogger] writes the fields as
     * separate keys of the log record.
     */
    fun render(logEvent: LogEvent) =
        fields(logEvent).entries.joinToString(prefix = "${logEvent.description} [", postfix = "]") {
            "${it.key}=${it.value}"
        }

    private fun Class<*>.eventFields(): List<Field> =
        generateSequence(this) { it.superclass }
            .takeWhile { it != LogEvent::class.java }
            .toList()
            .reversed()
            .flatMap { eventClass ->
                eventClass.declaredFields.filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
            }
            .onEach { it.isAccessible = true }

    private fun Any?.renderValue(renderEmployee: (Employee) -> String): String =
        when (this) {
            null -> NULL_VALUE
            is Employee -> renderEmployee(this)
            is Enum<*> -> name
            is Collection<*> -> joinToString(prefix = "[", postfix = "]") { it.renderValue(renderEmployee) }
            is Map<*, *> -> entries.joinToString(prefix = "{", postfix = "}") {
                "${it.key.renderValue(renderEmployee)}=${it.value.renderValue(renderEmployee)}"
            }
            else -> toString()
        }
}
