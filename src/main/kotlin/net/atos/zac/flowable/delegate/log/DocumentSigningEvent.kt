/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate.log

import nl.info.zac.log.LogEvent

sealed class DocumentSigningEvent(
    description: String,
    val informatieobjectIdentificatie: String?,
    val zaakIdentificatie: String?
) : LogEvent(description) {
    class DocumentAlreadySigned(informatieobjectIdentificatie: String?, zaakIdentificatie: String?) :
        DocumentSigningEvent("Document is already signed", informatieobjectIdentificatie, zaakIdentificatie)

    class DocumentSigningStarted(informatieobjectIdentificatie: String?, zaakIdentificatie: String?) :
        DocumentSigningEvent("Document signing started", informatieobjectIdentificatie, zaakIdentificatie)

    class DocumentSigningDenied(informatieobjectIdentificatie: String?, zaakIdentificatie: String?) :
        DocumentSigningEvent("Document signing denied", informatieobjectIdentificatie, zaakIdentificatie)
}
