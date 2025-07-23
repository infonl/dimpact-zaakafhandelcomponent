/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestZaakInitiatorGegevens(
    var betrokkeneIdentificatie: BetrokkeneIdentificatie,

    /**
     * Toelichting is only required when updating an existing initiator, not when creating a new one.
     */
    var toelichting: String?,

    var zaakUUID: UUID
)
