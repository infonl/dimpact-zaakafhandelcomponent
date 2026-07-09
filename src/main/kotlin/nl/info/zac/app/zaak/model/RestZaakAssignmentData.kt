/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.validation.constraints.Size
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestZaakAssignmentData(
    var zaakUUID: UUID,

    /**
     * Since this is used for the 'identificatie' field in
     * [net.atos.client.zgw.zrc.model.OrganisatorischeEenheid]
     * we need to make sure it adheres to the same constraints.
     */
    @field:Size(max = 24)
    @field:JsonbProperty("groepId")
    var groupId: String,

    @field:JsonbProperty("behandelaarGebruikersnaam")
    var assigneeUserName: String? = null,

    @field:JsonbProperty("reden")
    var reason: String? = null
)
