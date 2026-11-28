/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class MultipleBehandelaarRolesForZaak(
    val zaakUuid: UUID,
    val roleCount: Int
) : LogEvent("Zaak has more behandelaar roles than the one that is expected")
