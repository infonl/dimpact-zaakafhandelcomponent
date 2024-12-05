/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import jakarta.validation.constraints.NotNull
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.brc.model.generated.VervalredenEnum
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestBesluitWijzigenGegevens(
    @field:NotNull
    var besluitUuid: UUID,

    @field:NotNull
    var resultaattypeUuid: UUID,

    var toelichting: String? = null,

    var ingangsdatum: LocalDate? = null,

    var vervaldatum: LocalDate? = null,

    var publicatiedatum: LocalDate? = null,

    var uiterlijkeReactiedatum: LocalDate? = null,

    var informatieobjecten: List<UUID>? = null,

    var reden: String? = null
)

fun Besluit.updateBesluitWithBesluitWijzigenGegevens(besluitWijzigenGegevens: RestBesluitWijzigenGegevens) =
    this.apply {
        toelichting = besluitWijzigenGegevens.toelichting
        ingangsdatum = besluitWijzigenGegevens.ingangsdatum
        vervaldatum = besluitWijzigenGegevens.vervaldatum
        vervaldatum?.apply {
            vervalreden = VervalredenEnum.TIJDELIJK
        }
        publicatiedatum = besluitWijzigenGegevens.publicatiedatum
        uiterlijkeReactiedatum = besluitWijzigenGegevens.uiterlijkeReactiedatum
    }
