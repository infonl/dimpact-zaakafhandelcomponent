/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.log

import nl.info.zac.log.LogEvent

class SolrCoreVersionChecked(
    val solrCore: String,
    val currentVersion: Int,
    val isUpToDate: Boolean
) : LogEvent("Current version of the Solr core")
