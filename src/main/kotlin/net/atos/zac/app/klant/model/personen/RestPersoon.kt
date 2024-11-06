/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

@file:Suppress("TooManyFunctions")

package net.atos.zac.app.klant.model.personen

import net.atos.client.brp.model.generated.AbstractDatum
import net.atos.client.brp.model.generated.AbstractVerblijfplaats
import net.atos.client.brp.model.generated.Adres
import net.atos.client.brp.model.generated.DatumOnbekend
import net.atos.client.brp.model.generated.JaarDatum
import net.atos.client.brp.model.generated.JaarMaandDatum
import net.atos.client.brp.model.generated.PersonenQueryResponse
import net.atos.client.brp.model.generated.Persoon
import net.atos.client.brp.model.generated.PersoonBeperkt
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse
import net.atos.client.brp.model.generated.VerblijfadresBinnenland
import net.atos.client.brp.model.generated.VerblijfadresBuitenland
import net.atos.client.brp.model.generated.VerblijfplaatsBuitenland
import net.atos.client.brp.model.generated.VerblijfplaatsOnbekend
import net.atos.client.brp.model.generated.VolledigeDatum
import net.atos.client.brp.model.generated.Waardetabel
import net.atos.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatumResponse
import net.atos.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijvingResponse
import net.atos.client.brp.model.generated.ZoekMetNummeraanduidingIdentificatieResponse
import net.atos.client.brp.model.generated.ZoekMetPostcodeEnHuisnummerResponse
import net.atos.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse
import net.atos.client.klant.model.DigitaalAdres
import net.atos.zac.app.klant.KlantRestService.Companion.EMAIL_SOORT_DIGITAAL_ADRES
import net.atos.zac.app.klant.KlantRestService.Companion.TELEFOON_SOORT_DIGITAAL_ADRES
import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.klant.model.klant.RestKlant
import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.util.StringUtil
import net.atos.zac.util.StringUtil.ONBEKEND
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import java.util.EnumSet
import java.util.Locale
import java.util.Objects

@AllOpen
@NoArgConstructor
data class RestPersoon(
    var bsn: String? = null,
    var geslacht: String? = null,
    var geboortedatum: String? = null,
    var verblijfplaats: String? = null,
    override var emailadres: String? = null,
    override var naam: String? = null,
    override var telefoonnummer: String? = null,
    val indicaties: EnumSet<RestPersoonIndicaties> = EnumSet.noneOf(RestPersoonIndicaties::class.java),
) : RestKlant() {
    override fun getIdentificatieType(): IdentificatieType? {
        return if (bsn != null) IdentificatieType.BSN else null
    }

    override fun getIdentificatie(): String? {
        return bsn
    }
}

private const val OVERLIJDEN_OMSCHRIJVING = "overlijden"

fun List<Persoon>.toRestPersons(): List<RestPersoon> = this.map { it.toRestPersoon() }

fun List<PersoonBeperkt>.toRestPersonen(): List<RestPersoon> = this.map { it.toRestPerson() }

fun Persoon.toRestPersoon() = RestPersoon(
    bsn = this.burgerservicenummer,
    geslacht = this.geslacht?.toDescription(),
    geboortedatum = this.geboorte?.datum?.toStringRepresentation(),
    verblijfplaats = this.verblijfplaats?.toStringRepresentation(),
    naam = this.naam?.volledigeNaam,
).apply {
    if (inOnderzoek != null) {
        indicaties.add(RestPersoonIndicaties.IN_ONDERZOEK)
    }
    if (geheimhoudingPersoonsgegevens == true) {
        indicaties.add(RestPersoonIndicaties.GEHEIMHOUDING_OP_PERSOONSGEGEVENS)
    }
    // "Voor overleden personen wordt altijd het opschortingBijhouding veld geleverd met reden code ‘O’ en
    // omschrijving ‘overlijden’. Zie de overlijden overzicht feature voor meer informatie over dit veld."
    // https://brp-api.github.io/Haal-Centraal-BRP-bevragen/v2/features-overzicht
    if (opschortingBijhouding?.reden?.omschrijving == OVERLIJDEN_OMSCHRIJVING) {
        indicaties.add(RestPersoonIndicaties.OVERLEDEN)
    }
    if (opschortingBijhouding != null) {
        indicaties.add(RestPersoonIndicaties.OPSCHORTING_BIJHOUDING)
    }
    if (rni?.isNotEmpty() == true) {
        indicaties.add(RestPersoonIndicaties.NIET_INGEZETENE)
    }
    if (indicatieCurateleRegister == true) {
        indicaties.add(RestPersoonIndicaties.ONDER_CURATELE)
    }
}

