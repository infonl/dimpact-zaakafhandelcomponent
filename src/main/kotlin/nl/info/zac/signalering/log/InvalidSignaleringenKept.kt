/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.log

import nl.info.zac.log.LogEvent

class InvalidSignaleringenKept(
    val keptCount: Int
) : LogEvent("Kept invalid signaleringen, because they were stored again or deleted after they were checked")
