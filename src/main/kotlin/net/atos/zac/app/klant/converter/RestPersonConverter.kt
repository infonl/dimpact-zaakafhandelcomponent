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
import java.util.Objects

// Moet overeenkomen met wat er in convertToPersonenQuery gebeurt.
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
        Cardinaliteit.NON,
        Cardinaliteit.REQ,
        Cardinaliteit.OPT,
        Cardinaliteit.OPT,
        Cardinaliteit.REQ,
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.NON
    ),
    RestPersonenParameters(
        Cardinaliteit.NON,
        Cardinaliteit.REQ,
        Cardinaliteit.REQ,
        Cardinaliteit.OPT,
        Cardinaliteit.NON,
        Cardinaliteit.REQ,
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.NON
    ),
    RestPersonenParameters(
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.REQ,
        Cardinaliteit.REQ,
        Cardinaliteit.NON
    ),
    RestPersonenParameters(
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.NON,
        Cardinaliteit.REQ,
        Cardinaliteit.NON,
        Cardinaliteit.REQ,
        Cardinaliteit.REQ
    )
)

fun convertPersonen(personen: List<Persoon>): List<RestPersoon> =
    personen.map { convertPersoon(it) }.toList()

fun convertPersonenBeperkt(personen: List<PersoonBeperkt>): List<RestPersoon> =
    personen.map { convertPersoonBeperkt(it) }.toList()

