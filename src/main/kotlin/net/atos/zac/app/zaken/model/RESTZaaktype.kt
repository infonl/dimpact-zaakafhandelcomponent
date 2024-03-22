/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.app.admin.model.RESTZaakafhandelParameters
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RESTZaaktype(
    var uuid: UUID,

    var identificatie: String? = null,

    var doel: String? = null,

    var omschrijving: String? = null,

    var referentieproces: String? = null,

    var servicenorm: Boolean? = null,

    var versiedatum: LocalDate? = null,

    var beginGeldigheid: LocalDate? = null,

    var eindeGeldigheid: LocalDate? = null,

    var vertrouwelijkheidaanduiding: ZaakType.VertrouwelijkheidaanduidingEnum? = null,

    var nuGeldig: Boolean? = null,

    var opschortingMogelijk: Boolean? = null,

    var verlengingMogelijk: Boolean? = null,

    var verlengingstermijn: Int? = null,

    var zaaktypeRelaties: List<RESTZaaktypeRelatie>? = null,

    var informatieobjecttypes: List<UUID>? = null,

    var zaakafhandelparameters: RESTZaakafhandelParameters? = null
)
