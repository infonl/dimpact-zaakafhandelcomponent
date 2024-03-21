/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import jakarta.validation.constraints.NotNull
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.app.admin.model.RESTZaakafhandelParameters
import java.time.LocalDate
import java.util.*

class RESTZaaktype {
    @NotNull
    var uuid: UUID? = null

    var identificatie: String? = null

    var doel: String? = null

    var omschrijving: String? = null

    var referentieproces: String? = null

    var servicenorm: Boolean = false

    var versiedatum: LocalDate? = null

    var beginGeldigheid: LocalDate? = null

    var eindeGeldigheid: LocalDate? = null

    var vertrouwelijkheidaanduiding: ZaakType.VertrouwelijkheidaanduidingEnum? = null

    var nuGeldig: Boolean = false

    var opschortingMogelijk: Boolean = false

    var verlengingMogelijk: Boolean = false

    var verlengingstermijn: Int? = null

    var zaaktypeRelaties: List<RESTZaaktypeRelatie>? = null

    var informatieobjecttypes: List<UUID>? = null

    var zaakafhandelparameters: RESTZaakafhandelParameters? = null
}
