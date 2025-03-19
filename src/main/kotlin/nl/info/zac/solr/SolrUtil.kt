/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.solr

private const val SOLR_ESCAPE = '\\'
private const val SOLR_QUOTE = '\"'

/**
 * Produces a quoted Solr string, with properly encoded contents, from a raw Java string.
 *
 * @param value the raw unencoded string
 * @return the encoded and quoted Solr string
 */
fun quoted(value: String) = "$SOLR_QUOTE${encoded(value)}$SOLR_QUOTE"

/**
 * Produces an encoded Solr string from a raw Java string.
 *
 * @param value the raw unencoded string
 * @return the encoded Solr string
 */
fun encoded(value: String) = escape(escape(value, SOLR_ESCAPE), SOLR_QUOTE)

/**
 * Replaces all occurrences of a given character with the correct Solr escape sequence.
 * N.B. Always start by escaping the escape character itself, only then escape any other characters.
 *
 * @param value the string that may contain the raw unescaped characters
 * @param c      the character that will be escaped
 * @return the string with the Solr escaped characters
 */
fun escape(value: String, c: Char) = value.replace(c.toString(), SOLR_ESCAPE.toString() + c)
