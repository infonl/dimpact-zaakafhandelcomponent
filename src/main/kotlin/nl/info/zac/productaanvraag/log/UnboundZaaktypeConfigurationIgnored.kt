/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class UnboundZaaktypeConfigurationIgnored(
    val zaaktypeUuid: UUID,
    val productaanvraagType: String
) : LogEvent("Zaaktype configuration has the productaanvraag type but is not bound to a process engine, so it is ignored")
