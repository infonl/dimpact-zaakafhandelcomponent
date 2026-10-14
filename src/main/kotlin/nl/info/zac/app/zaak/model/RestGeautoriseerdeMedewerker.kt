/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.zaak.model.GeautoriseerdeMedewerkerRol

data class RestGeautoriseerdeMedewerker(
    val id: String?,
    val naam: String?,
    val roltype: String,
    @get:JsonbProperty("isOntkoppelbaar")
    val isOntkoppelbaar: Boolean
)

fun GeautoriseerdeMedewerkerRol.toRestGeautoriseerdeMedewerker() = RestGeautoriseerdeMedewerker(
    id = rol.identificatienummer,
    naam = rol.naam,
    roltype = rol.omschrijving,
    isOntkoppelbaar = isOntkoppelbaar
)
