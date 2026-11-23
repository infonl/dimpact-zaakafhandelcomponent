/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.log

import nl.info.zac.log.LogEvent

class SolrCoreNotAvailable(
    val solrCore: String,
    val errorType: String,
    val waitSeconds: Long
) : LogEvent("Solr core is not available yet; waiting for it to become available")
