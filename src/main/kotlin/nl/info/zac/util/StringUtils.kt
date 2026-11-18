/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.util

import java.nio.charset.StandardCharsets
import java.text.Collator
import java.util.Locale

const val NON_BREAKING_SPACE = "\u00A0"

fun String.isPureAscii(): Boolean = StandardCharsets.US_ASCII.newEncoder().canEncode(this)

fun Iterable<String?>.joinNonBlank(separator: String = "") =
    this.filterNot { it.isNullOrBlank() }.joinToString(separator)

fun String.abbreviate(maxWidth: Int, ellipsis: String = "...") =
    if (length <= maxWidth) this else take(maxWidth - ellipsis.length) + ellipsis

/**
 * Orders strings alphabetically for Dutch users, so that upper and lower case and accents do not move a word out of
 * its place, as `String.localeCompare` does in the browser.
 */
fun dutchCollator() =
    Collator.getInstance(Locale.forLanguageTag("nl")).let { collator -> Comparator<String>(collator::compare) }
