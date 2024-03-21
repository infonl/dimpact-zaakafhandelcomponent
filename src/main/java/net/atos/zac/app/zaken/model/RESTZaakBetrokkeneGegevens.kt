/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import net.atos.zac.app.klanten.model.klant.IdentificatieType
import java.util.*

data class RESTZaakBetrokkeneGegevens(
    val zaakUUID: @NotNull UUID,
    val roltypeUUID: @NotNull UUID,
    val roltoelichting: @NotBlank String,
    var betrokkeneIdentificatieType: @NotNull IdentificatieType,
    var betrokkeneIdentificatie: @NotBlank String
)
