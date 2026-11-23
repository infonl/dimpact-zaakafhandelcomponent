/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ZaakGroupNotConfigured(
    val zaakIdentificatie: String?,
    val zaakUuid: UUID,
    val bronNaam: String?,
    val bronKenmerk: String?,
    val productaanvraagType: String?
) : LogEvent("No group ID found in the zaaktype configuration; no group role was assigned")
