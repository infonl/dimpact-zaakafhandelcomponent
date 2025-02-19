/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.brp

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
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
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient

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
        private const val INDICATIE_CURATELE_REGISTER = "indicatieCurateleRegister"

        private val FIELDS_PERSOON = listOf(
            BURGERSERVICENUMMER,
            GESLACHT,
            NAAM,
            GEBOORTE,
            VERBLIJFPLAATS,
            INDICATIE_CURATELE_REGISTER
        )
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
            personenApi.personen(
                createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer)
            ) as RaadpleegMetBurgerservicenummerResponse
            )
            .personen?.firstOrNull()

    private fun createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer: String) =
        RaadpleegMetBurgerservicenummer().apply {
            type = RAADPLEEG_MET_BURGERSERVICENUMMER
            fields = FIELDS_PERSOON
        }.addBurgerservicenummerItem(burgerservicenummer)

    private fun updateQuery(personenQuery: PersonenQuery): PersonenQuery = personenQuery.apply {
        type = when (personenQuery) {
            is RaadpleegMetBurgerservicenummer -> RAADPLEEG_MET_BURGERSERVICENUMMER
            is ZoekMetGeslachtsnaamEnGeboortedatum -> ZOEK_MET_GESLACHTSNAAM_EN_GEBOORTEDATUM
            is ZoekMetNaamEnGemeenteVanInschrijving -> ZOEK_MET_NAAM_EN_GEMEENTE_VAN_INSCHRIJVING
            is ZoekMetNummeraanduidingIdentificatie -> ZOEK_MET_NUMMERAANDUIDING_IDENTIFICATIE
            is ZoekMetPostcodeEnHuisnummer -> ZOEK_MET_POSTCODE_EN_HUISNUMMER
            is ZoekMetStraatHuisnummerEnGemeenteVanInschrijving -> ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING
            else -> error("Must use one of the subclasses of '${PersonenQuery::class.java.simpleName}'")
        }
        fields = if (personenQuery is RaadpleegMetBurgerservicenummer) FIELDS_PERSOON else FIELDS_PERSOON_BEPERKT
    }
}
