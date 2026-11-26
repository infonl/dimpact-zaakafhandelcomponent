/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate.log

import nl.info.zac.log.LogEvent
import nl.info.zac.log.Redacted

class ZaakMailSendStarted(
    val zaakIdentificatie: String?,
    val templateName: String,
    @Redacted val recipient: String
) : LogEvent("Zaak mail send started")
