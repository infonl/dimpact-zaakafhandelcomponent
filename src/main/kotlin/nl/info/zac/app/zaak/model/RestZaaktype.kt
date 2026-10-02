/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.app.admin.model.RestZaaktypeConfiguration
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestZaaktype(
    var uuid: UUID,

    var identificatie: String? = null,

    var doel: String? = null,

    var omschrijving: String? = null,

    var referentieproces: String? = null,

    var hasServicenorm: Boolean? = null,

    var versiedatum: LocalDate? = null,

    var beginGeldigheid: LocalDate? = null,

    var eindeGeldigheid: LocalDate? = null,

    var vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding? = null,

    @get:JsonbProperty("isNuGeldig")
    @set:JsonbProperty("isNuGeldig")
    var isNuGeldig: Boolean? = null,

    @get:JsonbProperty("isOpschortingMogelijk")
    @set:JsonbProperty("isOpschortingMogelijk")
    var isOpschortingMogelijk: Boolean? = null,

    @get:JsonbProperty("isVerlengingMogelijk")
    @set:JsonbProperty("isVerlengingMogelijk")
    var isVerlengingMogelijk: Boolean? = null,

    var verlengingstermijn: Int? = null,

    var zaaktypeRelaties: List<RestZaaktypeRelatie>? = null,

    var informatieobjecttypes: List<UUID>? = null,

    var zaakafhandelparameters: RestZaaktypeConfiguration? = null
)
