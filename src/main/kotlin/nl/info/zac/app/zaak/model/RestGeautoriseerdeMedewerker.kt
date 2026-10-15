/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.zaak.model.ZaakspecifiekGeautoriseerdeMedewerkerRol

data class RestGeautoriseerdeMedewerker(
    val id: String,
    val naam: String?,
    val roltype: String,
    @get:JsonbProperty("isDetachable")
    val isDetachable: Boolean
)

fun ZaakspecifiekGeautoriseerdeMedewerkerRol.toRestGeautoriseerdeMedewerker() = RestGeautoriseerdeMedewerker(
    id = medewerkerId,
    naam = rol.naam,
    roltype = rol.omschrijving,
    isDetachable = isDetachable
)
