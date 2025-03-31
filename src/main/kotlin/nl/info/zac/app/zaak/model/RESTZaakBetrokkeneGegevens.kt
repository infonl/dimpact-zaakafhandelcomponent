/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.constraints.NotBlank
import net.atos.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RESTZaakBetrokkeneGegevens(
    var zaakUUID: UUID,

    var roltypeUUID: UUID,

    var roltoelichting: String?,

    var betrokkeneIdentificatieType: IdentificatieType,

    @field:NotBlank
    var betrokkeneIdentificatie: String
)
