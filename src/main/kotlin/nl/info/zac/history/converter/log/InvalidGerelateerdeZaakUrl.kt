/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter.log

import nl.info.zac.log.LogEvent

class InvalidGerelateerdeZaakUrl(
    val url: String?
) : LogEvent("Ignoring invalid gerelateerde zaak URL")
