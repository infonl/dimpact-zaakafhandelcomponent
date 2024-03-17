/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import net.atos.client.util.JAXRSClientFactory
import net.atos.client.zgw.shared.cache.Caching
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import net.atos.client.zgw.ztc.model.BesluittypeListParameters
import net.atos.client.zgw.ztc.model.CatalogusListParameters
import net.atos.client.zgw.ztc.model.ResultaattypeListParameters
import net.atos.client.zgw.ztc.model.RoltypeListParameters
import net.atos.client.zgw.ztc.model.StatustypeListParameters
import net.atos.client.zgw.ztc.model.ZaaktypeInformatieobjecttypeListParameters
import net.atos.client.zgw.ztc.model.ZaaktypeListParameters
import net.atos.client.zgw.ztc.model.generated.BesluitType
import net.atos.client.zgw.ztc.model.generated.Catalogus
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.client.zgw.ztc.model.generated.ResultaatType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.client.zgw.ztc.model.generated.RolType.OmschrijvingGeneriekEnum
import net.atos.client.zgw.ztc.model.generated.StatusType
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.client.zgw.ztc.model.generated.ZaakTypeInformatieObjectType
import net.atos.zac.configuratie.ConfiguratieService
import nl.info.zac.util.AllOpen
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.net.URI
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID
import javax.cache.annotation.CacheRemoveAll
import javax.cache.annotation.CacheResult

/**
 * Encapsulates {@link ZTCClient} by providing caching and authentication.
 */
@ApplicationScoped
@AllOpen
@Suppress("TooManyFunctions")
class ZTCClientService : Caching {
    @RestClient
    @Inject
    private lateinit var ztcClient: ZTCClient

    @Inject
    private lateinit var zgwClientHeadersFactory: ZGWClientHeadersFactory

    @Inject
    private lateinit var configuratieService: ConfiguratieService

    fun listCatalogus(catalogusListParameters: CatalogusListParameters): Results<Catalogus> =
        ztcClient.catalogusList(catalogusListParameters)

    /**
     * Read [Catalogus] filtered by [CatalogusListParameters].
     * Throws a RuntimeException if the [Catalogus] can not be read.
     *
     * @param filter [CatalogusListParameters].
     * @return [Catalogus]. Never 'null'!
     */
    fun readCatalogus(filter: CatalogusListParameters): Catalogus =
        ztcClient.catalogusList(filter)
            .singleResult
            .orElseThrow { RuntimeException("Catalogus not found.") }

    @CacheResult(cacheName = Caching.ZTC_CACHE_TIME)
    fun readCacheTime() = ZonedDateTime.now()

    /**
     * Read [ZaakType] via URI.
     * Throws a RuntimeException if the [ZaakType] can not be read.
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return [ZaakType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_ZAAKTYPE)
    fun readZaaktype(zaaktypeURI: URI): ZaakType =
        createInvocationBuilder(zaaktypeURI).get(ZaakType::class.java)

    /**
     * Read [ZaakType] via UUID.
     * Throws a RuntimeException if the [ZaakType] can not be read.
     *
     * @param zaaktypeUuid UUID of [ZaakType].
     * @return [ZaakType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_ZAAKTYPE)
    fun readZaaktype(zaaktypeUuid: UUID): ZaakType =
        ztcClient.zaaktypeRead(zaaktypeUuid)

    /**
     * List instances of [ZaakType] in [Catalogus].
     *
     * @param catalogusURI URI of [Catalogus].
     * @return List of [ZaakType] instances
     */
    @CacheResult(cacheName = Caching.ZTC_ZAAKTYPE)
    fun listZaaktypen(catalogusURI: URI): List<ZaakType> =
        ztcClient.zaaktypeList(ZaaktypeListParameters(catalogusURI)).results

    /**
     * Reads a {@link StatusType} via URI.
     * Throws a RuntimeException if the [StatusType] can not be read.
     *
     * @param statustypeURI URI of [StatusType].
     * @return [StatusType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_STATUSTYPE)
    fun readStatustype(statustypeURI: URI): StatusType =
        createInvocationBuilder(statustypeURI).get(StatusType::class.java)

    /**
     * Read [StatusType] via its UUID.
     * Throws a RuntimeException if the [StatusType] can not be read.
     *
     * @param statustypeUUID UUID of [StatusType].
     * @return [StatusType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_STATUSTYPE)
    fun readStatustype(statustypeUUID: UUID): StatusType =
        ztcClient.statustypeRead(statustypeUUID)

    /**
     * Read the [StatusType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [StatusType].
     */
    @CacheResult(cacheName = Caching.ZTC_STATUSTYPE)
    fun readStatustypen(zaaktypeURI: URI): List<StatusType> =
        ztcClient.statustypeList(StatustypeListParameters(zaaktypeURI)).singlePageResults

