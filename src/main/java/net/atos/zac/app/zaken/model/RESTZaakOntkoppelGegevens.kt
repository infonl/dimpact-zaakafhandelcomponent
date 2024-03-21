/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import java.util.*

data class RESTZaakOntkoppelGegevens(
    val zaakUuid: UUID,

    val gekoppeldeZaakIdentificatie: String,

    val relatietype: RelatieType,

    val reden: String
)
