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
    }

    fun severe(logEvent: LogEvent, throwable: Throwable? = null) = log(Level.SEVERE, logEvent, throwable)

    fun warning(logEvent: LogEvent, throwable: Throwable? = null) = log(Level.WARNING, logEvent, throwable)

    fun info(logEvent: LogEvent, throwable: Throwable? = null) = log(Level.INFO, logEvent, throwable)

    fun fine(logEvent: LogEvent, throwable: Throwable? = null) = log(Level.FINE, logEvent, throwable)

    /**
     * Logs the event at [Level.FINE], and only creates it when that level is enabled.
     */
    fun fine(logEventSupplier: () -> LogEvent) {
        if (isLoggable(Level.FINE)) log(Level.FINE, logEventSupplier())
    }

    fun isLoggable(level: Level): Boolean = logger.isLoggable(level)

    fun log(level: Level, logEvent: LogEvent, throwable: Throwable? = null) {
        if (!logger.isLoggable(level)) return
        // Render first: resolving an employee can log a record of its own, which must not see these fields.
        val mdcFields = logEvent.toMdcFields(throwable)
        val previousMdc = MDC.getMap().toMap()
        try {
            mdcFields.forEach { (key, value) -> MDC.put(key, value) }
            logger.log(level, logEvent.description, throwable)
        } finally {
            MDC.clear()
            previousMdc.forEach { (key, value) -> MDC.put(key, value) }
        }
    }

    private fun LogEvent.toMdcFields(throwable: Throwable?): Map<String, String> =
        buildMap {
            put(LogFieldNames.EVENT, LogEventRenderer.eventName(this@toMdcFields))
            putAll(LogEventRenderer.fields(this@toMdcFields, EmployeeKeycloakUserIds::resolve))
            throwable?.let { put(LogFieldNames.ERROR_TYPE, it.javaClass.name) }
            Span.current().spanContext.takeIf { it.isValid }?.let {
                put(LogFieldNames.TRACE_ID, it.traceId)
                put(LogFieldNames.SPAN_ID, it.spanId)
            }
        }
}
