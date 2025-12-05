/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.klanten.model.generated.CodeObjecttypeEnum.NATUURLIJK_PERSOON
import nl.info.client.klanten.model.generated.CodeObjecttypeEnum.NIET_NATUURLIJK_PERSOON
import nl.info.client.klanten.model.generated.CodeObjecttypeEnum.VESTIGING
import nl.info.client.klanten.model.generated.CodeSoortObjectIdEnum.BSN
import nl.info.client.klanten.model.generated.CodeSoortObjectIdEnum.KVK_NUMMER
import nl.info.client.klanten.model.generated.CodeSoortObjectIdEnum.VESTIGINGSNUMMER
import nl.info.client.klanten.model.generated.DigitaalAdres
import nl.info.client.klanten.model.generated.ExpandBetrokkene
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.logging.Logger

@ApplicationScoped
@AllOpen
@NoArgConstructor
class KlantClientService @Inject constructor(
    @RestClient
    private val klantClient: KlantClient
) {
    companion object {
        private val LOG = Logger.getLogger(KlantClientService::class.java.name)
        private const val DEFAULT_PAGE_SIZE = 100
    }

    /**
     * Finds digital addresses (i.e. 'contact details') in the Klantinteracties API for a 'vestiging'
     * identified by the given [vestigingsnummer] and [kvkNummer].
     * Note that there may be multiple 'partijen' in the Klantinteracties API for this vestigingsnummer,
     * each possibly linked to parent partijen with different KVK numbers.
     * This method filters the results to return digital addresses only for the 'partij' whose
     * related parent partij (via 'sub-identificator-van') has an identificator matching the provided KVK number.
     */
    fun findDigitalAddressesForVestiging(
        vestigingsnummer: String,
        kvkNummer: String
    ): List<DigitaalAdres> {
        val expandPartijen = klantClient.partijenList(
            expand = "digitaleAdressen",
            page = 1,
            pageSize = DEFAULT_PAGE_SIZE,
            partijIdentificatorCodeObjecttype = VESTIGING.toString(),
            partijIdentificatorCodeSoortObjectId = VESTIGINGSNUMMER.toString(),
            partijIdentificatorObjectId = vestigingsnummer
        ).getResults() ?: return emptyList()
        val expandPartijWithCorrectKvkNumber = expandPartijen.firstOrNull { expandPartij ->
            // check if the related 'sub-identificator-van' partij (i.e. the parent company of the vestiging)
            // has an identificator that matches the provided KVK number
            val subIdentificatorVan = expandPartij.partijIdentificatoren
                ?.firstOrNull()
                ?.subIdentificatorVan
                ?.uuid
                ?.let { klantClient.getPartijIdentificator(it) }
            subIdentificatorVan?.partijIdentificator?.let {
                it.codeObjecttype == NIET_NATUURLIJK_PERSOON &&
                    it.codeSoortObjectId == KVK_NUMMER &&
                    it.objectId == kvkNummer
            } == true
        } ?: run {
            LOG.info {
                "The related partij (through 'sub-identificator-van') of the vestiging partij " +
                    "with vestigingsnummer '$vestigingsnummer' does not have the required " +
                    "partij identificator with KVK number: '$kvkNummer'. This can occur for example " +
                    "when a vestiging was previously linked to another KVK number."
            }
            null
        }
        return expandPartijWithCorrectKvkNumber?.getExpand()?.getDigitaleAdressen() ?: emptyList()
    }

    fun findDigitalAddressesForNonNaturalPerson(kvkNummer: String): List<DigitaalAdres> =
        klantClient.partijenList(
            expand = "digitaleAdressen",
            page = 1,
            pageSize = DEFAULT_PAGE_SIZE,
            partijIdentificatorCodeObjecttype = NIET_NATUURLIJK_PERSOON.toString(),
            partijIdentificatorCodeSoortObjectId = KVK_NUMMER.toString(),
            partijIdentificatorObjectId = kvkNummer
        ).getResults().firstOrNull()?.getExpand()?.getDigitaleAdressen() ?: emptyList()

    fun findDigitalAddressesForNaturalPerson(number: String): List<DigitaalAdres> =
        klantClient.partijenList(
            expand = "digitaleAdressen",
            page = 1,
            pageSize = DEFAULT_PAGE_SIZE,
            partijIdentificatorCodeObjecttype = NATUURLIJK_PERSOON.toString(),
            partijIdentificatorCodeSoortObjectId = BSN.toString(),
            partijIdentificatorObjectId = number
        ).getResults().firstOrNull()?.getExpand()?.getDigitaleAdressen() ?: emptyList()

    fun listExpandBetrokkenen(number: String, page: Int): List<ExpandBetrokkene> =
        klantClient.partijenList(
            expand = "betrokkenen,betrokkenen.hadKlantcontact",
            page = page,
            pageSize = DEFAULT_PAGE_SIZE,
            partijIdentificatorObjectId = number
        ).getResults().firstOrNull()?.getExpand()?.betrokkenen ?: emptyList()
}
