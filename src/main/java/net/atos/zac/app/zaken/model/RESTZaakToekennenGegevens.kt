/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import jakarta.annotation.Nullable
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.*

class RESTZaakToekennenGegevens {
    @NotNull
    var zaakUUID: UUID? = null

    /**
     * Since this is used for the 'identificatie' field in
     * [net.atos.client.zgw.zrc.model.OrganisatorischeEenheid]
     * we need to make sure it adheres to the same constraints.
     */
    @Nullable
    @Size(max = 24)
    var groepId: String? = null

    var behandelaarGebruikersnaam: String? = null

    var reden: String? = null
}
