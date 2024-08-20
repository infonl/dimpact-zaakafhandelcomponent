/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

@file:Suppress("TooManyFunctions")

package net.atos.zac.app.klant.converter

import net.atos.client.brp.model.generated.AbstractDatum
import net.atos.client.brp.model.generated.AbstractVerblijfplaats
import net.atos.client.brp.model.generated.Adres
import net.atos.client.brp.model.generated.DatumOnbekend
import net.atos.client.brp.model.generated.JaarDatum
import net.atos.client.brp.model.generated.JaarMaandDatum
import net.atos.client.brp.model.generated.PersonenQuery
import net.atos.client.brp.model.generated.PersonenQueryResponse
import net.atos.client.brp.model.generated.Persoon
import net.atos.client.brp.model.generated.PersoonBeperkt
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummer
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse
import net.atos.client.brp.model.generated.VerblijfadresBinnenland
import net.atos.client.brp.model.generated.VerblijfadresBuitenland
import net.atos.client.brp.model.generated.VerblijfplaatsBuitenland
import net.atos.client.brp.model.generated.VerblijfplaatsOnbekend
import net.atos.client.brp.model.generated.VolledigeDatum
import net.atos.client.brp.model.generated.Waardetabel
import net.atos.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatum
import net.atos.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatumResponse
import net.atos.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijving
import net.atos.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijvingResponse
import net.atos.client.brp.model.generated.ZoekMetNummeraanduidingIdentificatieResponse
import net.atos.client.brp.model.generated.ZoekMetPostcodeEnHuisnummer
import net.atos.client.brp.model.generated.ZoekMetPostcodeEnHuisnummerResponse
import net.atos.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijving
import net.atos.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse
import net.atos.zac.app.klant.model.personen.RestListPersonenParameters
import net.atos.zac.app.klant.model.personen.RestPersonenParameters
import net.atos.zac.app.klant.model.personen.RestPersonenParameters.Cardinaliteit
import net.atos.zac.app.klant.model.personen.RestPersoon
import net.atos.zac.util.StringUtil
import net.atos.zac.util.StringUtil.ONBEKEND
import org.apache.commons.lang3.StringUtils
import java.util.Locale
import java.util.Objects

/**
 * Needs to correspond to the implementation [convertToPersonenQuery] function.
 */
val VALID_PERSONEN_QUERIES = listOf(
    RestPersonenParameters(
        bsn = Cardinaliteit.REQ,
        geslachtsnaam = Cardinaliteit.NON,
        voornamen = Cardinaliteit.NON,
        voorvoegsel = Cardinaliteit.NON,
        geboortedatum = Cardinaliteit.NON,
        gemeenteVanInschrijving = Cardinaliteit.NON,
        postcode = Cardinaliteit.NON,
        huisnummer = Cardinaliteit.NON,
        straat = Cardinaliteit.NON
    ),
    RestPersonenParameters(
        bsn = Cardinaliteit.NON,
        geslachtsnaam = Cardinaliteit.REQ,
        voornamen = Cardinaliteit.OPT,
        voorvoegsel = Cardinaliteit.OPT,
        geboortedatum = Cardinaliteit.REQ,
        gemeenteVanInschrijving = Cardinaliteit.NON,
        postcode = Cardinaliteit.NON,
        huisnummer = Cardinaliteit.NON,
        straat = Cardinaliteit.NON
    ),
    RestPersonenParameters(
        bsn = Cardinaliteit.NON,
        geslachtsnaam = Cardinaliteit.REQ,
        voornamen = Cardinaliteit.REQ,
        voorvoegsel = Cardinaliteit.OPT,
        geboortedatum = Cardinaliteit.NON,
        gemeenteVanInschrijving = Cardinaliteit.REQ,
        postcode = Cardinaliteit.NON,
        huisnummer = Cardinaliteit.NON,
        straat = Cardinaliteit.NON
    ),
    RestPersonenParameters(
        bsn = Cardinaliteit.NON,
        geslachtsnaam = Cardinaliteit.NON,
        voornamen = Cardinaliteit.NON,
        voorvoegsel = Cardinaliteit.NON,
        geboortedatum = Cardinaliteit.NON,
        gemeenteVanInschrijving = Cardinaliteit.NON,
        postcode = Cardinaliteit.REQ,
        huisnummer = Cardinaliteit.REQ,
        straat = Cardinaliteit.NON
    ),
    RestPersonenParameters(
        bsn = Cardinaliteit.NON,
        geslachtsnaam = Cardinaliteit.NON,
        voornamen = Cardinaliteit.NON,
        voorvoegsel = Cardinaliteit.NON,
        geboortedatum = Cardinaliteit.NON,
        gemeenteVanInschrijving = Cardinaliteit.REQ,
        postcode = Cardinaliteit.NON,
        huisnummer = Cardinaliteit.REQ,
        straat = Cardinaliteit.REQ
    )
)

