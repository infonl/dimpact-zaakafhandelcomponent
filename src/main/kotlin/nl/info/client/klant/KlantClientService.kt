/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.klant.model.CodeObjecttypeEnum
import nl.info.client.klant.model.CodeSoortObjectIdEnum
import nl.info.client.klant.model.DigitaalAdres
import nl.info.client.klant.model.ExpandBetrokkene
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
    }

    fun findDigitalAddressesForVestiging(
        vestigingsnummer: String,
        kvkNummer: String? = null
    ): List<DigitaalAdres> {
        val expandPartij = klantClient.partijenList(
            expand = "digitaleAdressen",
            page = 1,
            pageSize = 1,
            partijIdentificatorCodeObjecttype = CodeObjecttypeEnum.VESTIGING.toString(),
            partijIdentificatorCodeSoortObjectId = CodeSoortObjectIdEnum.VESTIGINGSNUMMER.toString(),
            partijIdentificatorObjectId = vestigingsnummer
        ).getResults().firstOrNull()
        // if a KVK number was provided for the vestiging, check if the related 'sub-identificator-van' partij has an identificator
        // that matches the requested KVK number
        if (kvkNummer != null && expandPartij != null) {
            val subIdentificatorVan = expandPartij.partijIdentificatoren?.firstOrNull()?.subIdentificatorVan?.uuid
                ?.let { klantClient.getPartijIdentificator(it) }
            if (subIdentificatorVan?.partijIdentificator?.let {
                    it.codeObjecttype != CodeObjecttypeEnum.NIET_NATUURLIJK_PERSOON.toString() ||
                        it.codeSoortObjectId != CodeSoortObjectIdEnum.KVK_NUMMER.toString() ||
                        it.objectId != kvkNummer
                } == true
            ) {
                // In future, we may want to return a message to the frontend in this case, so that we can show a
                // warning indication to the end-user. For now, we just log it and return an empty list.
                LOG.info {
                    "The related partij (through 'sub-identificator-van') of the vestiging partij " +
                        "with vestigingsnummer '$vestigingsnummer' does not have the required " +
                        "partij identificator with KVK number: '$kvkNummer'"
                }
                return emptyList()
            }
        }
        return expandPartij?.getExpand()?.getDigitaleAdressen() ?: emptyList()
    }

    fun findDigitalAddressesForNonNaturalPerson(kvkNummer: String): List<DigitaalAdres> =
        klantClient.partijenList(
            expand = "digitaleAdressen",
            page = 1,
            pageSize = 1,
            partijIdentificatorCodeObjecttype = CodeObjecttypeEnum.NIET_NATUURLIJK_PERSOON.toString(),
            partijIdentificatorCodeSoortObjectId = CodeSoortObjectIdEnum.KVK_NUMMER.toString(),
            partijIdentificatorObjectId = kvkNummer
        ).getResults().firstOrNull()?.getExpand()?.getDigitaleAdressen() ?: emptyList()

    fun findDigitalAddressesForNaturalPerson(number: String): List<DigitaalAdres> =
        klantClient.partijenList(
            expand = "digitaleAdressen",
            page = 1,
            pageSize = 1,
            partijIdentificatorCodeObjecttype = CodeObjecttypeEnum.NATUURLIJK_PERSOON.toString(),
            partijIdentificatorCodeSoortObjectId = CodeSoortObjectIdEnum.BSN.toString(),
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
