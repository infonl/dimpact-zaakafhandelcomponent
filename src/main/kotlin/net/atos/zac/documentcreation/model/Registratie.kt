/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import jakarta.json.bind.annotation.JsonbProperty
import net.atos.client.zgw.drc.model.generated.StatusEnum
import java.net.URI
import java.time.LocalDate

class Registratie {
    @JsonbProperty("zaak")
    var zaak: URI? = null

    @JsonbProperty("informatieobjectStatus")
    var informatieObjectStatus: StatusEnum? = null

    @JsonbProperty("informatieobjecttype")
    var informatieObjectType: URI? = null

    @JsonbProperty("bronorganisatie")
    var bronOrganisatie: String? = null

    @JsonbProperty("creatiedatum")
    var creatieDatum: LocalDate? = null

    @JsonbProperty("auditToelichting")
    var auditToelichting: String? = null
}
