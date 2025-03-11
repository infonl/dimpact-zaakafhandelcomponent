/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import net.atos.client.util.JAXRSClientFactory
import net.atos.client.zgw.shared.cache.Caching
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import net.atos.client.zgw.ztc.model.generated.BesluitType
import net.atos.client.zgw.ztc.model.generated.Catalogus
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.client.zgw.ztc.model.generated.ResultaatType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.client.zgw.ztc.model.generated.StatusType
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.client.zgw.ztc.model.generated.ZaakTypeInformatieObjectType
import net.atos.zac.configuratie.ConfiguratieService
import nl.info.client.zgw.ztc.exception.CatalogusNotFoundException
import nl.info.client.zgw.ztc.exception.RoltypeNotFoundException
import nl.info.client.zgw.ztc.model.BesluittypeListParameters
import nl.info.client.zgw.ztc.model.CatalogusListParameters
import nl.info.client.zgw.ztc.model.ResultaattypeListParameters
import nl.info.client.zgw.ztc.model.RoltypeListGeneriekParameters
import nl.info.client.zgw.ztc.model.RoltypeListParameters
import nl.info.client.zgw.ztc.model.StatustypeListParameters
import nl.info.client.zgw.ztc.model.ZaaktypeInformatieobjecttypeListParameters
import nl.info.client.zgw.ztc.model.ZaaktypeListParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Encapsulates [ZtcClient] by providing caching and authentication.
 */
