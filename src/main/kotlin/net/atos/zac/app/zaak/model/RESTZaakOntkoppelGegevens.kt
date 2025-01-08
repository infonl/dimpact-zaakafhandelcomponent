/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RESTZaakOntkoppelGegevens(
    var zaakUuid: UUID,

    var gekoppeldeZaakIdentificatie: String,

    var relatietype: RelatieType,

    var reden: String
)
