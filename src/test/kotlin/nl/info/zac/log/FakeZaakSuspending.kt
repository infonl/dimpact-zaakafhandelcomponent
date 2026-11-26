/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

class FakeZaakSuspending(val type: Type) : LogEvent("Fake event") {
    enum class Type { ZAAK_SUSPENDING, ZAAK_SUSPENSION_STARTED }
}