    /**
     * Read the [ZaakTypeInformatieObjectType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [ZaakTypeInformatieObjectType].
     */
    @CacheResult(cacheName = Caching.ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE)
    fun readZaaktypeInformatieobjecttypen(zaaktypeURI: URI): List<ZaakTypeInformatieObjectType> =
        ztcClient.zaaktypeinformatieobjecttypeList(ZaaktypeInformatieobjecttypeListParameters(zaaktypeURI))
            .singlePageResults

    /**
     * Read the [InformatieObjectType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [InformatieObjectType].
     */
    @CacheResult(cacheName = Caching.ZTC_INFORMATIEOBJECTTYPE)
    fun readInformatieobjecttypen(zaaktypeURI: URI): List<InformatieObjectType> =
        readZaaktypeInformatieobjecttypen(zaaktypeURI).stream()
            .map { zaaktypeInformatieobjecttype: ZaakTypeInformatieObjectType ->
                readInformatieobjecttype(
                    zaaktypeInformatieobjecttype.informatieobjecttype
                )
            }
            .toList()

    /**
     * Read [ResultaatType] via its URI.
     * Throws a RuntimeException if the [ResultaatType] can not be read.
     *
     * @param resultaattypeURI URI of [ResultaatType].
     * @return [ResultaatType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_RESULTAATTYPE)
    fun readResultaattype(resultaattypeURI: URI): ResultaatType =
        createInvocationBuilder(resultaattypeURI).get(ResultaatType::class.java)

    /**
     * Read [BesluitType] via its URI.
     * Throws a RuntimeException if the [BesluitType] can not be read.
     *
     * @param besluittypeURI URI of [BesluitType].
     * @return [BesluitType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_BESLUITTYPE)
    fun readBesluittype(besluittypeURI: URI): BesluitType =
        createInvocationBuilder(besluittypeURI).get(BesluitType::class.java)

    /**
     * Read [BesluitType] via its UUID.
     * Throws a RuntimeException if the [BesluitType] can not be read.
     *
     * @param besluittypeUUID UUID of [BesluitType].
     * @return [BesluitType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_BESLUITTYPE)
    fun readBesluittype(besluittypeUUID: UUID): BesluitType =
        ztcClient.besluittypeRead(besluittypeUUID)

    /**
     * Read the [BesluitType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [BesluitType].
     */
    @CacheResult(cacheName = Caching.ZTC_BESLUITTYPE)
    fun readBesluittypen(zaaktypeURI: URI): List<BesluitType> =
        ztcClient.besluittypeList(BesluittypeListParameters(zaaktypeURI)).singlePageResults

    /**
     * Read [ResultaatType] via its UUID.
     * Throws a RuntimeException if the [ResultaatType] can not be read.
     *
     * @param resultaattypeUUID UUID of [ResultaatType].
     * @return [ResultaatType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_RESULTAATTYPE)
    fun readResultaattype(resultaattypeUUID: UUID): ResultaatType =
        ztcClient.resultaattypeRead(resultaattypeUUID)

    /**
     * Read the [ResultaatType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [ResultaatType].
     */
    @CacheResult(cacheName = Caching.ZTC_RESULTAATTYPE)
    fun readResultaattypen(zaaktypeURI: URI): List<ResultaatType> =
        ztcClient.resultaattypeList(ResultaattypeListParameters(zaaktypeURI)).singlePageResults

    /**
     * Find [RolType] of [ZaakType] and [RolType.OmschrijvingGeneriekEnum].
     * returns null if the [ResultaatType] can not be found
     *
     * @param zaaktypeURI              URI of [ZaakType].
     * @param omschrijvingGeneriekEnum [RolType.OmschrijvingGeneriekEnum].
     * @return [RolType] or NULL
     */
    @CacheResult(cacheName = Caching.ZTC_ROLTYPE)
    fun findRoltype(zaaktypeURI: URI, omschrijvingGeneriekEnum: OmschrijvingGeneriekEnum): Optional<RolType> =
        ztcClient.roltypeList(RoltypeListParameters(zaaktypeURI, omschrijvingGeneriekEnum)).singleResult

    /**
     * Read [RolType] of [ZaakType] and [RolType.OmschrijvingGeneriekEnum].
     * Throws a RuntimeException if the [ResultaatType] can not be read.
     *
     * @param zaaktypeURI              URI of [ZaakType].
     * @param omschrijvingGeneriekEnum [RolType.OmschrijvingGeneriekEnum].
     * @return [RolType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_ROLTYPE)
    fun readRoltype(omschrijvingGeneriekEnum: OmschrijvingGeneriekEnum, zaaktypeURI: URI): RolType =
        ztcClient.roltypeList(RoltypeListParameters(zaaktypeURI, omschrijvingGeneriekEnum)).singleResult
            .orElseThrow {
                RuntimeException("Zaaktype '$zaaktypeURI': Roltype with aard '$omschrijvingGeneriekEnum' not found.")
            }

    /**
     * Read [RolType]s of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [RolType]s.
     */
    @CacheResult(cacheName = Caching.ZTC_ROLTYPE)
    fun listRoltypen(zaaktypeURI: URI): List<RolType> =
        ztcClient.roltypeList(RoltypeListParameters(zaaktypeURI)).results