fun List<Persoon>.toRestPersons(): List<RestPersoon> =
    this.map { convertPersoon(it) }

fun convertPersonenBeperkt(personen: List<PersoonBeperkt>): List<RestPersoon> =
    personen.map { it.toRestPerson() }

fun convertPersoon(persoon: Persoon): RestPersoon {
    val restPersoon = RestPersoon()
    restPersoon.bsn = persoon.burgerservicenummer
    if (persoon.geslacht != null) {
        restPersoon.geslacht = persoon.geslacht.toDescription()
    }
    if (persoon.naam != null) {
        restPersoon.naam = persoon.naam.volledigeNaam
    }
    if (persoon.geboorte != null) {
        restPersoon.geboortedatum = convertGeboortedatum(persoon.geboorte.datum)
    }
    if (persoon.verblijfplaats != null) {
        restPersoon.verblijfplaats = convertVerblijfplaats(persoon.verblijfplaats)
    }
    return restPersoon
}

fun PersoonBeperkt.toRestPerson(): RestPersoon {
    val restPersoon = RestPersoon()
    restPersoon.bsn = this.burgerservicenummer
    if (this.geslacht != null) {
        restPersoon.geslacht = this.geslacht.toDescription()
    }
    if (this.naam != null) {
        restPersoon.naam = this.naam.volledigeNaam
    }
    if (this.geboorte != null) {
        restPersoon.geboortedatum = convertGeboortedatum(this.geboorte.datum)
    }
    if (this.adressering != null) {
        val adressering = this.adressering
        restPersoon.verblijfplaats = StringUtil.joinNonBlankWith(
            ", ",
            adressering.adresregel1,
            adressering.adresregel2,
            adressering.adresregel3
        )
    }
    return restPersoon
}

@Suppress("ReturnCount", "CyclomaticComplexMethod")
fun convertToPersonenQuery(parameters: RestListPersonenParameters): PersonenQuery =
    when {
        StringUtils.isNotBlank(parameters.bsn) -> RaadpleegMetBurgerservicenummer().apply {
            addBurgerservicenummerItem(parameters.bsn)
        }
        StringUtils.isNotBlank(parameters.geslachtsnaam) && parameters.geboortedatum != null ->
            ZoekMetGeslachtsnaamEnGeboortedatum().apply {
                geslachtsnaam = parameters.geslachtsnaam
                geboortedatum = parameters.geboortedatum
                voornamen = parameters.voornamen
                voorvoegsel = parameters.voorvoegsel
            }
        StringUtils.isNotBlank(parameters.geslachtsnaam) && StringUtils.isNotBlank(parameters.voornamen) &&
            StringUtils.isNotBlank(parameters.gemeenteVanInschrijving) ->
            ZoekMetNaamEnGemeenteVanInschrijving().apply {
                geslachtsnaam = parameters.geslachtsnaam
                voornamen = parameters.voornamen
                gemeenteVanInschrijving = parameters.gemeenteVanInschrijving
                voorvoegsel = parameters.voorvoegsel
            }
        StringUtils.isNotBlank(parameters.postcode) && parameters.huisnummer != null ->
            ZoekMetPostcodeEnHuisnummer().apply {
                postcode = parameters.postcode
                huisnummer = parameters.huisnummer
            }
        StringUtils.isNotBlank(parameters.straat) && parameters.huisnummer != null &&
            StringUtils.isNotBlank(parameters.gemeenteVanInschrijving) ->
            ZoekMetStraatHuisnummerEnGemeenteVanInschrijving().apply {
                straat = parameters.straat
                huisnummer = parameters.huisnummer
                gemeenteVanInschrijving = parameters.gemeenteVanInschrijving
            }
        else -> throw IllegalArgumentException("Ongeldige combinatie van zoek parameters")
    }

