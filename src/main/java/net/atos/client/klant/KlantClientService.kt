/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klant

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.klant.model.DigitaalAdres
import net.atos.client.klant.model.ExpandBetrokkene
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
    fun findDigitalAddressesByNumber(number: String): List<DigitaalAdres> {
        val party = convertToSingleItem(
            klantClient.partijenList(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "digitaleAdressen",
                null,
                null,
                null,
                1,
                1,
                null,
                null,
                null,
                number,
                null,
                null,
                null
            ).getResults()
        )
        if (party == null || party.getExpand() == null) {
            return emptyList()
        }
        return party.getExpand().getDigitaleAdressen()
    }

    fun listBetrokkenenByNumber(number: String, page: Int): List<ExpandBetrokkene> {
        val party = convertToSingleItem(
            klantClient.partijenList(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "betrokkenen,betrokkenen.hadKlantcontact",
                null,
                null,
                null,
                page,
                1,
                null,
                null,
                null,
                number,
                null,
                null,
                null
            ).getResults()
        )
        if (party == null || party.getExpand() == null) {
            return emptyList()
        }
        return party.getExpand().getBetrokkenen()
    }

    private fun <T> convertToSingleItem(list: List<T>): T? {
        return when (list.size) {
            0 -> null
            1 -> list.first()
            else -> error("Too many results: ${list.size}")
        }
    }
}
