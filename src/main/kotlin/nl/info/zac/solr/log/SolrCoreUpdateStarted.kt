/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.log

import nl.info.zac.log.LogEvent

class SolrCoreUpdateStarted(
    val solrCore: String,
    val version: Int
) : LogEvent("Solr core update to version started")
