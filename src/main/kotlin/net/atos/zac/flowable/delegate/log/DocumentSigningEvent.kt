/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate.log

import nl.info.zac.log.LogEvent

class DocumentSigningEvent(
    val type: Type,
    val informatieobjectIdentificatie: String?,
    val zaakIdentificatie: String?
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        DOCUMENT_ALREADY_SIGNED("Document is already signed"),
        DOCUMENT_SIGNING_STARTED("Document signing started"),
        DOCUMENT_SIGNING_DENIED("Document signing denied")
    }
}
