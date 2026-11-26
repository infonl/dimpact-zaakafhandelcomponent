/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.UUID

sealed class FakeZaakEvent(description: String, val zaakUuid: UUID) : LogEvent(description) {
    class FakeZaakReopened(zaakUuid: UUID) : FakeZaakEvent("Zaak reopened", zaakUuid)
}
