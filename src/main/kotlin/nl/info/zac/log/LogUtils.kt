/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.log

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Simple wrapper function around [Logger.log] to make it easier to handle exception logging, for example in unit tests.
 */
fun log(logger: Logger, level: Level, message: String, throwable: Throwable) = logger.log(level, message, throwable)

fun log(logger: Logger, level: Level, message: String) = logger.log(level, message)

/**
 * A [Level] named "ERROR" with the same numeric severity as [Level.SEVERE] (1000), so that WildFly's JSON
 * log formatter (which prints a level's name as-is) renders server errors as "ERROR" instead of "SEVERE" -
 * the name log-aggregation tools such as Grafana/Loki expect from Log4j/SLF4J/syslog-style levels. Server
 * errors should be logged via `LOG.log(ERROR, ...)` instead of `LOG.severe(...)` or `Level.SEVERE`.
 */
@JvmField
val ERROR: Level = object : Level("ERROR", Level.SEVERE.intValue()) {}
