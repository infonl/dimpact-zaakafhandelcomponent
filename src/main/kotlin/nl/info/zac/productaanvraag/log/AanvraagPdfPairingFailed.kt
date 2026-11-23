/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent
import java.net.URI

class AanvraagPdfPairingFailed(
    val pdf: URI?,
    val zaakIdentificatie: String?
) : LogEvent("Failed to pair aanvraag PDF with zaak")
