/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import org.jboss.logging.MDC
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.reflect.KClass

private val FIELDS_ADDED_BY_ZAC_LOGGER = setOf(
    LogFieldNames.EVENT,
    LogFieldNames.ERROR_TYPE,
    LogFieldNames.TRACE_ID,
    LogFieldNames.SPAN_ID
)

/**
 * Captures the records that [block] logs through the loggers of [classes], in the order in which they are written.
 */
fun captureLogTexts(vararg classes: KClass<*>, block: () -> Unit): List<CapturedLogText> {
    val loggers = classes.map { Logger.getLogger(it.java.name) }
    val previousLevels = loggers.map { it.level }
    val records = mutableListOf<CapturedLogText>()
    val handler = object : Handler() {
        override fun publish(logRecord: LogRecord) {
            records += logRecord.toCapturedLogText()
        }

        override fun flush() = Unit

        override fun close() = Unit
    }
    loggers.forEach {
        it.level = Level.ALL
        it.addHandler(handler)
    }
    try {
        block()
    } finally {
        loggers.forEachIndexed { index, logger ->
            logger.removeHandler(handler)
            logger.level = previousLevels[index]
        }
    }
    return records
}

/**
 * The record as a [CapturedLogText]; call it while the record is written, because the fields are read from the MDC.
 */
fun LogRecord.toCapturedLogText(): CapturedLogText {
    val fields = MDC.getMap()
        .filterKeys { it !in FIELDS_ADDED_BY_ZAC_LOGGER }
        .toSortedMap()
    val text = if (fields.isEmpty()) {
        message
    } else {
        fields.entries.joinToString(prefix = "$message [", postfix = "]") { "${it.key}=${it.value}" }
    }
    return CapturedLogText(text, thrown)
}
