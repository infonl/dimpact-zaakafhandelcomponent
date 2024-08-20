/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven

data class RestVestigingsprofiel(
    var adressen: List<RestKlantenAdres>? = null,
    var commercieleVestiging: Boolean = false,
    var deeltijdWerkzamePersonen: Int = 0,
    var eersteHandelsnaam: String? = null,
    var kvkNummer: String? = null,
    var rsin: String? = null,
    var sbiActiviteiten: List<String>? = null,
    var sbiHoofdActiviteit: String? = null,
    var type: String? = null,
    var totaalWerkzamePersonen: Int = 0,
    var vestigingsnummer: String? = null,
    var voltijdWerkzamePersonen: Int = 0,
    var website: String? = null
)