fun convertPersoon(persoon: Persoon): RestPersoon {
    val restPersoon = RestPersoon()
    restPersoon.bsn = persoon.burgerservicenummer
    if (persoon.geslacht != null) {
        restPersoon.geslacht = convertGeslacht(persoon.geslacht)
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

fun convertPersoonBeperkt(persoon: PersoonBeperkt): RestPersoon {
    val restPersoon = RestPersoon()
    restPersoon.bsn = persoon.burgerservicenummer
    if (persoon.geslacht != null) {
        restPersoon.geslacht = convertGeslacht(persoon.geslacht)
    }
    if (persoon.naam != null) {
        restPersoon.naam = persoon.naam.volledigeNaam
    }
    if (persoon.geboorte != null) {
        restPersoon.geboortedatum = convertGeboortedatum(persoon.geboorte.datum)
    }
    if (persoon.adressering != null) {
        val adressering = persoon.adressering
        restPersoon.verblijfplaats = StringUtil.joinNonBlankWith(
            ", ",
            adressering.adresregel1,
            adressering.adresregel2,
            adressering.adresregel3
        )
    }
    return restPersoon
}

@Suppress("ReturnCount")
fun convertToPersonenQuery(parameters: RestListPersonenParameters): PersonenQuery {
    if (StringUtils.isNotBlank(parameters.bsn)) {
        val query = RaadpleegMetBurgerservicenummer()
        query.addBurgerservicenummerItem(parameters.bsn)
        return query
    }
    if (StringUtils.isNotBlank(parameters.geslachtsnaam) && parameters.geboortedatum != null) {
        val query = ZoekMetGeslachtsnaamEnGeboortedatum()
        query.geslachtsnaam = parameters.geslachtsnaam
        query.geboortedatum = parameters.geboortedatum
        query.voornamen = parameters.voornamen
        query.voorvoegsel = parameters.voorvoegsel
        return query
    }
    if (StringUtils.isNotBlank(parameters.geslachtsnaam) && StringUtils.isNotBlank(parameters.voornamen) &&
        StringUtils.isNotBlank(parameters.gemeenteVanInschrijving)
    ) {
        val query = ZoekMetNaamEnGemeenteVanInschrijving()
        query.geslachtsnaam = parameters.geslachtsnaam
        query.voornamen = parameters.voornamen
        query.gemeenteVanInschrijving = parameters.gemeenteVanInschrijving
        query.voorvoegsel = parameters.voorvoegsel
        return query
    }
    if (StringUtils.isNotBlank(parameters.postcode) && parameters.huisnummer != null) {
        val query = ZoekMetPostcodeEnHuisnummer()
        query.postcode = parameters.postcode
        query.huisnummer = parameters.huisnummer
        return query
    }
    if (
        StringUtils.isNotBlank(parameters.straat) &&
        parameters.huisnummer != null &&
        StringUtils.isNotBlank(parameters.gemeenteVanInschrijving)
    ) {
        val query = ZoekMetStraatHuisnummerEnGemeenteVanInschrijving()
        query.straat = parameters.straat
        query.huisnummer = parameters.huisnummer
        query.gemeenteVanInschrijving = parameters.gemeenteVanInschrijving
        return query
    }
    throw IllegalArgumentException("Ongeldige combinatie van zoek parameters")
}

fun convertFromPersonenQueryResponse(personenQueryResponse: PersonenQueryResponse): List<RestPersoon> {
    return when {
        personenQueryResponse is RaadpleegMetBurgerservicenummerResponse -> convertPersonen(
            personenQueryResponse.personen
        )
        personenQueryResponse is ZoekMetGeslachtsnaamEnGeboortedatumResponse -> convertPersonenBeperkt(
            personenQueryResponse.personen
        )
        personenQueryResponse is ZoekMetNaamEnGemeenteVanInschrijvingResponse ->
            convertPersonenBeperkt(personenQueryResponse.personen)
        personenQueryResponse is ZoekMetNummeraanduidingIdentificatieResponse ->
            convertPersonenBeperkt(personenQueryResponse.personen)
        personenQueryResponse is ZoekMetPostcodeEnHuisnummerResponse -> convertPersonenBeperkt(
            personenQueryResponse.personen
        )
        personenQueryResponse is ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse ->
            convertPersonenBeperkt(personenQueryResponse.personen)
        else -> emptyList()
    }
}

private fun convertGeslacht(geslacht: Waardetabel): String =
    if (StringUtils.isNotBlank(geslacht.omschrijving)) geslacht.omschrijving else geslacht.code

private fun convertGeboortedatum(abstractDatum: AbstractDatum): String? {
    return when {
        abstractDatum is VolledigeDatum -> abstractDatum.datum.toString()
        abstractDatum is JaarMaandDatum -> String.format(
            "%d2-%d4",
            abstractDatum.maand,
            abstractDatum.jaar
        )
        abstractDatum is JaarDatum -> String.format(
            "%d4",
            abstractDatum.jaar
        )
        abstractDatum is DatumOnbekend -> ONBEKEND
        else -> null
    }
}

private fun convertVerblijfplaats(abstractVerblijfplaats: AbstractVerblijfplaats): String? {
    return when {
        abstractVerblijfplaats is Adres -> abstractVerblijfplaats.verblijfadres?.let {
            convertVerblijfadresBinnenland(it)
        }
        abstractVerblijfplaats is VerblijfplaatsBuitenland -> abstractVerblijfplaats.verblijfadres?.let {
            convertVerblijfadresBuitenland(it)
        }
        abstractVerblijfplaats is VerblijfplaatsOnbekend -> ONBEKEND
        else -> null
    }
}

private fun convertVerblijfadresBinnenland(verblijfadresBinnenland: VerblijfadresBinnenland): String {
    val adres = StringUtils.replace(
        StringUtil.joinNonBlankWith(
            StringUtil.NON_BREAKING_SPACE,
            verblijfadresBinnenland.officieleStraatnaam,
            Objects.toString(verblijfadresBinnenland.huisnummer, null),
            verblijfadresBinnenland.huisnummertoevoeging,
            verblijfadresBinnenland.huisletter
        ),
        StringUtils.SPACE,
        StringUtil.NON_BREAKING_SPACE
    )
    val postcode =
        StringUtils.replace(verblijfadresBinnenland.postcode, StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
    val woonplaats =
        StringUtils.replace(verblijfadresBinnenland.woonplaats, StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
    return StringUtil.joinNonBlankWith(", ", adres, postcode, woonplaats)
}

private fun convertVerblijfadresBuitenland(verblijfadresBuitenland: VerblijfadresBuitenland): String {
    return StringUtil.joinNonBlankWith(
        ", ",
        verblijfadresBuitenland.regel1,
        verblijfadresBuitenland.regel2,
        verblijfadresBuitenland.regel3
    )
}
