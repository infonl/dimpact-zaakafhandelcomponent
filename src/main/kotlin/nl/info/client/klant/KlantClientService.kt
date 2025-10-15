/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.klant.model.CodeObjecttypeEnum
import nl.info.client.klant.model.DigitaalAdres
import nl.info.client.klant.model.ExpandBetrokkene
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
@AllOpen
@NoArgConstructor
class KlantClientService @Inject constructor(
    @RestClient
    private val klantClient: KlantClient
) {
    fun findDigitalAddresses(objectType: CodeObjecttypeEnum, number: String): List<DigitaalAdres> =
        klantClient.partijenList(
            expand = "digitaleAdressen",
            page = 1,
            pageSize = 1,
            partijIdentificatorCodeObjecttype = objectType.toString(),
            partijIdentificatorObjectId = number
        ).getResults().firstOrNull()?.getExpand()?.getDigitaleAdressen() ?: emptyList()

    fun listBetrokkenen(number: String, page: Int): List<ExpandBetrokkene> =
        klantClient.partijenList(
            expand = "betrokkenen,betrokkenen.hadKlantcontact",
            page = page,
            pageSize = 1,
            partijIdentificatorObjectId = number
        ).getResults().firstOrNull()?.getExpand()?.betrokkenen ?: emptyList()
}
