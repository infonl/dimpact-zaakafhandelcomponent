/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication.log

import nl.info.zac.log.LogEvent

class FunctioneleGebruikerFallback(
    val origin: String
) : LogEvent(
    "No user specified, fell back to the functionele gebruiker. This fallback is deprecated. " +
        "Beheerders: no action needed, please report this to the ZAC developers. " +
        "Developers: wrap the background work that leads here in runAsSystemUser or runAsLoggedInUser."
)