    /**
     * Read [RolType] via its UUID.
     * Throws a RuntimeException if the [RolType] can not be read.
     *
     * @param roltypeUUID UUID of [RolType].
     * @return [RolType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_ROLTYPE)
    fun readRoltype(roltypeUUID: UUID): RolType =
        ztcClient.roltypeRead(roltypeUUID)

    /**
     * Read [InformatieObjectType] via its URI.
     * Throws a RuntimeException if the [InformatieObjectType] can not be read.
     *
     * @param informatieobjecttypeURI URI of [InformatieObjectType].
     * @return [InformatieObjectType]. Never 'null'!
     */
    @CacheResult(cacheName = Caching.ZTC_INFORMATIEOBJECTTYPE)
    fun readInformatieobjecttype(informatieobjecttypeURI: URI): InformatieObjectType =
        createInvocationBuilder(informatieobjecttypeURI).get(InformatieObjectType::class.java)

    /**
     * Read [InformatieObjectType] via its UUID.
     * Throws a RuntimeException if the [InformatieObjectType] can not be read.
     *
     * @param informatieobjecttypeUUID UUID of [InformatieObjectType].
     * @return [InformatieObjectType].
     */
    @CacheResult(cacheName = Caching.ZTC_INFORMATIEOBJECTTYPE)
    fun readInformatieobjecttype(informatieobjecttypeUUID: UUID): InformatieObjectType =
        ztcClient.informatieObjectTypeRead(informatieobjecttypeUUID)

    @CacheRemoveAll(cacheName = Caching.ZTC_ZAAKTYPE)
    fun clearZaaktypeCache(): String = cleared(Caching.ZTC_ZAAKTYPE)

    @CacheRemoveAll(cacheName = Caching.ZTC_STATUSTYPE)
    fun clearStatustypeCache(): String = cleared(Caching.ZTC_STATUSTYPE)

    @CacheRemoveAll(cacheName = Caching.ZTC_RESULTAATTYPE)
    fun clearResultaattypeCache(): String = cleared(Caching.ZTC_RESULTAATTYPE)

    @CacheRemoveAll(cacheName = Caching.ZTC_INFORMATIEOBJECTTYPE)
    fun clearInformatieobjecttypeCache(): String = cleared(Caching.ZTC_INFORMATIEOBJECTTYPE)

    @CacheRemoveAll(cacheName = Caching.ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE)
    fun clearZaaktypeInformatieobjecttypeCache(): String = cleared(Caching.ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE)

    @CacheRemoveAll(cacheName = Caching.ZTC_BESLUITTYPE)
    fun clearBesluittypeCache(): String = cleared(Caching.ZTC_BESLUITTYPE)

    @CacheRemoveAll(cacheName = Caching.ZTC_ROLTYPE)
    fun clearRoltypeCache(): String = cleared(Caching.ZTC_ROLTYPE)

    @CacheRemoveAll(cacheName = Caching.ZTC_CACHE_TIME)
    fun clearCacheTime(): String = cleared(Caching.ZTC_CACHE_TIME)

    override fun cacheNames() = CACHES

    private fun createInvocationBuilder(uri: URI) =
        // for security reasons check if the provided URI starts with the value of the
        // environment variable that we use to configure the ztcClient
        uri.toString().startsWith(configuratieService.readZgwApiClientMpRestUrl()).let {
            JAXRSClientFactory.getOrCreateClient().target(uri)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, zgwClientHeadersFactory.generateJWTToken())
        } ?: throw IllegalStateException(
            "URI '$uri' does not start with value for environment variable " +
                "'${ConfiguratieService.ENV_VAR_ZGW_API_CLIENT_MP_REST_URL}': " +
                "'${configuratieService.readZgwApiClientMpRestUrl()}'"
        )

    companion object {
        private val CACHES: List<String> = java.util.List.of(
            Caching.ZTC_BESLUITTYPE,
            Caching.ZTC_CACHE_TIME,
            Caching.ZTC_RESULTAATTYPE,
            Caching.ZTC_STATUSTYPE,
            Caching.ZTC_INFORMATIEOBJECTTYPE,
            Caching.ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE,
            Caching.ZTC_ZAAKTYPE
        )
    }
}
