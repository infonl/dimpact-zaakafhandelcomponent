/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.UUID

class FakeMailSendCompleted(
    val zaakUuid: UUID?,
    @Redacted val recipient: String?,
    val outcome: Outcome,
    val durationMs: Long
) : LogEvent("Mail send completed")
