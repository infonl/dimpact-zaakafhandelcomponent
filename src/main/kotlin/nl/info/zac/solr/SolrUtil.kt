/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.solr

import org.apache.solr.client.solrj.util.ClientUtils

private const val SOLR_QUOTE = '\"'

/**
 * Produces a quoted Solr string, with properly encoded contents, from a raw Java string.
 *
 * @param value the raw unencoded string
 * @return the encoded and quoted Solr string
 *
 */
fun quoted(value: String): String =
    "$SOLR_QUOTE${encoded(value)}$SOLR_QUOTE"

/**
 * Produces an encoded Solr string from a raw Java string.
 *
 * @param value the raw unencoded string
 * @return the encoded Solr string
 *
 */
fun encoded(value: String): String =
    ClientUtils.escapeQueryChars(value)
