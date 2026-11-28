/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent
import java.net.URI

class ConfirmationOfReceiptEmailSkipped(
    val zaakIdentificatie: String?,
    val zaaktypeUri: URI?,
    val skipReason: ConfirmationOfReceiptSkipReason,
    val templateName: String? = null
) : LogEvent("Skipping automatic email confirmation")
