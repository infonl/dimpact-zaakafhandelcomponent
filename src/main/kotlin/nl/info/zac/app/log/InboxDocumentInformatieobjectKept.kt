/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class InboxDocumentInformatieobjectKept(
    val informatieobjectIdentificatie: String?,
    val zaakUuid: UUID
) : LogEvent("Deleting inbox document but not the related informatieobject, because it is linked to a zaak")
