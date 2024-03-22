/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.net.URI
import java.util.*

@AllOpen
@NoArgConstructor
data class RESTBesluittype(
    var id: UUID,

    var naam: String,

    var toelichting: String,

    var informatieobjecttypen: List<URI>
)
