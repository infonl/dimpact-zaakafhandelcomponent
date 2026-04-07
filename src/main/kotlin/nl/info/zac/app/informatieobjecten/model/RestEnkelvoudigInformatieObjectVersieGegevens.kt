/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.FormParam
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.zac.app.configuration.model.RestTaal
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestEnkelvoudigInformatieObjectVersieGegevens(
    @field:FormParam("uuid")
    var uuid: UUID? = null,

    @field:FormParam("zaakUuid")
    var zaakUuid: UUID? = null,

    @field:FormParam("titel")
    var titel: String? = null,

    @field:FormParam("vertrouwelijkheidaanduiding")
    var vertrouwelijkheidaanduiding: String? = null,

    @field:FormParam("auteur")
    var auteur: String? = null,

    @field:FormParam("status")
    var status: StatusEnum? = null,

    @field:FormParam("taal")
    var taal: RestTaal? = null,

    @field:FormParam("formaat")
    var formaat: String? = null,

    @field:FormParam("beschrijving")
    var beschrijving: String? = null,

    @field:FormParam("verzenddatum")
    var verzenddatum: LocalDate? = null,

    @field:FormParam("ontvangstdatum")
    var ontvangstdatum: LocalDate? = null,

    @field:FormParam("toelichting")
    var toelichting: String? = null,

    @field:NotNull
    @field:FormParam("informatieobjectTypeUUID")
    var informatieobjectTypeUUID: UUID? = null
) : RestEnkelvoudigInformatieFileUpload()
