/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn.log

import nl.info.zac.log.LogEvent

class AanvullendeInformatieTaskEvent(
    val taakId: String?,
    val state: String?,
    val eventName: String?
) : LogEvent("AanvullendeInformatie task event")
