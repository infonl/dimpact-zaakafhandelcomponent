/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.client.zgw.zrc.model.RolMedewerker

data class RestGeautoriseerdeMedewerker(
    val id: String?,
    val naam: String?,
    val roltype: String
)

fun RolMedewerker.toRestGeautoriseerdeMedewerker() = RestGeautoriseerdeMedewerker(
    id = identificatienummer,
    naam = naam,
    roltype = omschrijving
)
