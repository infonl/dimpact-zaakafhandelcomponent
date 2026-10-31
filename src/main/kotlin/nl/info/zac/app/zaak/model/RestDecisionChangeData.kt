/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.constraints.NotNull
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestDecisionChangeData(
    @field:NotNull
    var besluitUuid: UUID,

    @field:NotNull
    var resultaattypeUuid: UUID,

    var toelichting: String? = null,

    var ingangsdatum: LocalDate? = null,

    var vervaldatum: LocalDate? = null,

    var publicationDate: LocalDate? = null,

    var lastResponseDate: LocalDate? = null,

    var informatieobjecten: List<UUID>? = null,

    var reden: String? = null
)

fun Besluit.updateDecisionWithDecisionChangeData(besluitWijzigenGegevens: RestDecisionChangeData) =
    this.apply {
        toelichting = besluitWijzigenGegevens.toelichting
        ingangsdatum = besluitWijzigenGegevens.ingangsdatum
        vervaldatum = besluitWijzigenGegevens.vervaldatum
        vervaldatum?.apply {
            vervalreden = VervalredenEnum.TIJDELIJK
        }
        publicatiedatum = besluitWijzigenGegevens.publicationDate
        uiterlijkeReactiedatum = besluitWijzigenGegevens.lastResponseDate
    }
