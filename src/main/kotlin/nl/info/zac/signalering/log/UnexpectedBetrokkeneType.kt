/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.log

import nl.info.zac.log.LogEvent

class UnexpectedBetrokkeneType(
    val betrokkeneType: String?
) : LogEvent("Unexpected betrokkene type")
