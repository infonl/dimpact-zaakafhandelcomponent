/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.util.StringUtil
import nl.info.client.kvk.zoeken.model.generated.AdresType
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.klant.model.klant.RestKlant
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.Locale

@AllOpen
@NoArgConstructor
data class RestBedrijf(
    var vestigingsnummer: String? = null,
    var kvkNummer: String? = null,
    var rsin: String? = null,
    var adres: String? = null,
    var adresType: String? = null,
    var postcode: String? = null,
    var type: String? = null,
    override var emailadres: String? = null,
    override var naam: String? = null,
    override var telefoonnummer: String? = null
) : RestKlant() {
    /**
     * If a [vestigingsnummer] is present (with or without a (KVK nummer)[kvkNummer]), we return a vestiging type,
     * if we either have a (KVK nummer)[kvkNummer] or an (RSIN)[rsin] we return a rechtspersoon (RSIN) type.
     * If none of these are present (invalid bedrijf) we return null.
     */
    override fun getIdentificatieType(): IdentificatieType? =
        if (vestigingsnummer != null) {
            IdentificatieType.VN
        } else if (kvkNummer != null || rsin != null) {
            IdentificatieType.RSIN
        } else {
            // invalid bedrijf without any identification. should never happen but we currently lack proper validation
            null
        }
}

fun ResultaatItem.toRestBedrijf(): RestBedrijf {
    val adresType = this.adres?.binnenlandsAdres?.type ?: this.adres?.buitenlandsAdres?.type
    val isBezoekadres = adresType == AdresType.BEZOEKADRES
    return RestBedrijf(
        kvkNummer = this.kvkNummer,
        vestigingsnummer = this.vestigingsnummer,
        naam = this.toName(),
        postcode = this.adres?.binnenlandsAdres?.postcode,
        rsin = this.rsin,
        type = this.type.uppercase(Locale.getDefault()),
        adres = if (isBezoekadres) this.toAddress() else null,
        adresType = if (isBezoekadres) adresType.toString() else null
    )
}

fun List<RestBedrijf>.toRestResultaat() = RESTResultaat(this)

private fun ResultaatItem.toName(): String =
    this.naam.replace(" ", StringUtil.NON_BREAKING_SPACE)

private fun ResultaatItem.toAddress(): String? =
    this.adres?.binnenlandsAdres?.toKvkAdres()?.toFormattedAddress()
        ?: this.adres?.buitenlandsAdres?.toKvkAdres()?.toFormattedAddress()
