/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.besluit.log

import nl.info.zac.log.LogEvent

class UnknownBesluitWithdrawalReason(
    val withdrawalReason: String?
) : LogEvent("Unknown besluit withdrawal reason")
