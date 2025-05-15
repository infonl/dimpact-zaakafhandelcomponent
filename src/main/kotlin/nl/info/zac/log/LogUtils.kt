/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.log

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Simple wapper function around [Logger.log] to make it easier to unit test classes that use logging.
 */
fun log(logger: Logger, level: Level, message: String, throwable: Throwable) = logger.log(level, message, throwable)
