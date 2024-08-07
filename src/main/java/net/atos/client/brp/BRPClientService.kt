/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.brp;

import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.RAADPLEEG_MET_BURGERSERVICENUMMER
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_GESLACHTSNAAM_EN_GEBOORTEDATUM
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_NAAM_EN_GEMEENTE_VAN_INSCHRIJVING
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_POSTCODE_EN_HUISNUMMER
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.logging.Level

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.brp.model.generated.PersonenQuery;
import net.atos.client.brp.model.generated.PersonenQueryResponse;
import net.atos.client.brp.model.generated.Persoon;
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummer;
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse;
import net.atos.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatum;
import net.atos.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijving;
import net.atos.client.brp.model.generated.ZoekMetNummeraanduidingIdentificatie;
import net.atos.client.brp.model.generated.ZoekMetPostcodeEnHuisnummer;
import net.atos.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijving;
import net.atos.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_NUMMERAANDUIDING_IDENTIFICATIE

@ApplicationScoped
@AllOpen
@NoArgConstructor
 class BRPClientService @Inject constructor(
    @RestClient val personenApi: PersonenApi
 ) {
     companion object {
         private val  LOG = Logger.getLogger(BRPClientService::class.java.name)
         private val  BURGERSERVICENUMMER = "burgerservicenummer"
         private val  GESLACHT = "geslacht"
         private val  NAAM = "naam"
         private val GEBOORTE = "geboorte"
         private val VERBLIJFPLAATS = "verblijfplaats"
         private val ADRESSERING = "adressering"
         private val FIELDS_PERSOON = listOf(BURGERSERVICENUMMER, GESLACHT, NAAM, GEBOORTE, VERBLIJFPLAATS)
         private val FIELDS_PERSOON_BEPERKT = listOf(BURGERSERVICENUMMER, GESLACHT, NAAM, GEBOORTE, ADRESSERING)
     }

    fun queryPersonen(personenQuery: PersonenQuery): PersonenQueryResponse {
        complementQuery(personenQuery);
        return personenApi.personen(personenQuery);
    }

    /**
     * Vindt een persoon
     * <p>
     * Raadpleeg een (overleden) persoon.
     * Gebruik de fields parameter als je alleen specifieke velden in het antwoord wil zien,
     */
     fun  findPersoon( burgerservicenummer:String) :Optional<Persoon>{
        try {
             val response = personenApi.personen(
                    createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer)
            ) as RaadpleegMetBurgerservicenummerResponse;
            return if (!CollectionUtils.isEmpty(response.personen)) {
                Optional.of(response.personen.first());
            } else {
                Optional.empty();
            }
        } catch ( exception: RuntimeException) {
            LOG.log(Level.WARNING, "Error while calling findPersoon", exception);
            return Optional.empty();
        }
    }

    /**
     * Vindt een persoon asynchroon
     * <p>
     * Raadpleeg een (overleden) persoon.
     * Gebruik de fields parameter als je alleen specifieke velden in het antwoord wil zien,
     */
    fun findPersoonAsync(burgerservicenummer: String): CompletionStage<Optional<Persoon>> =
         personenApi.personenAsync(createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer))
                .handle { response, exception ->
                    handleFindPersoonAsync(response as RaadpleegMetBurgerservicenummerResponse, exception)
                }

    fun handleFindPersoonAsync(
        response: RaadpleegMetBurgerservicenummerResponse,
        exception: Throwable
    ): Optional<Persoon> {
        if (response.personen.isNotEmpty()) {
            return Optional.of(response.personen.first());
        } else {
            LOG.log(Level.WARNING, "Error while calling findPersoonAsync", exception);
            return Optional.empty();
        }
    }

    private fun createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer: String): RaadpleegMetBurgerservicenummer =
         RaadpleegMetBurgerservicenummer().let {
            it.type = RAADPLEEG_MET_BURGERSERVICENUMMER;
            it.fields = FIELDS_PERSOON;
            it.addBurgerservicenummerItem(burgerservicenummer);
            it
        }

    private fun complementQuery( personenQuery: PersonenQuery) {
        when (personenQuery) {
            is RaadpleegMetBurgerservicenummer -> {
                personenQuery.setType(RAADPLEEG_MET_BURGERSERVICENUMMER);
                personenQuery.setFields(FIELDS_PERSOON);
            }
            is ZoekMetGeslachtsnaamEnGeboortedatum -> {
                personenQuery.setType(ZOEK_MET_GESLACHTSNAAM_EN_GEBOORTEDATUM);
                personenQuery.setFields(FIELDS_PERSOON_BEPERKT);
            }
            is ZoekMetNaamEnGemeenteVanInschrijving  -> {
                personenQuery.setType(ZOEK_MET_NAAM_EN_GEMEENTE_VAN_INSCHRIJVING);
                personenQuery.setFields(FIELDS_PERSOON_BEPERKT);
            }
            is ZoekMetNummeraanduidingIdentificatie -> {
                personenQuery.setType(ZOEK_MET_NUMMERAANDUIDING_IDENTIFICATIE);
                personenQuery.setFields(FIELDS_PERSOON_BEPERKT);
            }
            is ZoekMetPostcodeEnHuisnummer  -> {
                personenQuery.setType(ZOEK_MET_POSTCODE_EN_HUISNUMMER);
                personenQuery.setFields(FIELDS_PERSOON_BEPERKT);
            }
            is ZoekMetStraatHuisnummerEnGemeenteVanInschrijving  -> {
                personenQuery.setType(ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING);
                personenQuery.setFields(FIELDS_PERSOON_BEPERKT);
            }
            else -> throw IllegalStateException(
                "Must use one of the subclasses of '${PersonenQuery::class.java.simpleName}'"
            )
        }
    }
}
