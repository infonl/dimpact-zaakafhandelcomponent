/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.personen

import net.atos.client.brp.model.generated.PersonenQuery
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummer
import net.atos.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatum
import net.atos.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijving
import net.atos.client.brp.model.generated.ZoekMetPostcodeEnHuisnummer
import net.atos.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijving
import org.apache.commons.lang3.StringUtils
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
        StringUtils.isNotBlank(this.bsn) -> RaadpleegMetBurgerservicenummer().apply {
            addBurgerservicenummerItem(this@toPersonenQuery.bsn)
        }
        StringUtils.isNotBlank(this.geslachtsnaam) && this.geboortedatum != null ->
            ZoekMetGeslachtsnaamEnGeboortedatum().apply {
                geslachtsnaam = this@toPersonenQuery.geslachtsnaam
                geboortedatum = this@toPersonenQuery.geboortedatum
                voornamen = this@toPersonenQuery.voornamen
                voorvoegsel = this@toPersonenQuery.voorvoegsel
            }
        StringUtils.isNotBlank(this.geslachtsnaam) && StringUtils.isNotBlank(this.voornamen) &&
            StringUtils.isNotBlank(this.gemeenteVanInschrijving) ->
            ZoekMetNaamEnGemeenteVanInschrijving().apply {
                geslachtsnaam = this@toPersonenQuery.geslachtsnaam
                voornamen = this@toPersonenQuery.voornamen
                gemeenteVanInschrijving = this@toPersonenQuery.gemeenteVanInschrijving
                voorvoegsel = this@toPersonenQuery.voorvoegsel
            }
        StringUtils.isNotBlank(this.postcode) && this.huisnummer != null ->
            ZoekMetPostcodeEnHuisnummer().apply {
                postcode = this@toPersonenQuery.postcode
                huisnummer = this@toPersonenQuery.huisnummer
            }
        StringUtils.isNotBlank(this.straat) && this.huisnummer != null &&
            StringUtils.isNotBlank(this.gemeenteVanInschrijving) ->
            ZoekMetStraatHuisnummerEnGemeenteVanInschrijving().apply {
                straat = this@toPersonenQuery.straat
                huisnummer = this@toPersonenQuery.huisnummer
                gemeenteVanInschrijving = this@toPersonenQuery.gemeenteVanInschrijving
            }
        else -> throw IllegalArgumentException("Ongeldige combinatie van zoek parameters")
    }
