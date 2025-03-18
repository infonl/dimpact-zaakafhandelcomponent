/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.personen

import nl.info.client.brp.model.generated.PersonenQuery
import nl.info.client.brp.model.generated.RaadpleegMetBurgerservicenummer
import nl.info.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatum
import nl.info.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijving
import nl.info.client.brp.model.generated.ZoekMetPostcodeEnHuisnummer
import nl.info.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijving
import java.time.LocalDate

data class RestListPersonenParameters(
    var bsn: String? = null,
    var geslachtsnaam: String? = null,
    var voornamen: String? = null,
    var voorvoegsel: String? = null,
    var geboortedatum: LocalDate? = null,
    var gemeenteVanInschrijving: String? = null,
    var postcode: String? = null,
    var huisnummer: Int? = null,
    var straat: String? = null
)

@Suppress("ReturnCount", "CyclomaticComplexMethod")
fun RestListPersonenParameters.toPersonenQuery(): PersonenQuery =
    when {
        this.bsn?.isNotBlank() == true -> RaadpleegMetBurgerservicenummer().apply {
            addBurgerservicenummerItem(this@toPersonenQuery.bsn)
        }
        this.geslachtsnaam?.isNotBlank() == true && this.geboortedatum != null ->
            ZoekMetGeslachtsnaamEnGeboortedatum().apply {
                geslachtsnaam = this@toPersonenQuery.geslachtsnaam
                geboortedatum = this@toPersonenQuery.geboortedatum
                voornamen = this@toPersonenQuery.voornamen
                voorvoegsel = this@toPersonenQuery.voorvoegsel
                inclusiefOverledenPersonen = true
            }
        this.geslachtsnaam?.isNotBlank() == true &&
            this.voornamen?.isNotBlank() == true &&
            this.gemeenteVanInschrijving?.isNotBlank() == true ->
            ZoekMetNaamEnGemeenteVanInschrijving().apply {
                geslachtsnaam = this@toPersonenQuery.geslachtsnaam
                voornamen = this@toPersonenQuery.voornamen
                gemeenteVanInschrijving = this@toPersonenQuery.gemeenteVanInschrijving
                voorvoegsel = this@toPersonenQuery.voorvoegsel
                inclusiefOverledenPersonen = true
            }
        this.postcode?.isNotBlank() == true && this.huisnummer != null ->
            ZoekMetPostcodeEnHuisnummer().apply {
                postcode = this@toPersonenQuery.postcode
                huisnummer = this@toPersonenQuery.huisnummer
                inclusiefOverledenPersonen = true
            }
        this.straat?.isNotBlank() == true &&
            this.huisnummer != null &&
            this.gemeenteVanInschrijving?.isNotBlank() == true ->
            ZoekMetStraatHuisnummerEnGemeenteVanInschrijving().apply {
                straat = this@toPersonenQuery.straat
                huisnummer = this@toPersonenQuery.huisnummer
                gemeenteVanInschrijving = this@toPersonenQuery.gemeenteVanInschrijving
                inclusiefOverledenPersonen = true
            }
        else -> throw IllegalArgumentException("Ongeldige combinatie van zoek parameters")
    }
