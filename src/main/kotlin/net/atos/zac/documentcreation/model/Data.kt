/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import jakarta.json.bind.annotation.JsonbProperty

class Data {
    @JsonbProperty("startformulier")
    var startformulierData: StartformulierData? = null

    @JsonbProperty("zaak")
    var zaakData: ZaakData? = null

    @JsonbProperty("taak")
    var taakData: TaakData? = null

    @JsonbProperty("gebruiker")
    var gebruikerData: GebruikerData? = null

    @JsonbProperty("aanvrager")
    var aanvragerData: AanvragerData? = null
}
