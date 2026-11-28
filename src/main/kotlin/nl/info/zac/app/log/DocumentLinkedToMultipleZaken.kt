/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class DocumentLinkedToMultipleZaken(
    val informatieobjectUuid: UUID,
    val zaakCount: Int,
    val zaakUuid: UUID?
) : LogEvent("Document is linked to more than one zaak; using the first one to determine the document rights")
