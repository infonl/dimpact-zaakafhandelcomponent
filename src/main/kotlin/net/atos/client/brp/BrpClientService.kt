/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.brp

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.brp.exception.BrpRuntimeException
import net.atos.client.brp.model.generated.PersonenQuery
import net.atos.client.brp.model.generated.PersonenQueryResponse
import net.atos.client.brp.model.generated.Persoon
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummer
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse
import net.atos.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatum
import net.atos.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijving
import net.atos.client.brp.model.generated.ZoekMetNummeraanduidingIdentificatie
import net.atos.client.brp.model.generated.ZoekMetPostcodeEnHuisnummer
import net.atos.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijving
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.RAADPLEEG_MET_BURGERSERVICENUMMER
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_GESLACHTSNAAM_EN_GEBOORTEDATUM
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_NAAM_EN_GEMEENTE_VAN_INSCHRIJVING
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_NUMMERAANDUIDING_IDENTIFICATIE
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_POSTCODE_EN_HUISNUMMER
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.concurrent.CompletionStage
import java.util.logging.Logger

@ApplicationScoped
@AllOpen
@NoArgConstructor
class BrpClientService @Inject constructor(
    @RestClient val personenApi: PersonenApi
) {
    companion object {
        private const val BURGERSERVICENUMMER = "burgerservicenummer"
        private const val GESLACHT = "geslacht"
        private const val NAAM = "naam"
        private const val GEBOORTE = "geboorte"
        private const val VERBLIJFPLAATS = "verblijfplaats"
        private const val ADRESSERING = "adressering"

        private val LOG = Logger.getLogger(BrpClientService::class.java.name)
        private val FIELDS_PERSOON = listOf(BURGERSERVICENUMMER, GESLACHT, NAAM, GEBOORTE, VERBLIJFPLAATS)
        private val FIELDS_PERSOON_BEPERKT = listOf(BURGERSERVICENUMMER, GESLACHT, NAAM, GEBOORTE, ADRESSERING)
    }

    fun queryPersonen(personenQuery: PersonenQuery): PersonenQueryResponse =
        updateQuery(personenQuery).let {
            personenApi.personen(it)
        }

    /**
     * Retrieves a person by burgerservicenummer from the BRP Personen API.
     *
     * @param burgerservicenummer the burgerservicenummer of the person to retrieve
     * @return the person if found, otherwise null
     */
    fun retrievePersoon(burgerservicenummer: String): Persoon? =
        (
            personenApi.personen(personenQuery = createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer))
                as RaadpleegMetBurgerservicenummerResponse
            ).personen?.let { persons ->
            if (persons.isNotEmpty()) {
                if (persons.size > 1) {
                    LOG.warning(
                        "Multiple persons found for burgerservicenummer: '$burgerservicenummer'. " +
                            "Returning the first one."
                    )
                }
                persons.first()
            } else {
                LOG.info("No person found for burgerservicenummer: $burgerservicenummer")
                null
            }
        }

    /**
     * Retrieves a person by burgerservicenummer from the BRP Personen API asynchronously.
     *
     * @param burgerservicenummer the burgerservicenummer of the person to retrieve
     * @return a CompletionStage with the person if found, otherwise null
     */
    fun retrievePersoonAsync(burgerservicenummer: String): CompletionStage<Persoon?> =
        createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer).let {
            personenApi.personenAsync(it)
                .handle { response, exception ->
                    (response as RaadpleegMetBurgerservicenummerResponse?)?.personen?.let { persons ->
                        if (persons.isNotEmpty()) {
                            if (persons.size > 1) {
                                LOG.warning(
                                    "Multiple persons found for burgerservicenummer: '$burgerservicenummer'. " +
                                        "Returning the first one."
                                )
                            }
                            return@handle persons.first()
                        } else {
                            LOG.info("No person found for burgerservicenummer: $burgerservicenummer")
                            null
                        }
                    }
                    exception?.let { e ->
                        throw BrpRuntimeException("Error occured while finding person in the BRP API", e)
                    }
                }
        }

    private fun createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer: String) =
        RaadpleegMetBurgerservicenummer().apply {
            type = RAADPLEEG_MET_BURGERSERVICENUMMER
            fields = FIELDS_PERSOON
        }.addBurgerservicenummerItem(burgerservicenummer)

    private fun updateQuery(personenQuery: PersonenQuery): PersonenQuery =
        personenQuery.apply {
            when (personenQuery) {
                is RaadpleegMetBurgerservicenummer -> {
                    type = RAADPLEEG_MET_BURGERSERVICENUMMER
                    fields = FIELDS_PERSOON
                }

                is ZoekMetGeslachtsnaamEnGeboortedatum -> {
                    type = ZOEK_MET_GESLACHTSNAAM_EN_GEBOORTEDATUM
                    fields = FIELDS_PERSOON_BEPERKT
                }

                is ZoekMetNaamEnGemeenteVanInschrijving -> {
                    type = ZOEK_MET_NAAM_EN_GEMEENTE_VAN_INSCHRIJVING
                    fields = FIELDS_PERSOON_BEPERKT
                }

                is ZoekMetNummeraanduidingIdentificatie -> {
                    type = ZOEK_MET_NUMMERAANDUIDING_IDENTIFICATIE
                    fields = FIELDS_PERSOON_BEPERKT
                }

                is ZoekMetPostcodeEnHuisnummer -> {
                    type = ZOEK_MET_POSTCODE_EN_HUISNUMMER
                    fields = FIELDS_PERSOON_BEPERKT
                }

                is ZoekMetStraatHuisnummerEnGemeenteVanInschrijving -> {
                    type = ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING
                    fields = FIELDS_PERSOON_BEPERKT
                }

                else -> error(
                    "Must use one of the subclasses of '${PersonenQuery::class.java.simpleName}'"
                )
            }
        }
}
