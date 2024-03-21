/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

data class RESTZaakBetrokkene(
    val rolid: String,

    val roltype: String,

    val roltoelichting: String,

    val type: String,

    val identificatie: String
)
