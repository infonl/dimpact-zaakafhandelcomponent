/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent
import java.net.URI

class BijlagePairingFailed(
    val bijlage: URI,
    val zaakUri: URI
) : LogEvent("Failed to pair bijlage with zaak")
