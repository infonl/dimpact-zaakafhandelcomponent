/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.constraints.NotBlank
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestZaakInitiatorGegevens(
    var identificatieType: IdentificatieType,

    @field:NotBlank
    var identificatie: String,

    /**
     * Toelichting is only required when updating an existing initiator, not when creating a new one.
     */
    var toelichting: String?,

    var zaakUUID: UUID
)
