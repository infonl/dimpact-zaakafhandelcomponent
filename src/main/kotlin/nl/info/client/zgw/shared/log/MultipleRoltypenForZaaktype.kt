/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.log

import nl.info.zac.log.LogEvent
import java.net.URI

class MultipleRoltypenForZaaktype(
    val zaaktypeUri: URI?,
    val omschrijvingGeneriek: String
) : LogEvent("Multiple role types found for zaaktype, using the first one")
