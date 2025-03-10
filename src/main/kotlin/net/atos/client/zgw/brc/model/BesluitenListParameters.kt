/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.brc.model

import jakarta.ws.rs.QueryParam
import net.atos.client.zgw.shared.model.AbstractListParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI

@NoArgConstructor
@AllOpen
class BesluitenListParameters : AbstractListParameters() {
    /**
     * Identificatie van het besluit binnen de organisatie die het besluit heeft vastgesteld. Indien deze niet opgegeven is, dan wordt die
     * gegenereerd.
     */
    @QueryParam("identificatie")
    var identificatie: String? = null

    /**
     * Het RSIN van de niet-natuurlijk persoon zijnde de organisatie die het besluit heeft vastgesteld.
     */
    @QueryParam("verantwoordelijkeOrganisatie")
    var verantwoordelijkeOrganisatie: String? = null

    /**
     * URL-referentie naar het BESLUITTYPE (in de Catalogi API).
     */
    @QueryParam("besluittype")
    var besluittypeUri: URI? = null

    /**
     * URL-referentie naar de ZAAK (in de Zaken API) waarvan dit besluit uitkomst is.
     */
    @QueryParam("zaak")
    var zaakUri: URI? = null
}
