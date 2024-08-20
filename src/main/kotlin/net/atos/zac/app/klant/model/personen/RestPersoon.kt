/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.personen

import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.klant.model.klant.RestKlant
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestPersoon(
    var bsn: String? = null,
    var geslacht: String? = null,
    var geboortedatum: String? = null,
    var verblijfplaats: String? = null,
    override var emailadres: String? = null,
    override var naam: String? = null,
    override var telefoonnummer: String? = null
) : RestKlant() {
    override fun getIdentificatieType(): IdentificatieType? {
        return if (bsn != null) IdentificatieType.BSN else null
    }

    override fun getIdentificatie(): String? {
        return bsn
    }
}
