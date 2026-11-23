/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import org.jboss.logging.MDC
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * Captures the log records of the logger of [clazz], together with the MDC at the moment each record is written.
 */
class LogCapture(clazz: KClass<*>, level: Level = Level.ALL) : AutoCloseable {
    private val logger = Logger.getLogger(clazz.java.name)
    private val previousLevel = logger.level
    private val isUsingParentHandlersBefore = logger.useParentHandlers
    private val handler = object : Handler() {
        override fun publish(logRecord: LogRecord) {
            records += CapturedLogRecord(
                level = logRecord.level,
                message = logRecord.message,
                thrown = logRecord.thrown,
                mdc = MDC.getMap().mapValues { (_, value) -> value.toString() }
            )
            onPublish(logRecord)
        }

        override fun flush() = Unit

        override fun close() = Unit
    }

    val records = CopyOnWriteArrayList<CapturedLogRecord>()

    var onPublish: (LogRecord) -> Unit = {}

    init {
        logger.level = level
        logger.useParentHandlers = false
        logger.addHandler(handler)
    }

    override fun close() {
        logger.removeHandler(handler)
        logger.level = previousLevel
        logger.useParentHandlers = isUsingParentHandlersBefore
    }
}
