/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

data class RESTZaakLocatieGegevens(
    val geometrie: RESTGeometry,

    val reden: String
)
