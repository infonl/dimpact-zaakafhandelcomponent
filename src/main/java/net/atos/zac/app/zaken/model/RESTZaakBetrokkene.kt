/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RESTZaakBetrokkene(
    var rolid: String,

    var roltype: String,

    var roltoelichting: String,

    var type: String,

    var identificatie: String
)
