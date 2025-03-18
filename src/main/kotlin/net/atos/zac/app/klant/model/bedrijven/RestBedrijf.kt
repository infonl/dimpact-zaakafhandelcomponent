/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven

import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.klant.model.klant.RestKlant
import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.util.StringUtil
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
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
    override fun getIdentificatieType(): IdentificatieType? {
        return if (vestigingsnummer != null) IdentificatieType.VN else (if (rsin != null) IdentificatieType.RSIN else null)
    }

    override fun getIdentificatie(): String? {
        return if (vestigingsnummer != null) vestigingsnummer else rsin
    }
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
