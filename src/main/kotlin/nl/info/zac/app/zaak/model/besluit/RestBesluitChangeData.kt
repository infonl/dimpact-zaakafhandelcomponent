/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model.besluit

import jakarta.validation.constraints.NotNull
import nl.info.client.zgw.brc.model.NillableDatesBesluitPatch
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestBesluitChangeData(
    @field:NotNull
    var besluitUuid: UUID,

    var toelichting: String? = null,

    var ingangsdatum: LocalDate? = null,

    var vervaldatum: LocalDate? = null,

    var publicationDate: LocalDate? = null,

    var lastResponseDate: LocalDate? = null,

    var informatieobjecten: List<UUID>? = null,

    @field:NotNull
    var reden: String
)

fun RestBesluitChangeData.toBesluitPatch() = NillableDatesBesluitPatch(
    toelichting = toelichting,
    ingangsdatum = ingangsdatum,
    vervaldatum = vervaldatum,
    // The ZGW vervalreden is not nullable; clearing the vervaldatum resets it to the blank value.
    vervalreden = vervaldatum?.let { VervalredenEnum.TIJDELIJK } ?: VervalredenEnum.EMPTY,
    publicatiedatum = publicationDate,
    uiterlijkeReactiedatum = lastResponseDate
)
