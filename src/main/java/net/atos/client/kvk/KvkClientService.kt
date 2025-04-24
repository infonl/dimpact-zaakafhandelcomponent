/*
 * SPDX-FileCopyrightText: 2022 Atos
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
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger

@ApplicationScoped
class KvkClientService {
    private var zoekenClient: ZoekenClient? = null
    private var vestigingsprofielClient: VestigingsprofielClient? = null

    @Inject
    constructor(
        @RestClient zoekenClient: ZoekenClient,
        @RestClient vestigingsprofielClient: VestigingsprofielClient
    ) {
        this.zoekenClient = zoekenClient
        this.vestigingsprofielClient = vestigingsprofielClient
    }

    /**
     * Default no-arg constructor, required by Weld.
     */
    constructor()

    fun list(parameters: KvkZoekenParameters?): Resultaat? {
        try {
            return zoekenClient!!.getResults(parameters)
        } catch (exception: KvkClientNoResultException) {
            // Nothing to report
        } catch (exception: RuntimeException) {
            LOG.log(Level.SEVERE, "Failed to search for company information using the KVK API", exception)
        }
        return createEmptyResultaat()
    }

    fun findVestigingsprofiel(vestigingsnummer: String?): Optional<Vestiging?> {
        return Optional.of<Vestiging?>(vestigingsprofielClient!!.getVestigingByVestigingsnummer(vestigingsnummer, false))
    }

    fun findVestiging(vestigingsnummer: String?): Optional<ResultaatItem?> {
        val zoekParameters = KvkZoekenParameters()
        zoekParameters.vestigingsnummer = vestigingsnummer
        return convertToSingleItem(list(zoekParameters)!!)
    }

    fun findRechtspersoon(rsin: String?): Optional<ResultaatItem?> {
        val zoekParameters = KvkZoekenParameters()
        zoekParameters.type = "rechtspersoon"
        zoekParameters.rsin = rsin
        return convertToSingleItem(list(zoekParameters)!!)
    }

    private fun convertToSingleItem(resultaat: Resultaat): Optional<ResultaatItem?> {
        return when (resultaat.getTotaal()) {
            0 -> Optional.empty<ResultaatItem?>()
            1 -> Optional.of<ResultaatItem?>(resultaat.getResultaten().getFirst())
            else -> throw IllegalStateException("Too many results: %d".formatted(resultaat.getTotaal()))
        }
    }

    private fun createEmptyResultaat(): Resultaat {
        val resultaat = Resultaat()
        resultaat.setTotaal(0)
        resultaat.setResultaten(mutableListOf<ResultaatItem?>())
        return resultaat
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(KvkClientService::class.java.getName())
    }
}