fun PersoonBeperkt.toRestPerson() = RestPersoon(
    bsn = this.burgerservicenummer,
    geslacht = this.geslacht?.toDescription(),
    geboortedatum = this.geboorte?.datum?.toStringRepresentation(),
    naam = this.naam?.volledigeNaam,
    verblijfplaats = this.adressering?.let {
        StringUtil.joinNonBlankWith(
            ", ",
            it.adresregel1,
            it.adresregel2,
            it.adresregel3
        )
    },
).apply {
    if (inOnderzoek != null) {
        indicaties.add(RestPersoonIndicaties.IN_ONDERZOEK)
    }
    if (geheimhoudingPersoonsgegevens == true) {
        indicaties.add(RestPersoonIndicaties.GEHEIMHOUDING_OP_PERSOONSGEGEVENS)
    }
    // "Voor overleden personen wordt altijd het opschortingBijhouding veld geleverd met reden code ‘O’ en
    // omschrijving ‘overlijden’. Zie de overlijden overzicht feature voor meer informatie over dit veld."
    // https://brp-api.github.io/Haal-Centraal-BRP-bevragen/v2/features-overzicht
    if (opschortingBijhouding?.reden?.omschrijving == OVERLIJDEN_OMSCHRIJVING) {
        indicaties.add(RestPersoonIndicaties.OVERLEDEN)
    }
    if (opschortingBijhouding != null) {
        indicaties.add(RestPersoonIndicaties.OPSCHORTING_BIJHOUDING)
    }
    if (rni?.isNotEmpty() == true) {
        indicaties.add(RestPersoonIndicaties.NIET_INGEZETENE)
    }
}

fun List<DigitaalAdres>.toRestPersoon(): RestPersoon {
    val restPersoon = RestPersoon()
    for (digitalAdress in this) {
        when (digitalAdress.soortDigitaalAdres) {
            TELEFOON_SOORT_DIGITAAL_ADRES -> restPersoon.telefoonnummer = digitalAdress.adres
            EMAIL_SOORT_DIGITAAL_ADRES -> restPersoon.emailadres = digitalAdress.adres
        }
    }
    return restPersoon
}
fun PersonenQueryResponse.toRechtsPersonen(): List<RestPersoon> =
    when (this) {
        is RaadpleegMetBurgerservicenummerResponse -> this.personen.toRestPersons()
        is ZoekMetGeslachtsnaamEnGeboortedatumResponse -> this.personen.toRestPersonen()
        is ZoekMetNaamEnGemeenteVanInschrijvingResponse -> this.personen.toRestPersonen()
        is ZoekMetNummeraanduidingIdentificatieResponse -> this.personen.toRestPersonen()
        is ZoekMetPostcodeEnHuisnummerResponse -> this.personen.toRestPersonen()
        is ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse -> this.personen.toRestPersonen()
        else -> emptyList()
    }

fun List<RestPersoon>.toRestResultaat() = RESTResultaat(this)

private fun Waardetabel.toDescription(): String =
    if (StringUtils.isNotBlank(this.omschrijving)) this.omschrijving else this.code

private fun AbstractDatum.toStringRepresentation(): String? =
    when (this) {
        is VolledigeDatum -> this.datum.toString()
        is JaarMaandDatum -> String.format(
            Locale.getDefault(),
            "%d2-%d4",
            this.maand,
            this.jaar
        )

        is JaarDatum -> String.format(
            Locale.getDefault(),
            "%d4",
            this.jaar
        )

        is DatumOnbekend -> ONBEKEND
        else -> null
    }

private fun AbstractVerblijfplaats.toStringRepresentation(): String? =
    when (this) {
        is Adres -> this.verblijfadres?.toStringRepresentation()

        is VerblijfplaatsBuitenland -> this.verblijfadres?.toStringRepresentation()

        is VerblijfplaatsOnbekend -> ONBEKEND
        else -> null
    }

private fun VerblijfadresBinnenland.toStringRepresentation(): String {
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
    val postcode = StringUtils.replace(this.postcode, StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
    val woonplaats = StringUtils.replace(this.woonplaats, StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
    return StringUtil.joinNonBlankWith(", ", adres, postcode, woonplaats)
}

private fun VerblijfadresBuitenland.toStringRepresentation(): String {
    return StringUtil.joinNonBlankWith(
        ", ",
        this.regel1,
        this.regel2,
        this.regel3
    )
}
