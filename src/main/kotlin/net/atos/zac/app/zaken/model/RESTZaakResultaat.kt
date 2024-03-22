/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RESTZaakResultaat(
    var toelichting: String? = null,

    var resultaattype: RESTResultaattype? = null
)
