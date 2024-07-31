/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import jakarta.json.bind.annotation.JsonbProperty
import net.atos.client.zgw.drc.model.generated.StatusEnum
import java.net.URI
import java.time.LocalDate

data class Registratie(
    @field:JsonbProperty("zaak")
    val zaak: URI,

    @field:JsonbProperty("informatieobjectStatus")
    val informatieObjectStatus: StatusEnum,

    @field:JsonbProperty("informatieobjecttype")
    val informatieObjectType: URI,

    @field:JsonbProperty("bronorganisatie")
    val bronOrganisatie: String,

    @field:JsonbProperty("creatiedatum")
    val creatieDatum: LocalDate,

    @field:JsonbProperty("auditToelichting")
    val auditToelichting: String
)
