/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

data class RESTZaakEigenschap(
    // TODO: not used?
    val type: String? = null,

    val naam: String,

    val waarde: String
)
