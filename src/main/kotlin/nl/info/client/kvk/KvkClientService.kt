/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.kvk.exception.KvkClientNoResultException
import nl.info.client.kvk.model.KvkSearchParameters
import nl.info.client.kvk.vestigingsprofiel.model.generated.Vestiging
import nl.info.client.kvk.zoeken.model.generated.Resultaat
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
class KvkClientService @Inject constructor(
    @RestClient private val kvkSearchClient: KvkSearchClient,
    @RestClient private val kvkVestigingsprofielClient: KvkVestigingsprofielClient
) {
    companion object {
        private val LOG = Logger.getLogger(KvkClientService::class.java.getName())
    }

    @Suppress("TooGenericExceptionCaught")
    fun search(kvkSearchParameters: KvkSearchParameters): Resultaat {
        try {
            return kvkSearchClient.getResults(kvkSearchParameters)
        } catch (_: KvkClientNoResultException) {
            // Nothing to report
        } catch (exception: RuntimeException) {
            LOG.log(Level.SEVERE, "Failed to search for company information using the KVK API", exception)
        }
        return Resultaat().apply {
            totaal = 0
            resultaten = listOf()
        }
    }

    fun findVestigingsprofiel(vestigingsnummer: String): Optional<Vestiging> =
        Optional.of<Vestiging>(kvkVestigingsprofielClient.getVestigingByVestigingsnummer(vestigingsnummer, false))

    fun findVestiging(vestigingsnummer: String): Optional<ResultaatItem> =
        convertToSingleItem(
            search(
                KvkSearchParameters().apply {
                    this.vestigingsnummer = vestigingsnummer
                }
            )
        )

    fun findRechtspersoon(rsin: String): Optional<ResultaatItem> =
        convertToSingleItem(
            search(
                KvkSearchParameters().apply {
                    this.type = "rechtspersoon"
                    this.rsin = rsin
                }
            )
        )

    private fun convertToSingleItem(resultaat: Resultaat): Optional<ResultaatItem> =
        when (resultaat.totaal) {
            0 -> Optional.empty<ResultaatItem>()
            1 -> Optional.of(resultaat.resultaten.first())
            else -> error("Too many results: ${resultaat.totaal}")
        }
}
