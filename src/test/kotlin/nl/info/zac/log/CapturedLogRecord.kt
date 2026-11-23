/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.logging.Level

class CapturedLogRecord(
    val level: Level,
    val message: String?,
    val thrown: Throwable?,
    val mdc: Map<String, String>
) {
    fun containsAnywhere(text: String) =
        message.orEmpty().contains(text) ||
            mdc.values.any { it.contains(text) } ||
            generateSequence(thrown) { it.cause }.any { it.toString().contains(text) }
}
