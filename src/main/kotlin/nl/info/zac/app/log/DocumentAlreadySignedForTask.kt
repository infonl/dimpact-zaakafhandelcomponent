/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent

class DocumentAlreadySignedForTask(
    val informatieobjectIdentificatie: String?
) : LogEvent("Enkelvoudiginformatie object has already been signed")
