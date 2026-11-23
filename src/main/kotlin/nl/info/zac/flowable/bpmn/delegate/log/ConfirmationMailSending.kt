/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.delegate.log

import nl.info.zac.log.LogEvent
import nl.info.zac.log.Redacted

class ConfirmationMailSending(
    val zaakIdentificatie: String?,
    val templateName: String,
    val emailSource: EmailSource,
    @Redacted val recipient: String?
) : LogEvent("Sending confirmation mail")
