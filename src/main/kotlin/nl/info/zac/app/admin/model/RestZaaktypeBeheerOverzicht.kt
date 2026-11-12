/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

/**
 * The fields of a zaaktype that the Zaakafhandelparameters page shows, filters and sorts on.
 */
@NoArgConstructor
@AllOpen
data class RestZaaktypeBeheerOverzicht(
    var uuid: UUID,
    var identificatie: String? = null,
    var omschrijving: String,
    var doel: String? = null,

    @get:JsonbProperty("isNuGeldig")
    @set:JsonbProperty("isNuGeldig")
    var isNuGeldig: Boolean = false,

    var beginGeldigheid: LocalDate,
    var eindeGeldigheid: LocalDate? = null
)

fun ZaakType.toRestZaaktypeBeheerOverzicht() = RestZaaktypeBeheerOverzicht(
    uuid = url.extractUuid(),
    identificatie = identificatie,
    omschrijving = omschrijving,
    doel = doel,
    isNuGeldig = isNuGeldig(),
    beginGeldigheid = beginGeldigheid,
    eindeGeldigheid = eindeGeldigheid
)
