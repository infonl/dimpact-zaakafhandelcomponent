/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import jakarta.validation.constraints.Size
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
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
    var groepId: String? = null,

    var behandelaarGebruikersnaam: String? = null,

    var reden: String? = null
)
