/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.kvk

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.kvk.exception.KvkClientNoResultException
import net.atos.client.kvk.model.KvkZoekenParameters
import nl.info.client.kvk.vestigingsprofiel.model.generated.Vestiging
import nl.info.client.kvk.zoeken.model.generated.Resultaat
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
class KvkClientService @Inject constructor(
    @RestClient private val zoekenClient: ZoekenClient,
    @RestClient private val vestigingsprofielClient: VestigingsprofielClient
) {
    companion object {
        private val LOG = Logger.getLogger(KvkClientService::class.java.getName())
    }

    @Suppress("TooGenericExceptionCaught")
    fun list(parameters: KvkZoekenParameters): Resultaat {
        try {
            return zoekenClient.getResults(parameters)
        } catch (_: KvkClientNoResultException) {
            // Nothing to report
        } catch (exception: RuntimeException) {
            LOG.log(Level.SEVERE, "Failed to search for company information using the KVK API", exception)
        }
        return createEmptyResultaat()
    }

    fun findVestigingsprofiel(vestigingsnummer: String): Optional<Vestiging> =
        Optional.of<Vestiging>(vestigingsprofielClient.getVestigingByVestigingsnummer(vestigingsnummer, false))

    fun findVestiging(vestigingsnummer: String): Optional<ResultaatItem> {
        val zoekParameters = KvkZoekenParameters()
        zoekParameters.vestigingsnummer = vestigingsnummer
        return convertToSingleItem(list(zoekParameters))
    }

    fun findRechtspersoon(rsin: String): Optional<ResultaatItem> {
        val zoekParameters = KvkZoekenParameters()
        zoekParameters.type = "rechtspersoon"
        zoekParameters.rsin = rsin
        return convertToSingleItem(list(zoekParameters))
    }

    private fun convertToSingleItem(resultaat: Resultaat): Optional<ResultaatItem> =
        when (resultaat.totaal) {
            0 -> Optional.empty<ResultaatItem>()
            1 -> Optional.of(resultaat.resultaten.first())
            else -> error("Too many results: ${resultaat.totaal}")
        }

    private fun createEmptyResultaat(): Resultaat {
        val resultaat = Resultaat()
        resultaat.totaal = 0
        resultaat.resultaten = listOf<ResultaatItem>()
        return resultaat
    }
}
