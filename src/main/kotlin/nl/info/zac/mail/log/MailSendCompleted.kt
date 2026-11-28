/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mail.log

import nl.info.zac.log.LogEvent
import nl.info.zac.log.Outcome
import nl.info.zac.log.Redacted
import java.util.UUID

class MailSendCompleted(
    val zaakUuid: UUID?,
    @Redacted val recipient: String,
    val outcome: Outcome,
    val durationMs: Long,
    val errorType: String? = null
) : LogEvent("Mail send completed")
