/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven

import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.klant.model.klant.RestKlant

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
