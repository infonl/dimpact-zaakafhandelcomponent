/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent
import java.net.URI

class MultipleRoltypenFound(
    val zaaktypeUri: URI?,
    val roltypeOmschrijving: String?,
    val isGenericRole: Boolean,
    val zaakIdentificatie: String?,
    val usedRoltypeOmschrijving: String?
) : LogEvent("Multiple roltypen found for the zaaktype; using the first one")
