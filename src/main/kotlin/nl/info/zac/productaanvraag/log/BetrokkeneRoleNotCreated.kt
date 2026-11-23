/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent

class BetrokkeneRoleNotCreated(
    val zaakIdentificatie: String?,
    val roltypeOmschrijving: String?,
    val isGenericRole: Boolean,
    val skipReason: BetrokkeneSkipReason
) : LogEvent("No betrokkene role created for the zaak")
