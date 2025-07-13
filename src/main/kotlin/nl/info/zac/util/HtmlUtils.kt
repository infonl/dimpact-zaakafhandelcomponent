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
fun stripHtmlParagraphTags(text: String): String =
    Pattern.compile("</?p>", Pattern.CASE_INSENSITIVE)
        .matcher(text)
        .replaceAll(StringUtils.EMPTY)
