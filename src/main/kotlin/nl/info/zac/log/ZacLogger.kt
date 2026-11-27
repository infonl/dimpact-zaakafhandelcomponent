/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import io.opentelemetry.api.trace.Span
import org.jboss.logging.MDC
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * The only logger of ZAC. It writes a [LogEvent] as a log record whose message is the description of the event,
 * with every field of the event as a separate MDC entry, which the JSON formatter of WildFly writes as a JSON key.
 *
 * It keeps the name of the class that logs as logger name, so the log categories of WildFly keep working.
 */
class ZacLogger private constructor(private val logger: Logger) {
    companion object {
        fun getLogger(clazz: KClass<*>) = ZacLogger(Logger.getLogger(clazz.java.name))

        /**
         * Registers the Keycloak user ID of an employee who logs in, so that no lookup is needed for that employee.
         */
        fun registerEmployee(username: String, keycloakUserId: String) =
            EmployeeKeycloakUserIds.register(username, keycloakUserId)

        /**
         * Sets the lookup of the Keycloak user ID of an employee who has not logged in on this ZAC instance.
         */
        fun useEmployeeLookup(lookup: (username: String) -> String?) = EmployeeKeycloakUserIds.useEmployeeLookup(lookup)

        /**
         * Runs [block] with the Keycloak user ID of the logged-in employee as [LogFieldNames.EMPLOYEE] in the MDC, so
         * that every record written in the block has that field, without an event of its own naming the employee and
         * without a lookup.
         */
        fun <T> withLoggedInEmployee(keycloakUserId: String, block: () -> T): T =
            withMdcEntry(LogFieldNames.EMPLOYEE, keycloakUserId, block)

        /**
         * Runs [block] with [requestId] as [LogFieldNames.REQUEST_ID] in the MDC, so that every record written in the
         * block can be traced back to its request, also when tracing is disabled and no trace ID is written.
         */
        fun <T> withRequestId(requestId: String, block: () -> T): T = withMdcEntry(LogFieldNames.REQUEST_ID, requestId, block)

        private fun <T> withMdcEntry(key: String, value: String, block: () -> T): T {
            val previousValue = MDC.get(key)
            MDC.put(key, value)
            try {
                return block()
            } finally {
                previousValue?.let { MDC.put(key, it) } ?: MDC.remove(key)
            }
        }
    }

    fun severe(logEvent: LogEvent, throwable: Throwable? = null) = log(Level.SEVERE, logEvent, throwable)

    fun warning(logEvent: LogEvent, throwable: Throwable? = null) = log(Level.WARNING, logEvent, throwable)

    fun info(logEvent: LogEvent, throwable: Throwable? = null) = log(Level.INFO, logEvent, throwable)

    fun fine(logEvent: LogEvent, throwable: Throwable? = null) = log(Level.FINE, logEvent, throwable)

    fun isLoggable(level: Level): Boolean = logger.isLoggable(level)

    /**
     * Writes the fields of [logEvent] for this one record to the MDC, the Mapped Diagnostic Context: the map of keys and
     * values per thread that the log manager adds to every record of that thread. The JSON formatter of WildFly writes
     * it as separate keys under `mdc`, so each field becomes a JSON key without a log formatter of our own. The previous
     * MDC is restored afterwards, so that no field leaks to the next record on the thread.
     */
    fun log(level: Level, logEvent: LogEvent, throwable: Throwable? = null) {
        if (!logger.isLoggable(level)) return
        // Render first: resolving an employee can log a record of its own, which must not see these fields.
        val logRecordFields = logEvent.toLogRecordFields(throwable)
        val previousMappedDiagnosticContext = MDC.getMap().toMap()
        try {
            logRecordFields.forEach { (key, value) -> MDC.put(key, value) }
            logger.log(level, logEvent.description, throwable)
        } finally {
            MDC.clear()
            previousMappedDiagnosticContext.forEach { (key, value) -> MDC.put(key, value) }
        }
    }

    private fun LogEvent.toLogRecordFields(throwable: Throwable?): Map<String, String> =
        buildMap {
            put(LogFieldNames.EVENT, this@toLogRecordFields.javaClass.simpleName)
            putAll(LogEventRenderer.fields(this@toLogRecordFields, EmployeeKeycloakUserIds::resolve))
            throwable?.let { put(LogFieldNames.ERROR_TYPE, it.javaClass.name) }
            Span.current().spanContext.takeIf { it.isValid }?.let {
                put(LogFieldNames.TRACE_ID, it.traceId)
                put(LogFieldNames.SPAN_ID, it.spanId)
            }
        }
}