fun convertFromPersonenQueryResponse(personenQueryResponse: PersonenQueryResponse): List<RestPersoon> =
    when (personenQueryResponse) {
        is RaadpleegMetBurgerservicenummerResponse -> personenQueryResponse.personen.toRestPersons()
        is ZoekMetGeslachtsnaamEnGeboortedatumResponse -> convertPersonenBeperkt(personenQueryResponse.personen)
        is ZoekMetNaamEnGemeenteVanInschrijvingResponse -> convertPersonenBeperkt(personenQueryResponse.personen)
        is ZoekMetNummeraanduidingIdentificatieResponse -> convertPersonenBeperkt(personenQueryResponse.personen)
        is ZoekMetPostcodeEnHuisnummerResponse -> convertPersonenBeperkt(personenQueryResponse.personen)
        is ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse -> convertPersonenBeperkt(
            personenQueryResponse.personen
        )
        else -> emptyList()
    }

private fun Waardetabel.toDescription(): String =
    if (StringUtils.isNotBlank(this.omschrijving)) this.omschrijving else this.code

private fun convertGeboortedatum(abstractDatum: AbstractDatum): String? =
    when (abstractDatum) {
        is VolledigeDatum -> abstractDatum.datum.toString()
        is JaarMaandDatum -> String.format(
            Locale.getDefault(),
            "%d2-%d4",
            abstractDatum.maand,
            abstractDatum.jaar
        )

        is JaarDatum -> String.format(
            Locale.getDefault(),
            "%d4",
            abstractDatum.jaar
        )

        is DatumOnbekend -> ONBEKEND
        else -> null
    }

private fun convertVerblijfplaats(abstractVerblijfplaats: AbstractVerblijfplaats): String? =
    when (abstractVerblijfplaats) {
        is Adres -> abstractVerblijfplaats.verblijfadres?.toCommaSeparatedString()

        is VerblijfplaatsBuitenland -> abstractVerblijfplaats.verblijfadres?.toCommaSeparatedString()

        is VerblijfplaatsOnbekend -> ONBEKEND
        else -> null
    }

private fun VerblijfadresBinnenland.toCommaSeparatedString(): String {
    val adres = StringUtils.replace(
        StringUtil.joinNonBlankWith(
            StringUtil.NON_BREAKING_SPACE,
            this.officieleStraatnaam,
            Objects.toString(this.huisnummer, null),
            this.huisnummertoevoeging,
            this.huisletter
        ),
        StringUtils.SPACE,
        StringUtil.NON_BREAKING_SPACE
    )
    val postcode =
        StringUtils.replace(this.postcode, StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
    val woonplaats =
        StringUtils.replace(this.woonplaats, StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
    return StringUtil.joinNonBlankWith(", ", adres, postcode, woonplaats)
}

private fun VerblijfadresBuitenland.toCommaSeparatedString(): String {
    return StringUtil.joinNonBlankWith(
        ", ",
        this.regel1,
        this.regel2,
        this.regel3
    )
}
