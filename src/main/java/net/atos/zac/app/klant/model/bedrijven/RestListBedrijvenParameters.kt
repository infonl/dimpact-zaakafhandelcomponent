/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven


data class RestListBedrijvenParameters(
    var kvkNummer: String? = null,
    var vestigingsnummer: String? = null,
    var rsin: String? = null,
    var naam: String? = null,
    var postcode: String? = null,
    var huisnummer: Int? = null,
    var type: BedrijfType? = null,
)
