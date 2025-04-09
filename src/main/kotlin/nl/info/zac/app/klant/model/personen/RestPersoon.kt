/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

@file:Suppress("TooManyFunctions")

package nl.info.zac.app.klant.model.personen

import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.util.StringUtil
import net.atos.zac.util.StringUtil.ONBEKEND
import nl.info.client.brp.model.generated.AbstractDatum
import nl.info.client.brp.model.generated.AbstractVerblijfplaats
import nl.info.client.brp.model.generated.Adres
import nl.info.client.brp.model.generated.DatumOnbekend
import nl.info.client.brp.model.generated.JaarDatum
import nl.info.client.brp.model.generated.JaarMaandDatum
import nl.info.client.brp.model.generated.PersonenQueryResponse
import nl.info.client.brp.model.generated.Persoon
import nl.info.client.brp.model.generated.PersoonBeperkt
import nl.info.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse
import nl.info.client.brp.model.generated.VerblijfadresBinnenland
import nl.info.client.brp.model.generated.VerblijfadresBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsOnbekend
import nl.info.client.brp.model.generated.VolledigeDatum
import nl.info.client.brp.model.generated.Waardetabel
import nl.info.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatumResponse
import nl.info.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijvingResponse
import nl.info.client.brp.model.generated.ZoekMetNummeraanduidingIdentificatieResponse
import nl.info.client.brp.model.generated.ZoekMetPostcodeEnHuisnummerResponse
import nl.info.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse
import nl.info.client.klanten.model.generated.DigitaalAdres
import nl.info.client.klanten.model.generated.SoortDigitaalAdresEnum
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.klant.model.klant.RestKlant
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
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
    override var naam: String? = null,
    override var emailadres: String? = null,
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

private const val DECEASED_CODE = "O"
private const val MINISTRIAL_REGULATION_CODE = "M"
private const val EMIGRATION_CODE = "E"

fun List<Persoon>.toRestPersons(): List<RestPersoon> = this.map { it.toRestPersoon() }

fun List<PersoonBeperkt>.toRestPersonen(): List<RestPersoon> = this.map { it.toRestPersoon() }

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
    when (opschortingBijhouding?.reden?.code) {
        // "Voor overleden personen wordt altijd het opschortingBijhouding veld geleverd met reden code ‘O’ en
        // omschrijving ‘overlijden’. Zie de overlijden overzicht feature voor meer informatie over dit veld."
        // https://brp-api.github.io/Haal-Centraal-BRP-bevragen/v2/features-overzicht
        DECEASED_CODE -> indicaties.add(RestPersoonIndicaties.OVERLEDEN)
        MINISTRIAL_REGULATION_CODE -> indicaties.add(RestPersoonIndicaties.MINISTERIELE_REGELING)
        EMIGRATION_CODE -> indicaties.add(RestPersoonIndicaties.EMIGRATIE)
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
    if (adressering?.indicatieVastgesteldVerblijftNietOpAdres == true) {
        indicaties.add(RestPersoonIndicaties.BLOKKERING_VANWEGE_VERHUIZING)
    }
}

fun PersoonBeperkt.toRestPersoon() = RestPersoon(
    bsn = this.burgerservicenummer,
    geslacht = this.geslacht?.toDescription(),
    geboortedatum = this.geboorte?.datum?.toStringRepresentation(),
    naam = this.naam?.volledigeNaam,
    verblijfplaats = this.adressering?.let {
        listOfNotNull(
            it.adresregel1,
            it.adresregel2,
            it.adresregel3,
            it.land?.omschrijving
        )
            .joinToString()
            .replace(StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
    },
).apply {
    if (inOnderzoek != null) {
        indicaties.add(RestPersoonIndicaties.IN_ONDERZOEK)
    }
    if (geheimhoudingPersoonsgegevens == true) {
        indicaties.add(RestPersoonIndicaties.GEHEIMHOUDING_OP_PERSOONSGEGEVENS)
    }
    when (opschortingBijhouding?.reden?.code) {
        // "Voor overleden personen wordt altijd het opschortingBijhouding veld geleverd met reden code ‘O’ en
        // omschrijving ‘overlijden’. Zie de overlijden overzicht feature voor meer informatie over dit veld."
        // https://brp-api.github.io/Haal-Centraal-BRP-bevragen/v2/features-overzicht
        DECEASED_CODE -> indicaties.add(RestPersoonIndicaties.OVERLEDEN)
        MINISTRIAL_REGULATION_CODE -> indicaties.add(RestPersoonIndicaties.MINISTERIELE_REGELING)
        EMIGRATION_CODE -> indicaties.add(RestPersoonIndicaties.EMIGRATIE)
    }
    if (opschortingBijhouding != null) {
        indicaties.add(RestPersoonIndicaties.OPSCHORTING_BIJHOUDING)
    }
    if (rni?.isNotEmpty() == true) {
        indicaties.add(RestPersoonIndicaties.NIET_INGEZETENE)
    }
    if (adressering?.indicatieVastgesteldVerblijftNietOpAdres == true) {
        indicaties.add(RestPersoonIndicaties.BLOKKERING_VANWEGE_VERHUIZING)
    }
}

fun List<DigitaalAdres>.toRestPersoon(): RestPersoon {
    val restPersoon = RestPersoon()
    for (digitalAdress in this) {
        when (digitalAdress.soortDigitaalAdres) {
            SoortDigitaalAdresEnum.TELEFOONNUMMER -> restPersoon.telefoonnummer = digitalAdress.adres
            SoortDigitaalAdresEnum.EMAIL -> restPersoon.emailadres = digitalAdress.adres
            SoortDigitaalAdresEnum.OVERIG -> null // not supported in ZAC
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

private fun VerblijfadresBinnenland.toStringRepresentation() =
    listOfNotNull(
        this.officieleStraatnaam,
        Objects.toString(this.huisnummer, null),
        this.huisnummertoevoeging,
        this.huisletter,
    ).joinToString(StringUtil.NON_BREAKING_SPACE).let { address ->
        listOfNotNull(address, this.postcode, this.woonplaats)
            .joinToString()
            .replace(StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
    }

private fun VerblijfadresBuitenland.toStringRepresentation() =
    listOfNotNull(
        this.regel1,
        this.regel2,
        this.regel3,
        land?.omschrijving
    )
        .joinToString()
        .replace(StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
