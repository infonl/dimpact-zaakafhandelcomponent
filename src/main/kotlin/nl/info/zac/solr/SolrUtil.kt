/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.solr

private const val SOLR_SPECIAL_CHARS = """+\-!(){}[]^"~*?:\/"""
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
fun encoded(value: String) = value.replace(Regex("([${Regex.escape(SOLR_SPECIAL_CHARS)}])")) { "$SOLR_ESCAPE${it.value}" }

