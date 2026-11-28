/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent

class InboxDocumentAlreadyDeleted(
    val inboxDocumentId: Long
) : LogEvent("Inbox document not found; it may already have been deleted")
