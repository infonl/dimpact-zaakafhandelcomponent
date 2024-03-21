/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import java.net.URI
import java.util.*

data class RESTBesluittype(
    val id: UUID,

    val naam: String,

    val toelichting: String,

    val informatieobjecttypen: List<URI>
)
