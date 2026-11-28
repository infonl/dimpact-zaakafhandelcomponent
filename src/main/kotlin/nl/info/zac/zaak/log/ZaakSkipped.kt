/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ZaakSkipped(
    val zaakUuid: UUID,
    val batchOperation: ZaakBatchOperation,
    val skipReason: ZaakSkipReason
) : LogEvent("Zaak skipped in a batch")
