/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.ZaakafhandelParameters
import nl.info.client.brp.model.generated.PersonenQuery
import nl.info.client.brp.model.generated.PersonenQueryResponse
import nl.info.client.brp.model.generated.Persoon
import nl.info.client.brp.model.generated.RaadpleegMetBurgerservicenummer
import nl.info.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse
import nl.info.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatum
import nl.info.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijving
import nl.info.client.brp.model.generated.ZoekMetNummeraanduidingIdentificatie
import nl.info.client.brp.model.generated.ZoekMetPostcodeEnHuisnummer
import nl.info.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijving
import nl.info.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.RAADPLEEG_MET_BURGERSERVICENUMMER
import nl.info.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_GESLACHTSNAAM_EN_GEBOORTEDATUM
import nl.info.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_NAAM_EN_GEMEENTE_VAN_INSCHRIJVING
import nl.info.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_NUMMERAANDUIDING_IDENTIFICATIE
import nl.info.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_POSTCODE_EN_HUISNUMMER
import nl.info.client.brp.util.PersonenQueryResponseJsonbDeserializer.Companion.ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
@AllOpen
@NoArgConstructor
class BrpClientService @Inject constructor(
    @RestClient val personenApi: PersonenApi,

    @ConfigProperty(name = ENV_VAR_BRP_DOELBINDING_ZOEKMET)
    private val queryPersonenDefaultPurpose: String?,

    @ConfigProperty(name = ENV_VAR_BRP_DOELBINDING_RAADPLEEGMET)
    private val retrievePersoonDefaultPurpose: String?,

    private val zrcClientService: ZrcClientService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService
) {
    companion object {
        private const val ENV_VAR_BRP_DOELBINDING_ZOEKMET = "brp.doelbinding.zoekmet"
        private const val ENV_VAR_BRP_DOELBINDING_RAADPLEEGMET = "brp.doelbinding.raadpleegmet"
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

    fun queryPersonen(personenQuery: PersonenQuery, auditEvent: String): PersonenQueryResponse =
        updateQuery(personenQuery).let { updatedQuery ->
            personenApi.personen(
                personenQuery = updatedQuery,
                purpose = resolvePurposeFromContext(
                    auditEvent,
                    queryPersonenDefaultPurpose
                ) { it.brpDoelbindingen?.zoekWaarde },
                auditEvent = auditEvent
            )
        }

    /**
     * Retrieves a person by burgerservicenummer from the BRP Personen API.
     *
     * @param burgerservicenummer the burgerservicenummer of the person to retrieve
     * @return the person if found, otherwise null
     *
     */
    fun retrievePersoon(burgerservicenummer: String, auditEvent: String): Persoon? = (
        personenApi.personen(
            personenQuery = createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer),
            purpose = resolvePurposeFromContext(
                auditEvent,
                retrievePersoonDefaultPurpose
            ) { it.brpDoelbindingen?.raadpleegWaarde },
            auditEvent = auditEvent
        ) as RaadpleegMetBurgerservicenummerResponse
        ).personen?.firstOrNull()

    private fun resolvePurposeFromContext(
        auditEvent: String,
        defaultPurpose: String?,
        extractPurpose: (ZaakafhandelParameters) -> String?
    ): String? {
        val match = Regex("""ZAAK-\d{4}-\d+""")
            .find(auditEvent)
        val zaakIdentificatie = if (match != null) {
            match.value
        } else {
            return defaultPurpose
        }

        return runCatching {
            val zaak = zrcClientService.readZaakByID(zaakIdentificatie)
            val parameters = zaakafhandelParameterService.readZaakafhandelParameters(
                zaak.zaaktype.extractUuid()
            )
            extractPurpose(parameters)
        }.getOrNull() ?: defaultPurpose
    }

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
            is ZoekMetStraatHuisnummerEnGemeenteVanInschrijving ->
                ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING
            else -> error("Must use one of the subclasses of '${PersonenQuery::class.java.simpleName}'")
        }
        fields = if (personenQuery is RaadpleegMetBurgerservicenummer) FIELDS_PERSOON else FIELDS_PERSOON_BEPERKT
    }
}
