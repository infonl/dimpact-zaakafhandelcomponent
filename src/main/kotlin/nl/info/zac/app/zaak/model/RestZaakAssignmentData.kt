/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.validation.constraints.Size
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie.IDENTIFICATIE_MAX_LENGTH
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestZaakAssignmentData(
    var zaakUUID: UUID,

    /**
     * Since this is used for the 'identificatie' field in
     * [nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie]
     * we need to make sure it adheres to the same constraints.
     */
    @field:Size(max = IDENTIFICATIE_MAX_LENGTH)
    @field:JsonbProperty("groepId")
    var groupId: String,

    @field:JsonbProperty("behandelaarGebruikersnaam")
    var assigneeUserName: String? = null,

    @field:JsonbProperty("reden")
    var reason: String? = null
)
