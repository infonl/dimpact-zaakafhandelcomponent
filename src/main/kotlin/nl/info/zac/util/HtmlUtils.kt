/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import org.apache.commons.lang3.StringUtils
import java.util.regex.Pattern

/**
 * Replaces all HTML paragraph tags in the given text with an empty string.
 *
 * @returns the input text with all `<p>` and `</p>` tags removed.
 */
private val HTML_PARAGRAPH_TAG_PATTERN: Pattern = Pattern.compile("</?p>", Pattern.CASE_INSENSITIVE)

fun stripHtmlParagraphTags(text: String): String =
    HTML_PARAGRAPH_TAG_PATTERN
        .matcher(text)
        .replaceAll(StringUtils.EMPTY)
