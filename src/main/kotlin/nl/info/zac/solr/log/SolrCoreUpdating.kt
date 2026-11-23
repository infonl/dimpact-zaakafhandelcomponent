/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.log

import nl.info.zac.log.LogEvent

class SolrCoreUpdating(
    val solrCore: String,
    val version: Int
) : LogEvent("Updating Solr core to version")
