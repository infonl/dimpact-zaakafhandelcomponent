/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter.log

import nl.info.zac.log.LogEvent
import java.net.URI

class GerelateerdeZaakUnreadable(
    val zaakUri: URI
) : LogEvent("Cannot read gerelateerde zaak; showing its URL instead")
