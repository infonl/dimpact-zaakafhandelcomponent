/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import jakarta.json.bind.annotation.JsonbProperty

data class Data(
    @field:JsonbProperty("startformulier")
    val startformulierData: StartformulierData? = null,

    @field:JsonbProperty("zaak")
    val zaakData: ZaakData,

    @field:JsonbProperty("taak")
    val taakData: TaakData? = null,

    @field:JsonbProperty("gebruiker")
    val gebruikerData: GebruikerData,

    @field:JsonbProperty("aanvrager")
    val aanvragerData: AanvragerData? = null
)