@ApplicationScoped
@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions")
class ZtcClientService @Inject constructor(
    @RestClient
    private val ztcClient: ZtcClient,
    private val zgwClientHeadersFactory: ZGWClientHeadersFactory,
    private val configuratieService: ConfiguratieService
) : Caching {
    companion object {
        private val CACHES = mutableMapOf<String, Cache<*, *>>()

        private val LOG = Logger.getLogger(ZtcClientService::class.java.name)
        private const val MAX_CACHE_SIZE: Long = 20
        private const val EXPIRATION_TIME_HOURS: Long = 1

        private fun <K, V> createCache(name: String, size: Long = MAX_CACHE_SIZE): Cache<K & Any, V> {
            val cache: Cache<K & Any, V> = Caffeine.newBuilder()
                .maximumSize(size)
                .expireAfterAccess(EXPIRATION_TIME_HOURS, TimeUnit.HOURS)
                .recordStats()
                .removalListener { key: K?, _: V?, cause ->
                    LOG.fine("Removing key: $key in cache $name because of: $cause")
                }.build()

            CACHES["ZTC $name"] = cache
            return cache
        }

        private val ztcTimeCache: Cache<String, ZonedDateTime> = createCache("Time", 1)

        private val uuidToZaakTypeCache: Cache<UUID, ZaakType> = createCache("UUID -> ZaakType")
        private val uriToZaakTypeCache: Cache<URI, ZaakType> = createCache("URI -> ZaakType")
        private val uriToZaakTypeListCache: Cache<URI, List<ZaakType>> = createCache("URI -> List<ZaakType>")

        private val uriToStatusTypeCache: Cache<URI, StatusType> = createCache("URI -> StatusType")
        private val uuidToStatusTypeCache: Cache<UUID, StatusType> = createCache("UUID -> StatusType")
        private val uriToStatusTypeListCache: Cache<URI, List<StatusType>> = createCache("URI -> List<StatusType>")

        private val uriToZaakTypeInformatieObjectTypeListCache: Cache<URI, List<ZaakTypeInformatieObjectType>> =
            createCache("URI -> List<ZaakTypeInformatieObjectType")

        private val uriToInformatieObjectTypeCache: Cache<URI, InformatieObjectType> =
            createCache("URI -> InformatieObjectType")
        private val uuidToInformatieObjectTypeCache: Cache<UUID, InformatieObjectType> =
            createCache("UUID -> InformatieObjectType")
        private val uriToInformatieObjectTypeListCache: Cache<URI, List<InformatieObjectType>> =
            createCache("URI -> List<InformatieObjectType>")

        private val uriToResultaatTypeCache: Cache<URI, ResultaatType> = createCache("URI -> ResultaatType")
        private val uuidToResultaatTypeCache: Cache<UUID, ResultaatType> = createCache("UUID -> ResultaatType")
        private val uriToResultaatTypeListCache: Cache<URI, List<ResultaatType>> =
            createCache("URI -> List<ResultaatType>")

        private val uriToBesluitTypeCache: Cache<URI, BesluitType> = createCache("URI -> BesluitType")
        private val uuidToBesluitTypeCache: Cache<UUID, BesluitType> = createCache("UUID -> BesluitType")
        private val uriToBesluitTypeListCache: Cache<URI, List<BesluitType>> = createCache("URI -> List<BesluitType>")

        private val uriOmschrijvingEnumToRolTypeCache: Cache<String, List<RolType>> =
            createCache("URI & OmschrijvingEnumT -> RolType")
        private val uriOmschrijvingGeneriekEnumToRolTypeCache: Cache<String, List<RolType>> =
            createCache("URI & OmschrijvingGeneriekEnumT -> RolType")
        private val uriToRolTypeListCache: Cache<URI, List<RolType>> = createCache("URI -> List<RolType>")
        private val rolTypeListCache: Cache<String, List<RolType>> = createCache("List<RolType>", 1)
        private val uuidToRolTypeCache: Cache<UUID, RolType> = createCache("UUID -> RolType")
    }

    fun listCatalogus(catalogusListParameters: CatalogusListParameters): Results<Catalogus> =
        ztcClient.catalogusList(catalogusListParameters)

    /**
     * Read [Catalogus] filtered by [CatalogusListParameters].
     *
     * @param catalogusListParameters [CatalogusListParameters].
     * @return [Catalogus]. Never 'null'
     * @throws CatalogusNotFoundException if no [Catalogus] could be found.
     */
    fun readCatalogus(catalogusListParameters: CatalogusListParameters): Catalogus =
        ztcClient.catalogusList(catalogusListParameters)
            .singleResult
            .orElseThrow {
                CatalogusNotFoundException(
                    "No catalogus found for catalogus list parameters '$catalogusListParameters'."
                )
            }

    fun resetCacheTimeToNow(): ZonedDateTime = ztcTimeCache.get(Caching.ZTC_CACHE_TIME) { ZonedDateTime.now() }

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
            readZaaktypeInformatieobjecttypen(zaaktypeURI)
                .map { readInformatieobjecttype(it.informatieobjecttype) }
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
     * Find all [RolType]s for a [ZaakType] and a role type description.
     *
     * @param zaaktypeURI thr URI of the zaak type
     * @param roltypeOmschrijving the role type description
     * @return the list of [RolType]s; may be empty if no rol types have been defined for the zaak type
     * and generic role type description.
     */
    fun findRoltypen(zaaktypeURI: URI, roltypeOmschrijving: String?): List<RolType> =
        uriOmschrijvingEnumToRolTypeCache.get("$zaaktypeURI$roltypeOmschrijving") {
            ztcClient.roltypeList(RoltypeListParameters(zaaktypeURI)).results.filter {
                // No query parameter is available for roltypeOmschrijving, so we filter here. See:
                // https://github.com/open-zaak/open-zaak/issues/1933
                it.omschrijving == roltypeOmschrijving
            }
        }

    /**
     * Find all [RolType]s for a [ZaakType] and generic role type description.
     *
     * @param zaaktypeURI thr URI of the zaak type
     * @param omschrijvingGeneriekEnum the generic role type description
     * @return the list of [RolType]s; may be empty if no rol types have been defined for the zaak type
     * and generic role type description.
     */
    fun findRoltypen(zaaktypeURI: URI, omschrijvingGeneriekEnum: OmschrijvingGeneriekEnum): List<RolType> =
        uriOmschrijvingGeneriekEnumToRolTypeCache.get("$zaaktypeURI$omschrijvingGeneriekEnum") {
            ztcClient.roltypeListGeneriek(RoltypeListGeneriekParameters(zaaktypeURI, omschrijvingGeneriekEnum)).results
        }

    /**
     * Retrieves the [RolType] of the specified zaak type and generic role type description.
     * If there are multiple role types found the first one is returned.
     * This method should only be used for role type descriptions for which you are sure there is one and only
     * one role type defined in the zaaktype.
     *
     * @param zaaktypeURI URI of the zaak type
     * @param omschrijvingGeneriekEnum the generic role type description
     * @return [RolType] the first role type for the zaak type and generic role type description
     * @throws RoltypeNotFoundException if no role type could be found
     */
    fun readRoltype(zaaktypeURI: URI, omschrijvingGeneriekEnum: OmschrijvingGeneriekEnum): RolType =
        uriOmschrijvingGeneriekEnumToRolTypeCache.get("$zaaktypeURI$omschrijvingGeneriekEnum") {
            ztcClient.roltypeListGeneriek(RoltypeListGeneriekParameters(zaaktypeURI, omschrijvingGeneriekEnum)).results
        }.firstOrNull() ?: throw
            RoltypeNotFoundException(
                "Roltype with aard '$omschrijvingGeneriekEnum' not found for zaaktype '$zaaktypeURI':"
            )

    /**
     * Returns all [RolType]s for the specified [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType]
     * @return list of [RolType]s
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
        uriOmschrijvingEnumToRolTypeCache.invalidateAll()
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

    override fun cacheStatistics(): Map<String, CacheStats> =
        CACHES.mapValuesTo(mutableMapOf<String, CacheStats>()) { it.value.stats() }

    override fun cacheSizes(): Map<String, Long> =
        CACHES.mapValuesTo(mutableMapOf()) { it.value.estimatedSize() }

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
