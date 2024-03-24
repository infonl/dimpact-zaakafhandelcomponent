/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RESTGeometry(
    var type: String,

    var point: RESTCoordinates? = null,

    var polygon: List<List<RESTCoordinates>>? = null,

    var geometrycollection: List<RESTGeometry>? = null
)
