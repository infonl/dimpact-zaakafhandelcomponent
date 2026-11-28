/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class InboxDocumentInformatieobjectReadFailed(
    val inboxDocumentId: Long?,
    val informatieobjectUuid: UUID
) : LogEvent("Error reading EnkelvoudigInformatieobject for InboxDocument")
