/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.constraints.NotNull
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestDecisionCreateData(
    @field:NotNull
    var zaakUuid: UUID,

    @field:NotNull
    var besluittypeUuid: UUID,

    var toelichting: String? = null,

    var ingangsdatum: LocalDate? = null,

    var vervaldatum: LocalDate? = null,

    var publicationDate: LocalDate? = null,

    var lastResponseDate: LocalDate? = null,

    var informatieobjecten: List<UUID>? = null
)
