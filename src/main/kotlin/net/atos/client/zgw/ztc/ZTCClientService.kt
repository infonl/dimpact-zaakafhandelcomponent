/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
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
import nl.lifely.zac.util.AllOpen
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.net.URI
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID
import java.util.logging.Logger

/**
 * Encapsulates [ZTCClient] by providing caching and authentication.
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

    companion object {
        private val CACHES = listOf(
            Caching.ZTC_BESLUITTYPE,
            Caching.ZTC_CACHE_TIME,
            Caching.ZTC_RESULTAATTYPE,
            Caching.ZTC_STATUSTYPE,
            Caching.ZTC_INFORMATIEOBJECTTYPE,
            Caching.ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE,
            Caching.ZTC_ZAAKTYPE
        )

        private val LOG = Logger.getLogger(ZTCClientService::class.java.name)

        private fun <K, V> createCache(size: Long = 100): Cache<K, V> =
            Caffeine.newBuilder()
                .maximumSize(size)
                .recordStats()
                .removalListener { key: K?, _: V?, cause ->
                    LOG.info("Remove key : $key because : $cause")
                }.build()

        private val ztcTimeCache: Cache<String, ZonedDateTime> = createCache(1)

        private val uuidToZaakTypeCache: Cache<UUID, ZaakType> = createCache()
        private val uriToZaakTypeCache: Cache<URI, ZaakType> = createCache()
        private val uriToZaakTypeListCache: Cache<URI, List<ZaakType>> = createCache()

        private val uriToStatusTypeCache: Cache<URI, StatusType> = createCache()
        private val uuidToStatusTypeCache: Cache<UUID, StatusType> = createCache()
        private val uriToStatusTypeListCache: Cache<URI, List<StatusType>> = createCache()

        private val uriToZaakTypeInformatieObjectTypeListCache: Cache<URI, List<ZaakTypeInformatieObjectType>> =
            createCache()

        private val uriToInformatieObjectTypeCache: Cache<URI, InformatieObjectType> = createCache()
        private val uuidToInformatieObjectTypeCache: Cache<UUID, InformatieObjectType> = createCache()
        private val uriToInformatieObjectTypeListCache: Cache<URI, List<InformatieObjectType>> = createCache()

        private val uriToResultaatTypeCache: Cache<URI, ResultaatType> = createCache()
        private val uuidToResultaatTypeCache: Cache<UUID, ResultaatType> = createCache()
        private val uriToResultaatTypeListCache: Cache<URI, List<ResultaatType>> = createCache()

        private val uriToBesluitTypeCache: Cache<URI, BesluitType> = createCache()
        private val uuidToBesluitTypeCache: Cache<UUID, BesluitType> = createCache()
        private val uriToBesluitTypeListCache: Cache<URI, List<BesluitType>> = createCache()

        private val uriOmschrijvingGeneriekEnumToRolTypeCache: Cache<String, Optional<RolType>> = createCache()
        private val uriToRolTypeListCache: Cache<URI, List<RolType>> = createCache()
        private val rolTypeListCache: Cache<String, List<RolType>> = createCache(1)
        private val uuidToRolTypeCache: Cache<UUID, RolType> = createCache()
    }

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

    fun readCacheTime(): ZonedDateTime = ztcTimeCache.get(Caching.ZTC_CACHE_TIME) { ZonedDateTime.now() }

    /**
     * Read [ZaakType] via URI.
     * Throws a RuntimeException if the [ZaakType] can not be read.
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return [ZaakType]. Never 'null'!
     */
    fun readZaaktype(zaaktypeURI: URI): ZaakType = uriToZaakTypeCache.get(zaaktypeURI) {
        createInvocationBuilder(zaaktypeURI).get(ZaakType::class.java)
    }

    /**
     * Read [ZaakType] via UUID.
     * Throws a RuntimeException if the [ZaakType] can not be read.
     *
     * @param zaaktypeUuid UUID of [ZaakType].
     * @return [ZaakType]. Never 'null'!
     */
    fun readZaaktype(zaaktypeUuid: UUID): ZaakType = uuidToZaakTypeCache.get(zaaktypeUuid) {
        ztcClient.zaaktypeRead(zaaktypeUuid)
    }

    /**
     * List instances of [ZaakType] in [Catalogus].
     *
     * @param catalogusURI URI of [Catalogus].
     * @return List of [ZaakType] instances
     */
    fun listZaaktypen(catalogusURI: URI): List<ZaakType> = uriToZaakTypeListCache.get(catalogusURI) {
        ztcClient.zaaktypeList(ZaaktypeListParameters(catalogusURI)).results
    }

    /**
     * Reads a [StatusType] via URI.
     * Throws a RuntimeException if the [StatusType] can not be read.
     *
     * @param statustypeURI URI of [StatusType].
     * @return [StatusType]. Never 'null'!
     */
    fun readStatustype(statustypeURI: URI): StatusType = uriToStatusTypeCache.get(statustypeURI) {
        createInvocationBuilder(statustypeURI).get(StatusType::class.java)
    }

    /**
     * Read [StatusType] via its UUID.
     * Throws a RuntimeException if the [StatusType] can not be read.
     *
     * @param statustypeUUID UUID of [StatusType].
     * @return [StatusType]. Never 'null'!
     */
    fun readStatustype(statustypeUUID: UUID): StatusType = uuidToStatusTypeCache.get(statustypeUUID) {
        ztcClient.statustypeRead(statustypeUUID)
    }

    /**
     * Read the [StatusType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [StatusType].
     */
    fun readStatustypen(zaaktypeURI: URI): List<StatusType> = uriToStatusTypeListCache.get(zaaktypeURI) {
        ztcClient.statustypeList(StatustypeListParameters(zaaktypeURI)).singlePageResults
    }

    /**
     * Read the [ZaakTypeInformatieObjectType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [ZaakTypeInformatieObjectType].
     */
    fun readZaaktypeInformatieobjecttypen(zaaktypeURI: URI): List<ZaakTypeInformatieObjectType> =
        uriToZaakTypeInformatieObjectTypeListCache.get(zaaktypeURI) {
            ztcClient.zaaktypeinformatieobjecttypeList(ZaaktypeInformatieobjecttypeListParameters(zaaktypeURI))
                .singlePageResults
        }

    /**
     * Read the [InformatieObjectType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [InformatieObjectType].
     */
    fun readInformatieobjecttypen(zaaktypeURI: URI): List<InformatieObjectType> =
        uriToInformatieObjectTypeListCache.get(zaaktypeURI) {
            readZaaktypeInformatieobjecttypen(zaaktypeURI).stream()
                .map { readInformatieobjecttype(it.informatieobjecttype) }
                .toList()
        }

    /**
     * Read [ResultaatType] via its URI.
     * Throws a RuntimeException if the [ResultaatType] can not be read.
     *
     * @param resultaattypeURI URI of [ResultaatType].
     * @return [ResultaatType]. Never 'null'!
     */
    fun readResultaattype(resultaattypeURI: URI): ResultaatType = uriToResultaatTypeCache.get(resultaattypeURI) {
        createInvocationBuilder(resultaattypeURI).get(ResultaatType::class.java)
    }

    /**
     * Read [BesluitType] via its URI.
     * Throws a RuntimeException if the [BesluitType] can not be read.
     *
     * @param besluittypeURI URI of [BesluitType].
     * @return [BesluitType]. Never 'null'!
     */
    fun readBesluittype(besluittypeURI: URI): BesluitType = uriToBesluitTypeCache.get(besluittypeURI) {
        createInvocationBuilder(besluittypeURI).get(BesluitType::class.java)
    }

    /**
     * Read [BesluitType] via its UUID.
     * Throws a RuntimeException if the [BesluitType] can not be read.
     *
     * @param besluittypeUUID UUID of [BesluitType].
     * @return [BesluitType]. Never 'null'!
     */
    fun readBesluittype(besluittypeUUID: UUID): BesluitType = uuidToBesluitTypeCache.get(besluittypeUUID) {
        ztcClient.besluittypeRead(besluittypeUUID)
    }

    /**
     * Read the [BesluitType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [BesluitType].
     */
    fun readBesluittypen(zaaktypeURI: URI): List<BesluitType> = uriToBesluitTypeListCache.get(zaaktypeURI) {
        ztcClient.besluittypeList(BesluittypeListParameters(zaaktypeURI)).singlePageResults
    }

    /**
     * Read [ResultaatType] via its UUID.
     * Throws a RuntimeException if the [ResultaatType] can not be read.
     *
     * @param resultaattypeUUID UUID of [ResultaatType].
     * @return [ResultaatType]. Never 'null'!
     */
    fun readResultaattype(resultaattypeUUID: UUID): ResultaatType = uuidToResultaatTypeCache.get(resultaattypeUUID) {
        ztcClient.resultaattypeRead(resultaattypeUUID)
    }

    /**
     * Read the [ResultaatType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [ResultaatType].
     */
    fun readResultaattypen(zaaktypeURI: URI): List<ResultaatType> = uriToResultaatTypeListCache.get(zaaktypeURI) {
        ztcClient.resultaattypeList(ResultaattypeListParameters(zaaktypeURI)).singlePageResults
    }

    /**
     * Find [RolType] of [ZaakType] and [RolType.OmschrijvingGeneriekEnum].
     * returns null if the [ResultaatType] can not be found
     *
     * @param zaaktypeURI              URI of [ZaakType].
     * @param omschrijvingGeneriekEnum [RolType.OmschrijvingGeneriekEnum].
     * @return [RolType] or NULL
     */
    fun findRoltype(zaaktypeURI: URI, omschrijvingGeneriekEnum: OmschrijvingGeneriekEnum): Optional<RolType> =
        uriOmschrijvingGeneriekEnumToRolTypeCache.get("$zaaktypeURI$omschrijvingGeneriekEnum") {
            ztcClient.roltypeList(RoltypeListParameters(zaaktypeURI, omschrijvingGeneriekEnum)).singleResult
        }

    /**
     * Read [RolType] of [ZaakType] and [RolType.OmschrijvingGeneriekEnum].
     * Throws a RuntimeException if the [ResultaatType] can not be read.
     *
     * @param zaaktypeURI              URI of [ZaakType].
     * @param omschrijvingGeneriekEnum [RolType.OmschrijvingGeneriekEnum].
     * @return [RolType]. Never 'null'!
     */
    fun readRoltype(omschrijvingGeneriekEnum: OmschrijvingGeneriekEnum, zaaktypeURI: URI): RolType =
        uriOmschrijvingGeneriekEnumToRolTypeCache.get("$zaaktypeURI$omschrijvingGeneriekEnum") {
            ztcClient.roltypeList(RoltypeListParameters(zaaktypeURI, omschrijvingGeneriekEnum)).singleResult
        }.orElseThrow {
            RuntimeException("Zaaktype '$zaaktypeURI': Roltype with aard '$omschrijvingGeneriekEnum' not found.")
        }

    /**
     * Read [RolType]s of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [RolType]s.
     */
    fun listRoltypen(zaaktypeURI: URI): List<RolType> = uriToRolTypeListCache.get(zaaktypeURI) {
        ztcClient.roltypeList(RoltypeListParameters(zaaktypeURI)).results
    }

    /**
     * Read [RolType]s of [ZaakType].
     *
     * @return list of [RolType]s.
     */
    fun listRoltypen(): List<RolType> = rolTypeListCache.get(Caching.ZTC_ROLTYPE) {
        ztcClient.roltypeList().results
    }

    /**
     * Read [RolType] via its UUID.
     * Throws a RuntimeException if the [RolType] can not be read.
     *
     * @param roltypeUUID UUID of [RolType].
     * @return [RolType]. Never 'null'!
     */
    fun readRoltype(roltypeUUID: UUID): RolType = uuidToRolTypeCache.get(roltypeUUID) {
        ztcClient.roltypeRead(roltypeUUID)
    }

    /**
     * Read [InformatieObjectType] via its URI.
     * Throws a RuntimeException if the [InformatieObjectType] can not be read.
     *
     * @param informatieobjecttypeURI URI of [InformatieObjectType].
     * @return [InformatieObjectType]. Never 'null'!
     */
    fun readInformatieobjecttype(informatieobjecttypeURI: URI): InformatieObjectType =
        uriToInformatieObjectTypeCache.get(informatieobjecttypeURI) {
            createInvocationBuilder(informatieobjecttypeURI).get(InformatieObjectType::class.java)
        }

    /**
     * Read [InformatieObjectType] via its UUID.
     * Throws a RuntimeException if the [InformatieObjectType] can not be read.
     *
     * @param informatieobjecttypeUUID UUID of [InformatieObjectType].
     * @return [InformatieObjectType].
     */
    fun readInformatieobjecttype(informatieobjecttypeUUID: UUID): InformatieObjectType =
        uuidToInformatieObjectTypeCache.get(informatieobjecttypeUUID) {
            ztcClient.informatieObjectTypeRead(informatieobjecttypeUUID)
        }

    fun clearZaaktypeCache(): String {
        uuidToZaakTypeCache.invalidateAll()
        uriToZaakTypeCache.invalidateAll()
        uriToZaakTypeListCache.invalidateAll()
        return cleared(Caching.ZTC_ZAAKTYPE)
    }

    fun clearStatustypeCache(): String {
        uriToStatusTypeCache.invalidateAll()
        uuidToStatusTypeCache.invalidateAll()
        uriToStatusTypeListCache.invalidateAll()
        return cleared(Caching.ZTC_STATUSTYPE)
    }

    fun clearResultaattypeCache(): String {
        uriToResultaatTypeCache.invalidateAll()
        uuidToResultaatTypeCache.invalidateAll()
        uriToResultaatTypeListCache.invalidateAll()
        return cleared(Caching.ZTC_RESULTAATTYPE)
    }

    fun clearInformatieobjecttypeCache(): String {
        uriToInformatieObjectTypeCache.invalidateAll()
        uuidToInformatieObjectTypeCache.invalidateAll()
        uriToInformatieObjectTypeListCache.invalidateAll()
        return cleared(Caching.ZTC_INFORMATIEOBJECTTYPE)
    }

    fun clearZaaktypeInformatieobjecttypeCache(): String {
        uriToZaakTypeInformatieObjectTypeListCache.invalidateAll()
        return cleared(Caching.ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE)
    }

    fun clearBesluittypeCache(): String {
        uriToBesluitTypeCache.invalidateAll()
        uuidToBesluitTypeCache.invalidateAll()
        uriToBesluitTypeListCache.invalidateAll()
        return cleared(Caching.ZTC_BESLUITTYPE)
    }

    fun clearRoltypeCache(): String {
        uriOmschrijvingGeneriekEnumToRolTypeCache.invalidateAll()
        uriToRolTypeListCache.invalidateAll()
        rolTypeListCache.invalidateAll()
        uuidToRolTypeCache.invalidateAll()
        return cleared(Caching.ZTC_ROLTYPE)
    }

    fun clearCacheTime(): String {
        ztcTimeCache.invalidateAll()
        return cleared(Caching.ZTC_CACHE_TIME)
    }

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
}
