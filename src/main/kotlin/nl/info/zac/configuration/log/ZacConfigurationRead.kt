/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration.log

import nl.info.zac.log.LogEvent

@Suppress("LongParameterList")
class ZacConfigurationRead(
    val bronOrganisatieRsin: String,
    val catalogusDomein: String,
    val contextUrl: String,
    val gemeenteCode: String,
    val gemeenteMail: String,
    val gemeenteNaam: String,
    val verantwoordelijkeOrganisatieRsin: String,
    val zgwApiClientMpRestUrl: String,
    val brpConfiguration: String
) : LogEvent("ZAC configuration environment variables")
