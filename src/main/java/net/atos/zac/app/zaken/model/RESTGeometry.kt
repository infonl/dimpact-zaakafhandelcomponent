/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

data class RESTGeometry(
    val type: String,

    val point: RESTCoordinates? = null,

    val polygon: List<List<RESTCoordinates>>? = null,

    val geometrycollection: List<RESTGeometry>? = null
)
