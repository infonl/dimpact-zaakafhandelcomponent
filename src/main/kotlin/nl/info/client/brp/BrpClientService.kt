/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
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
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.configuratie.BrpConfiguration
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrNull

@ApplicationScoped
@AllOpen
@NoArgConstructor
class BrpClientService @Inject constructor(
    @RestClient val personenApi: PersonenApi,
    private val brpConfiguration: BrpConfiguration,
    private val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService
) {
    companion object {
        private val LOG = Logger.getLogger(BrpClientService::class.java.name)
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

    fun queryPersonen(personenQuery: PersonenQuery, zaaktypeUuid: UUID? = null, user: String? = null): PersonenQueryResponse =
        updateQuery(personenQuery).let { updatedQuery ->
            if (brpConfiguration.isBrpProtocolleringEnabled()) {
                personenApi.personen(
                    personenQuery = updatedQuery,
                    doelbinding = resolveDoelbinding(
                        zaaktypeUuid,
                        brpConfiguration.doelbindingZoekMetDefault.getOrNull()
                    ) {
                        it.zaaktypeBrpParameters?.zoekWaarde
                    },
                    verwerking = resoleVerwerkingregister(
                        zaaktypeUuid,
                        brpConfiguration.verwerkingregisterDefault.getOrNull()
                    ),
                    gebruikersnaam = user
                )
            } else {
                personenApi.personen(updatedQuery)
            }
        }

    /**
     * Retrieves a person by burgerservicenummer from the BRP Personen API.
     *
     * @param burgerservicenummer the burgerservicenummer of the person to retrieve
     * @param zaaktypeUuid the ID of the zaak the person is requested for, if any
     * @param userName the username making the request, if any
     * @return the person if found, otherwise null
     *
     */
    fun retrievePersoon(
        burgerservicenummer: String,
        zaaktypeUuid: UUID? = null,
        userName: String? = null
    ): Persoon? =
        createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer).let { personenQuery ->
            (
                if (brpConfiguration.isBrpProtocolleringEnabled()) {
                    personenApi.personen(
                        personenQuery = personenQuery,
                        doelbinding = resolveDoelbinding(
                            zaaktypeUuid,
                            brpConfiguration.doelbindingRaadpleegMetDefault.getOrNull()
                        ) {
                            it.zaaktypeBrpParameters?.raadpleegWaarde
                        },
                        verwerking = resoleVerwerkingregister(
                            zaaktypeUuid,
                            brpConfiguration.verwerkingregisterDefault.getOrNull()
                        ),
                        gebruikersnaam = userName
                    )
                } else {
                    personenApi.personen(personenQuery)
                } as RaadpleegMetBurgerservicenummerResponse
                ).personen?.firstOrNull()
        }

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

    private fun createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer: String) =
        RaadpleegMetBurgerservicenummer().apply {
            type = RAADPLEEG_MET_BURGERSERVICENUMMER
            fields = FIELDS_PERSOON
        }.addBurgerservicenummerItem(burgerservicenummer)

    private fun resolveDoelbinding(
        zaaktypeUuid: UUID?,
        defaultDoelbinding: String?,
        extractDoelbinding: (ZaaktypeCmmnConfiguration) -> String?
    ): String? =
        resolveBRPValue(
            zaaktypeUuid = zaaktypeUuid,
            defaultValue = defaultDoelbinding,
            valueDescription = "doelbinding",
            resolveFunction = extractDoelbinding,
            buildFunction = { resolvedValue, _ -> resolvedValue }
        )

    private fun resoleVerwerkingregister(
        zaaktypeUuid: UUID?,
        defaultVerwerkingregisterValue: String?
    ): String? =
        resolveBRPValue(
            zaaktypeUuid = zaaktypeUuid,
            defaultValue = defaultVerwerkingregisterValue,
            valueDescription = "verwerkingregister value",
            resolveFunction = { it.zaaktypeBrpParameters?.verwerkingregisterWaarde },
            buildFunction = { resolvedValue, zaakTypeConfiguration ->
                "${resolvedValue ?: defaultVerwerkingregisterValue}@${zaakTypeConfiguration.zaaktypeOmschrijving}"
            }
        )

    private fun resolveBRPValue(
        zaaktypeUuid: UUID?,
        defaultValue: String?,
        valueDescription: String,
        resolveFunction: (ZaaktypeCmmnConfiguration) -> String?,
        buildFunction: (resolvedValue: String?, ZaaktypeCmmnConfiguration) -> String?
    ): String? =
        zaaktypeUuid?.runCatching {
            LOG.fine("Resolving purpose for zaak with UUID: $this")
            resolveValueFromZaaktypeCmmnConfiguration(valueDescription, defaultValue, resolveFunction, buildFunction)
        }?.onFailure {
            LOG.log(Level.WARNING, "Failed to resolve $valueDescription for zaaktype $zaaktypeUuid", it)
        }?.getOrElse {
            LOG.info("Using default $valueDescription '$defaultValue' for zaaktype $zaaktypeUuid")
            null
        } ?: run {
            val reason = zaaktypeUuid?.let { "No $valueDescription found for zaaktype $zaaktypeUuid" }
                ?: "No zaak identification provided"
            LOG.info("$reason. Using default $valueDescription '$defaultValue'")
            defaultValue
        }

    /**
     * Resolves a value using the zaakafhandelparameters settings of the zaaktype
     *
     * @this zaakIdentificatie the UUID of the zaaktype
     * @param valueDescription a description of the value to resolve, for logging purposes
     * @param defaultValue the default value to use if no value can be resolved
     * @param resolveFunction the function to use to resolve the value
     * @param buildFunction the function to use to build the final value
     *
     * @return the resolved value, or null if only whitespace characters are present in the resolved value
     */
    private fun UUID.resolveValueFromZaaktypeCmmnConfiguration(
        valueDescription: String,
        defaultValue: String?,
        resolveFunction: (ZaaktypeCmmnConfiguration) -> String?,
        buildFunction: (String?, ZaaktypeCmmnConfiguration) -> String?
    ): String? =
        zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(this).let { zaaktypeCmmnConfiguration ->
            resolveFunction(zaaktypeCmmnConfiguration)?.let { resolvedValue ->
                if (resolvedValue.isPureAscii()) {
                    resolvedValue.trim().takeIf { it.isNotBlank() }
                } else {
                    LOG.warning {
                        "Resolved $valueDescription '$resolvedValue' contains non-ASCII characters. Using '$defaultValue' instead"
                    }
                    defaultValue
                }
            }.let { buildFunction(it, zaaktypeCmmnConfiguration) }
        }

    private fun String.isPureAscii(): Boolean = StandardCharsets.US_ASCII.newEncoder().canEncode(this)
}
