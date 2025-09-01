/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.util.StringUtil
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.klant.model.klant.RestKlant
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.Locale
import java.util.Objects

@AllOpen
@NoArgConstructor
data class RestBedrijf(
    var vestigingsnummer: String? = null,
    var kvkNummer: String? = null,
    var rsin: String? = null,
    var adres: String? = null,
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

    /**
     * Remove this function? It seems obsolete because we already return all the individual fields.
     * Check with frontend.
     */
    override fun getIdentificatie(): String? =
        if (vestigingsnummer != null) vestigingsnummer else if (rsin != null) rsin else kvkNummer
}

fun ResultaatItem.toRestBedrijf() = RestBedrijf(
    kvkNummer = this.kvkNummer,
    vestigingsnummer = this.vestigingsnummer,
    naam = this.toName(),
    postcode = this.adres.binnenlandsAdres.postcode,
    rsin = this.rsin,
    type = this.type.uppercase(Locale.getDefault()),
    adres = this.toAddress()
)

fun List<RestBedrijf>.toRestResultaat() = RESTResultaat(this)

private fun ResultaatItem.toName(): String =
    this.naam.replace(" ", StringUtil.NON_BREAKING_SPACE)

private fun ResultaatItem.toAddress(): String =
    this.adres.binnenlandsAdres.let { binnenlandsAdres ->
        StringUtil.joinNonBlankWith(
            StringUtil.NON_BREAKING_SPACE,
            binnenlandsAdres.straatnaam,
            Objects.toString(binnenlandsAdres.huisnummer, null),
            binnenlandsAdres.huisletter
        ).replace(" ", StringUtil.NON_BREAKING_SPACE).let { adres ->
            StringUtil.joinNonBlankWith(
                ", ",
                adres,
                binnenlandsAdres.postcode?.replace(" ", StringUtil.NON_BREAKING_SPACE),
                binnenlandsAdres.plaats?.replace(" ", StringUtil.NON_BREAKING_SPACE)
            )
        }
    }
