/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ZaakCreatingForProductaanvraag(
    val processEngine: String,
    val zaaktypeUuid: UUID
) : LogEvent("Creating a zaak for the productaanvraag")
