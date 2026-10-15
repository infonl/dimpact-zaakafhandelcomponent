/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.util

import java.nio.charset.StandardCharsets

const val NON_BREAKING_SPACE = "\u00A0"

fun String.isPureAscii(): Boolean = StandardCharsets.US_ASCII.newEncoder().canEncode(this)

fun Iterable<String?>.joinNonBlank(separator: String = "") =
    this.filterNot { it.isNullOrBlank() }.joinToString(separator)
