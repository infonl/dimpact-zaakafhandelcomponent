/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.zgw.shared.cache.CacheMemoryUsage
import nl.info.client.zgw.shared.cache.Caching
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.exception.CatalogusNotFoundException
import nl.info.client.zgw.ztc.exception.EigenschapNotFoundException
import nl.info.client.zgw.ztc.exception.RoltypeNotFoundException
import nl.info.client.zgw.ztc.model.BesluittypeListParameters
import nl.info.client.zgw.ztc.model.CatalogusListParameters
import nl.info.client.zgw.ztc.model.EigenschapListParameters
import nl.info.client.zgw.ztc.model.EigenschapListParametersStatus
import nl.info.client.zgw.ztc.model.InformatieobjecttypeListParameters
import nl.info.client.zgw.ztc.model.ObjectStatusFilter
import nl.info.client.zgw.ztc.model.ResultaattypeListParameters
import nl.info.client.zgw.ztc.model.RoltypeListGeneriekParameters
import nl.info.client.zgw.ztc.model.RoltypeListParameters
import nl.info.client.zgw.ztc.model.StatustypeListParameters
import nl.info.client.zgw.ztc.model.ZaaktypeInformatieobjecttypeListParameters
import nl.info.client.zgw.ztc.model.ZaaktypeListParameters
import nl.info.client.zgw.ztc.model.generated.BesluitType
import nl.info.client.zgw.ztc.model.generated.Catalogus
import nl.info.client.zgw.ztc.model.generated.Eigenschap
import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.ResultaatType
import nl.info.client.zgw.ztc.model.generated.RolType
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.client.zgw.ztc.model.generated.ZaakTypeInformatieObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.openjdk.jol.info.GraphLayout
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.Level
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

    @ConfigProperty(name = ENV_VAR_ZTC_CACHE_MAX_SIZE, defaultValue = DEFAULT_CACHE_MAX_SIZE)
    private val cacheMaxSize: Long,

    @ConfigProperty(name = ENV_VAR_ZTC_CACHE_EXPIRATION_SECONDS, defaultValue = DEFAULT_CACHE_EXPIRATION_SECONDS)
    private val cacheExpirationSeconds: Long
) : Caching {
    companion object {
        const val ENV_VAR_ZTC_CACHE_MAX_SIZE = "ZTC_CACHE_MAX_SIZE"
        const val DEFAULT_CACHE_MAX_SIZE = "10000"
        const val ENV_VAR_ZTC_CACHE_EXPIRATION_SECONDS = "ZTC_CACHE_EXPIRATION_SECONDS"
        const val DEFAULT_CACHE_EXPIRATION_SECONDS = "86400"
        const val ZAAKTYPE_PAGE_SIZE: Int = 100

        private const val ZTC_EIGENSCHAP = "ztc-eigenschap"
        private const val FIRST_PAGE = 1

        private val LOG = Logger.getLogger(ZtcClientService::class.java.name)
    }

    private val caches = mutableMapOf<String, Cache<*, *>>()
    private val cacheLock = Any()
    private var runningRefreshes = 0
    private val invalidationsDuringRefresh = mutableMapOf<Cache<*, *>, MutableList<(Any, Any?) -> Boolean>>()

    private val ztcTimeCache: Cache<String, ZonedDateTime> = createCache("Time", 1)

    private val uuidToZaakTypeCache: Cache<UUID, ZaakType> = createCache("UUID -> ZaakType")
    private val uriToZaakTypeListCache: Cache<URI, List<ZaakType>> = createCache("URI -> List<ZaakType>")
    private val uuidToStatusTypeCache: Cache<UUID, StatusType> = createCache("UUID -> StatusType")
    private val uriToStatusTypeListCache: Cache<URI, List<StatusType>> = createCache("URI -> List<StatusType>")
    private val uriToZaakTypeInformatieObjectTypeListCache: Cache<URI, List<ZaakTypeInformatieObjectType>> =
        createCache("URI -> List<ZaakTypeInformatieObjectType>")
    private val uuidToInformatieObjectTypeCache: Cache<UUID, InformatieObjectType> =
        createCache("UUID -> InformatieObjectType")
    private val uriToInformatieObjectTypeListCache: Cache<URI, List<InformatieObjectType>> =
        createCache("URI -> List<InformatieObjectType>")
    private val uuidToResultaatTypeCache: Cache<UUID, ResultaatType> = createCache("UUID -> ResultaatType")
    private val uriToResultaatTypeListCache: Cache<URI, List<ResultaatType>> =
        createCache("URI -> List<ResultaatType>")
    private val uuidToBesluitTypeCache: Cache<UUID, BesluitType> = createCache("UUID -> BesluitType")
    private val uriToBesluitTypeListCache: Cache<URI, List<BesluitType>> = createCache("URI -> List<BesluitType>")
    private val uriOmschrijvingEnumToRolTypeCache: Cache<String, List<RolType>> =
        createCache("URI & OmschrijvingEnumT -> RolType")
    private val uriOmschrijvingGeneriekEnumToRolTypeCache: Cache<String, List<RolType>> =
        createCache("URI & OmschrijvingGeneriekEnumT -> RolType")
    private val uriToRolTypeListCache: Cache<URI, List<RolType>> = createCache("URI -> List<RolType>")
    private val rolTypeListCache: Cache<String, List<RolType>> = createCache("List<RolType>", 1)
    private val uuidToRolTypeCache: Cache<UUID, RolType> = createCache("UUID -> RolType")
    private val uriToEigenschappenCache: Cache<URI, List<Eigenschap>> = createCache("URI -> List<Eigenschap>")

    /**
     * Entries expire only when they have been neither read nor written for [cacheExpirationSeconds]. ZAC ignores
     * notifications about deleted zaaktypen, so a zaaktype that was removed from the ZTC disappears from the cache
     * only once it has not been used for that time.
     */
    private fun <K, V> createCache(name: String, size: Long = cacheMaxSize): Cache<K & Any, V> {
        val cache: Cache<K & Any, V> = Caffeine.newBuilder()
            .maximumSize(size)
            .expireAfterAccess(cacheExpirationSeconds, TimeUnit.SECONDS)
            .recordStats()
            .removalListener { key: K?, _: V?, cause ->
                LOG.fine { "Removing key: $key in cache $name because of: $cause" }
            }.build()

        caches["ZTC $name"] = cache
        return cache
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
        ztcClient.catalogusList(catalogusListParameters).singleResult
            ?: throw CatalogusNotFoundException(
                "No catalogus found for catalogus list parameters '$catalogusListParameters'."
            )

    fun resetCacheTimeToNow(): ZonedDateTime = ztcTimeCache.get(Caching.ZTC_CACHE_TIME) { ZonedDateTime.now() }

    /**
     * Read [ZaakType] via URI.
     * Throws a RuntimeException if the [ZaakType] can not be read.
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return [ZaakType]. Never 'null'!
     */
    fun readZaaktype(zaaktypeURI: URI): ZaakType = readZaaktype(zaaktypeURI.extractUuid())

    /**
     * Read [ZaakType] via UUID.
     * Throws a RuntimeException if the [ZaakType] can not be read.
     *
     * @param zaaktypeUuid UUID of [ZaakType].
     * @return [ZaakType]. Never 'null'!
     */
    fun readZaaktype(zaaktypeUuid: UUID): ZaakType = uuidToZaakTypeCache.get(zaaktypeUuid) {
        LOG.fine { "OpenZaak: Retrieving zaaktype $zaaktypeUuid" }
        ztcClient.zaaktypeRead(zaaktypeUuid)
    }

    /**
     * List instances of [ZaakType] in [Catalogus], retrieving all pages.
     *
     * @param catalogusURI URI of [Catalogus].
     * @return List of [ZaakType] instances
     */
    fun listZaaktypen(catalogusURI: URI): List<ZaakType> = uriToZaakTypeListCache.get(catalogusURI) {
        LOG.fine { "OpenZaak: listZaaktypen(catalogusURI $catalogusURI)" }
        fetchZaaktypen(catalogusURI).onEach { zaaktype -> uuidToZaakTypeCache.put(zaaktype.url.extractUuid(), zaaktype) }
    }

    private fun fetchZaaktypen(catalogusURI: URI) =
        listAllPages { page ->
            ztcClient.zaaktypeList(ZaaktypeListParameters(catalogusURI, page = page, pageSize = ZAAKTYPE_PAGE_SIZE))
        }

    /**
     * Reads a [StatusType] via URI.
     * Throws a RuntimeException if the [StatusType] can not be read.
     *
     * @param statustypeURI URI of [StatusType].
     * @return [StatusType]. Never 'null'!
     */
    fun readStatustype(statustypeURI: URI): StatusType = readStatustype(statustypeURI.extractUuid())

    /**
     * Read [StatusType] via its UUID.
     * Throws a RuntimeException if the [StatusType] can not be read.
     *
     * @param statustypeUUID UUID of [StatusType].
     * @return [StatusType]. Never 'null'!
     */
    fun readStatustype(statustypeUUID: UUID): StatusType = uuidToStatusTypeCache.get(statustypeUUID) {
        LOG.fine { "OpenZaak: readStatustype(statustype: $statustypeUUID)" }
        ztcClient.statustypeRead(statustypeUUID)
    }

    /**
     * Read the [StatusType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [StatusType].
     */
    fun readStatustypen(zaaktypeURI: URI): List<StatusType> = uriToStatusTypeListCache.get(zaaktypeURI) {
        LOG.fine { "OpenZaak: readStatustypen(zaaktypeURI: $zaaktypeURI)" }
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
            LOG.fine { "OpenZaak: readZaaktypeInformatieobjecttypen(zaaktypeURI: $zaaktypeURI)" }
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
            LOG.fine { "OpenZaak: readInformatieobjecttypen(zaaktypeURI: $zaaktypeURI)" }
            readZaaktypeInformatieobjecttypen(zaaktypeURI)
                .map { zaaktypeInformatieobjecttype -> readInformatieobjecttype(zaaktypeInformatieobjecttype.informatieobjecttype) }
        }

    /**
     * Read [ResultaatType] via its URI.
     * Throws a RuntimeException if the [ResultaatType] can not be read.
     *
     * @param resultaattypeURI URI of [ResultaatType].
     * @return [ResultaatType]. Never 'null'!
     */
    fun readResultaattype(resultaattypeURI: URI): ResultaatType = readResultaattype(resultaattypeURI.extractUuid())

    /**
     * Read [BesluitType] via its URI.
     * Throws a RuntimeException if the [BesluitType] can not be read.
     *
     * @param besluittypeURI URI of [BesluitType].
     * @return [BesluitType]. Never 'null'!
     */
    fun readBesluittype(besluittypeURI: URI): BesluitType = readBesluittype(besluittypeURI.extractUuid())

    /**
     * Read [BesluitType] via its UUID.
     * Throws a RuntimeException if the [BesluitType] can not be read.
     *
     * @param besluittypeUUID UUID of [BesluitType].
     * @return [BesluitType]. Never 'null'!
     */
    fun readBesluittype(besluittypeUUID: UUID): BesluitType = uuidToBesluitTypeCache.get(besluittypeUUID) {
        LOG.fine { "OpenZaak: readBesluittype(besluittypeUUID: $besluittypeUUID)" }
        ztcClient.besluittypeRead(besluittypeUUID)
    }

    /**
     * Read the [BesluitType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [BesluitType].
     */
    fun readBesluittypen(zaaktypeURI: URI): List<BesluitType> = uriToBesluitTypeListCache.get(zaaktypeURI) {
        LOG.fine { "OpenZaak: readBesluittypen(zaaktypeURI $zaaktypeURI)" }
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
        LOG.fine { "OpenZaak: readResultaattype(resultaattypeUUID: $resultaattypeUUID)" }
        ztcClient.resultaattypeRead(resultaattypeUUID)
    }

    /**
     * Read the [ResultaatType] of [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType].
     * @return list of [ResultaatType].
     */
    fun readResultaattypen(zaaktypeURI: URI): List<ResultaatType> = uriToResultaatTypeListCache.get(zaaktypeURI) {
        LOG.fine { "OpenZaak: readResultaattypen(zaaktypeURI $zaaktypeURI)" }
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
            LOG.fine { "OpenZaak: findRoltypen(zaaktypeURI $zaaktypeURI, roltypeOmschrijving $roltypeOmschrijving)" }
            ztcClient.roltypeList(RoltypeListParameters(zaaktypeURI)).results().filter { rolType ->
                // No query parameter is available for roltypeOmschrijving, so we filter here. See:
                // https://github.com/open-zaak/open-zaak/issues/1933
                rolType.omschrijving == roltypeOmschrijving
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
            LOG.fine { "OpenZaak: findRoltypen(zaaktypeURI $zaaktypeURI, omschrijvingGeneriekEnum $omschrijvingGeneriekEnum)" }
            ztcClient.roltypeListGeneriek(
                RoltypeListGeneriekParameters(zaaktypeURI, omschrijvingGeneriekEnum)
            ).results()
        }

    /**
     * Returns all [RolType]s for the specified [ZaakType].
     *
     * @param zaaktypeURI URI of [ZaakType]
     * @return list of [RolType]s
     */
    fun listRoltypen(zaaktypeURI: URI): List<RolType> = uriToRolTypeListCache.get(zaaktypeURI) {
        ztcClient.roltypeList(RoltypeListParameters(zaaktypeURI)).results()
    }

    /**
     * Read [RolType]s of [ZaakType].
     *
     * @return list of [RolType]s.
     */
    fun listRoltypen(): List<RolType> = rolTypeListCache.get(Caching.ZTC_ROLTYPE) {
        LOG.fine { "OpenZaak: listRoltypen()" }
        ztcClient.roltypeList().results()
    }

    /**
     * Read [RolType] via its UUID.
     * Throws a RuntimeException if the [RolType] can not be read.
     *
     * @param roltypeUUID UUID of [RolType].
     * @return [RolType]. Never 'null'!
     */
    fun readRoltype(roltypeUUID: UUID): RolType = uuidToRolTypeCache.get(roltypeUUID) {
        LOG.fine { "OpenZaak: readRoltype(roltypeUUID $roltypeUUID)" }
        ztcClient.roltypeRead(roltypeUUID)
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
            LOG.fine { "OpenZaak: readRoltype(zaaktypeURI $zaaktypeURI, omschrijvingGeneriekEnum $omschrijvingGeneriekEnum)" }
            ztcClient.roltypeListGeneriek(RoltypeListGeneriekParameters(zaaktypeURI, omschrijvingGeneriekEnum)).results()
        }.firstOrNull() ?: throw
            RoltypeNotFoundException(
                "Roltype with aard '$omschrijvingGeneriekEnum' not found for zaaktype '$zaaktypeURI':"
            )

    /**
     * Retrieves the [RolType] of the specified zaak type with the given generic role type description
     * and the given specific description (omschrijving).
     *
     * @param zaaktypeURI URI of the zaak type
     * @param omschrijvingGeneriekEnum the generic role type description
     * @param omschrijving the specific role type description (exact match)
     * @throws RoltypeNotFoundException if no matching role type could be found
     */
    fun readRoltype(
        zaaktypeURI: URI,
        omschrijvingGeneriekEnum: OmschrijvingGeneriekEnum,
        omschrijving: String
    ): RolType =
        findRoltypen(zaaktypeURI, omschrijvingGeneriekEnum)
            .firstOrNull { it.omschrijving == omschrijving }
            ?: throw RoltypeNotFoundException(
                "Roltype with aard '$omschrijvingGeneriekEnum' and omschrijving '$omschrijving' " +
                    "not found for zaaktype '$zaaktypeURI'"
            )

    /**
     * Read [InformatieObjectType] via its URI.
     * Throws a RuntimeException if the [InformatieObjectType] can not be read.
     *
     * @param informatieobjecttypeURI URI of [InformatieObjectType].
     * @return [InformatieObjectType]. Never 'null'!
     */
    fun readInformatieobjecttype(informatieobjecttypeURI: URI): InformatieObjectType =
        readInformatieobjecttype(informatieobjecttypeURI.extractUuid())

    /**
     * Read [InformatieObjectType] via its UUID.
     * Throws a RuntimeException if the [InformatieObjectType] can not be read.
     *
     * @param informatieobjecttypeUUID UUID of [InformatieObjectType].
     * @return [InformatieObjectType].
     */
    fun readInformatieobjecttype(informatieobjecttypeUUID: UUID): InformatieObjectType =
        uuidToInformatieObjectTypeCache.get(informatieobjecttypeUUID) {
            LOG.fine { "OpenZaak: readInformatieobjecttype(informatieobjecttypeUUID $informatieobjecttypeUUID)" }
            ztcClient.informatieObjectTypeRead(informatieobjecttypeUUID)
        }

    fun readEigenschappen(
        zaaktype: URI,
        status: EigenschapListParametersStatus = EigenschapListParametersStatus.DEFINITIEF
    ) =
        uriToEigenschappenCache.get(zaaktype) {
            LOG.fine { "OpenZaak: readEigenschappen(zaaktype=$zaaktype, status=$status)" }
            val response = ztcClient.eigenschapList(
                EigenschapListParameters().apply {
                    this.zaaktype = zaaktype
                    this.status = status
                }
            )
            response.results()
        }

    fun findEigenschap(zaaktype: URI, eigenschap: String): Eigenschap? =
        this.readEigenschappen(zaaktype).find { it.naam == eigenschap }

    fun readEigenschap(zaaktype: URI, eigenschap: String) =
        this.findEigenschap(zaaktype, eigenschap)
            ?: throw EigenschapNotFoundException(
                """
                No '${EigenschapListParametersStatus.DEFINITIEF}' eigenschap with naam '$eigenschap' found for zaaktype '$zaaktype'.
                """.trimIndent()
            )

    /**
     * Fills the caches with the zaaktypes of the given catalogus and their related types, with one paged list
     * request per type instead of one request per zaaktype. A type that fails to refresh is logged and skipped,
     * so that its cache is still filled on demand.
     *
     * The caches are written only after all types have been fetched, without the entries that were invalidated in
     * the meantime: the fetched types may predate the change that caused the invalidation, and writing them would
     * put back entries that the invalidation removed, such as a deleted type. Those entries are filled on demand.
     */
    fun refreshCaches(catalogusURI: URI) {
        synchronized(cacheLock) { runningRefreshes++ }
        try {
            fetchTypes(catalogusURI)?.let(::writeCaches)
        } finally {
            synchronized(cacheLock) {
                if (--runningRefreshes == 0) invalidationsDuringRefresh.clear()
            }
        }
    }

    private fun fetchTypes(catalogusURI: URI): List<() -> Unit>? {
        val cacheWrites = mutableListOf<() -> Unit>()
        val zaaktypeURIs = refreshCache("zaaktypen") {
            fetchZaaktypen(catalogusURI).also { zaaktypen ->
                cacheWrites += { uriToZaakTypeListCache.putUnlessInvalidated(catalogusURI, zaaktypen) }
                cacheWrites += {
                    zaaktypen.forEach { uuidToZaakTypeCache.putUnlessInvalidated(it.url.extractUuid(), it) }
                }
            }
        }?.map { it.url }?.toSet() ?: return null
        refreshCache("statustypen") { refreshStatustypen(zaaktypeURIs, cacheWrites) }
        refreshCache("resultaattypen") { refreshResultaattypen(zaaktypeURIs, cacheWrites) }
        refreshCache("besluittypen") { refreshBesluittypen(zaaktypeURIs, cacheWrites) }
        refreshCache("informatieobjecttypen") {
            refreshInformatieobjecttypen(catalogusURI, zaaktypeURIs, cacheWrites)
        }
        refreshCache("eigenschappen") { refreshEigenschappen(zaaktypeURIs, cacheWrites) }
        refreshCache("roltypen") { refreshRoltypen(zaaktypeURIs, cacheWrites) }
        return cacheWrites
    }

    private fun writeCaches(cacheWrites: List<() -> Unit>) =
        synchronized(cacheLock) {
            cacheWrites.forEach { it() }
            LOG.info("Wrote the refreshed types to the ZTC caches, except the ones invalidated during the refresh")
        }

    private fun <K : Any, V : Any> Cache<K, V>.putUnlessInvalidated(key: K, value: V) {
        if (invalidationsDuringRefresh[this].orEmpty().none { isInvalidated -> isInvalidated(key, value) }) {
            put(key, value)
        }
    }

    private fun <K : Any, V : Any> Cache<K, V>.putAllUnlessInvalidated(entries: Map<K, V>) =
        entries.forEach { (key, value) -> putUnlessInvalidated(key, value) }

    @Suppress("TooGenericExceptionCaught")
    private fun <T> refreshCache(typeName: String, refresh: () -> List<T>): List<T>? =
        try {
            refresh().also { LOG.info { "Fetched ${it.size} $typeName for the ZTC caches" } }
        } catch (runtimeException: RuntimeException) {
            LOG.log(Level.WARNING, runtimeException) { "Failed to refresh the ZTC cache with $typeName" }
            null
        }

    private fun refreshStatustypen(zaaktypeURIs: Set<URI>, cacheWrites: MutableList<() -> Unit>) =
        listAllPages { page -> ztcClient.statustypeList(StatustypeListParameters(page = page)) }
            .groupByZaaktype(zaaktypeURIs) { listOfNotNull(it.zaaktype) }
            .also { cacheWrites += { uriToStatusTypeListCache.putAllUnlessInvalidated(it) } }
            .values.flatten()
            .also { statustypen ->
                cacheWrites += { statustypen.forEach { uuidToStatusTypeCache.putUnlessInvalidated(it.url.extractUuid(), it) } }
            }

    private fun refreshResultaattypen(zaaktypeURIs: Set<URI>, cacheWrites: MutableList<() -> Unit>) =
        listAllPages { page -> ztcClient.resultaattypeList(ResultaattypeListParameters(page = page)) }
            .groupByZaaktype(zaaktypeURIs) { listOfNotNull(it.zaaktype) }
            .also { cacheWrites += { uriToResultaatTypeListCache.putAllUnlessInvalidated(it) } }
            .values.flatten()
            .also { resultaattypen ->
                cacheWrites += { resultaattypen.forEach { uuidToResultaatTypeCache.putUnlessInvalidated(it.url.extractUuid(), it) } }
            }

    /**
     * Lists the besluittypen of all catalogi, because a zaaktype can be linked to a besluittype of another catalogus
     * and [readBesluittypen] does not filter on catalogus either.
     */
    private fun refreshBesluittypen(zaaktypeURIs: Set<URI>, cacheWrites: MutableList<() -> Unit>) = listAllPages { page ->
        ztcClient.besluittypeList(BesluittypeListParameters().apply { this.page = page })
    }.also { besluittypen ->
        val besluittypenByZaaktype = besluittypen.groupByZaaktype(zaaktypeURIs) { it.zaaktypen.orEmpty() }
        cacheWrites += { uriToBesluitTypeListCache.putAllUnlessInvalidated(besluittypenByZaaktype) }
        cacheWrites += { besluittypen.forEach { uuidToBesluitTypeCache.putUnlessInvalidated(it.url.extractUuid(), it) } }
    }

    /**
     * Informatieobjecttypen are linked to zaaktypen through zaaktype-informatieobjecttypen, so both are refreshed
     * together. All statuses are cached, because [readInformatieobjecttype] does not filter on status either.
     */
    private fun refreshInformatieobjecttypen(
        catalogusURI: URI,
        zaaktypeURIs: Set<URI>,
        cacheWrites: MutableList<() -> Unit>
    ): List<InformatieObjectType> {
        val informatieobjecttypen = listAllPages { page ->
            ztcClient.informatieobjecttypeList(
                InformatieobjecttypeListParameters(catalogusURI, page).apply { setStatus(ObjectStatusFilter.ALLES) }
            )
        }
        val zaaktypeInformatieobjecttypen = listAllPages { page ->
            ztcClient.zaaktypeinformatieobjecttypeList(ZaaktypeInformatieobjecttypeListParameters(page = page))
        }.groupByZaaktype(zaaktypeURIs) { listOfNotNull(it.zaaktype) }
        val informatieobjecttypenOfCatalogusUUIDs = informatieobjecttypen.map { it.url.extractUuid() }.toSet()
        val informatieobjecttypenOfOtherCatalogi = zaaktypeInformatieobjecttypen.values.flatten()
            .map { it.informatieobjecttype.extractUuid() }
            .filterNot(informatieobjecttypenOfCatalogusUUIDs::contains)
            .distinct()
            .map(ztcClient::informatieObjectTypeRead)
        val allInformatieobjecttypen = informatieobjecttypen + informatieobjecttypenOfOtherCatalogi
        val informatieobjecttypenByUUID = allInformatieobjecttypen.associateBy { it.url.extractUuid() }
        val informatieobjecttypenByZaaktype = zaaktypeInformatieobjecttypen.mapValues { zaaktypeEntry ->
            zaaktypeEntry.value.map { informatieobjecttypenByUUID.getValue(it.informatieobjecttype.extractUuid()) }
        }
        cacheWrites += {
            allInformatieobjecttypen.forEach { uuidToInformatieObjectTypeCache.putUnlessInvalidated(it.url.extractUuid(), it) }
        }
        cacheWrites += { uriToZaakTypeInformatieObjectTypeListCache.putAllUnlessInvalidated(zaaktypeInformatieobjecttypen) }
        cacheWrites += { uriToInformatieObjectTypeListCache.putAllUnlessInvalidated(informatieobjecttypenByZaaktype) }
        return allInformatieobjecttypen
    }

    private fun refreshEigenschappen(zaaktypeURIs: Set<URI>, cacheWrites: MutableList<() -> Unit>) =
        listAllPages { page ->
            ztcClient.eigenschapList(
                EigenschapListParameters().apply {
                    this.status = EigenschapListParametersStatus.DEFINITIEF
                    this.page = page
                }
            )
        }.groupByZaaktype(zaaktypeURIs) { listOfNotNull(it.zaaktype) }
            .also { cacheWrites += { uriToEigenschappenCache.putAllUnlessInvalidated(it) } }
            .values.flatten()

    private fun refreshRoltypen(zaaktypeURIs: Set<URI>, cacheWrites: MutableList<() -> Unit>) =
        listAllPages { page -> ztcClient.roltypeList(RoltypeListParameters(page = page)) }
            .groupByZaaktype(zaaktypeURIs) { listOfNotNull(it.zaaktype) }
            .also { cacheWrites += { uriToRolTypeListCache.putAllUnlessInvalidated(it) } }
            .values.flatten()
            .also { roltypen -> cacheWrites += { roltypen.forEach { uuidToRolTypeCache.putUnlessInvalidated(it.url.extractUuid(), it) } } }

    private fun <T> listAllPages(listPage: (page: Int) -> Results<T>): List<T> = buildList {
        var page = FIRST_PAGE
        do {
            val results = listPage(page++)
            addAll(results.results())
        } while (results.next() != null)
    }

    /**
     * Groups the items by zaaktype, with an empty list for each zaaktype without items, so that looking up
     * such a zaaktype is also a cache hit. Items of zaaktypen that are not given are left out.
     */
    private fun <T> List<T>.groupByZaaktype(zaaktypeURIs: Set<URI>, zaaktypenOf: (T) -> List<URI>) =
        flatMap { item -> zaaktypenOf(item).map { it to item } }
            .groupBy({ it.first }, { it.second })
            .let { itemsByZaaktype -> zaaktypeURIs.associateWith { itemsByZaaktype[it].orEmpty() } }

    /**
     * Measures the memory that the keys and values of each cache take, including all objects they refer to.
     * This walks through every cached object, so only call it on request.
     */
    fun estimatedMemoryUsage(): CacheMemoryUsage {
        val rootsPerCache = caches.mapValues { (_, cache) -> cache.asMap().flatMap { listOf(it.key, it.value) } }
        return CacheMemoryUsage(
            perCache = rootsPerCache.mapValues { (_, roots) -> roots.totalSize() },
            total = rootsPerCache.values.flatten().totalSize()
        )
    }

    private fun List<Any?>.totalSize() = if (isEmpty()) 0 else GraphLayout.parseInstance(this).totalSize()

    fun clearZaaktypeCache() = clear(Caching.ZTC_ZAAKTYPE, uuidToZaakTypeCache, uriToZaakTypeListCache)

    fun clearStatustypeCache() = clear(Caching.ZTC_STATUSTYPE, uuidToStatusTypeCache, uriToStatusTypeListCache)

    fun clearResultaattypeCache() =
        clear(Caching.ZTC_RESULTAATTYPE, uuidToResultaatTypeCache, uriToResultaatTypeListCache)

    fun clearInformatieobjecttypeCache() =
        clear(Caching.ZTC_INFORMATIEOBJECTTYPE, uuidToInformatieObjectTypeCache, uriToInformatieObjectTypeListCache)

    fun clearZaaktypeInformatieobjecttypeCache() =
        clear(Caching.ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE, uriToZaakTypeInformatieObjectTypeListCache)

    fun clearBesluittypeCache() = clear(Caching.ZTC_BESLUITTYPE, uuidToBesluitTypeCache, uriToBesluitTypeListCache)

    fun clearEigenschapCache() = clear(ZTC_EIGENSCHAP, uriToEigenschappenCache)

    fun clearRoltypeCache() =
        clear(
            Caching.ZTC_ROLTYPE,
            uriOmschrijvingEnumToRolTypeCache,
            uriOmschrijvingGeneriekEnumToRolTypeCache,
            uriToRolTypeListCache,
            rolTypeListCache,
            uuidToRolTypeCache
        )

    private fun clear(cacheName: String, vararg cachesToClear: Cache<*, *>): String {
        cachesToClear.forEach { cache -> invalidateEntries(cache) { _, _ -> true } }
        return cleared(cacheName)
    }

    /**
     * Removes the zaaktype and the types that are cached for it, so that a change to one zaaktype does not empty
     * the caches of all other zaaktypen. The cached zaaktype lists are all removed, because a new zaaktype is not
     * in any list yet, and there is only one list per catalogus.
     */
    fun invalidateZaaktype(zaaktypeURI: URI) {
        val zaaktypeUUID = zaaktypeURI.extractUuid()
        val isZaaktype = { uri: URI? -> uri.refersTo(zaaktypeUUID) }
        invalidate(uuidToZaakTypeCache) { uuid, _ -> uuid == zaaktypeUUID }
        invalidate(uriToZaakTypeListCache) { _, _ -> true }
        invalidate(uriToStatusTypeListCache) { zaaktype, _ -> isZaaktype(zaaktype) }
        invalidate(uuidToStatusTypeCache) { _, statustype -> isZaaktype(statustype.zaaktype) }
        invalidate(uriToResultaatTypeListCache) { zaaktype, _ -> isZaaktype(zaaktype) }
        invalidate(uuidToResultaatTypeCache) { _, resultaattype -> isZaaktype(resultaattype.zaaktype) }
        invalidate(uriToBesluitTypeListCache) { zaaktype, _ -> isZaaktype(zaaktype) }
        invalidate(uriToZaakTypeInformatieObjectTypeListCache) { zaaktype, _ -> isZaaktype(zaaktype) }
        invalidate(uriToInformatieObjectTypeListCache) { zaaktype, _ -> isZaaktype(zaaktype) }
        invalidate(uriToEigenschappenCache) { zaaktype, _ -> isZaaktype(zaaktype) }
        invalidate(uriToRolTypeListCache) { zaaktype, _ -> isZaaktype(zaaktype) }
        invalidate(uuidToRolTypeCache) { _, roltype -> isZaaktype(roltype.zaaktype) }
        invalidate(uriOmschrijvingEnumToRolTypeCache) { key, _ -> key.contains("$zaaktypeUUID") }
        invalidate(uriOmschrijvingGeneriekEnumToRolTypeCache) { key, _ -> key.contains("$zaaktypeUUID") }
        invalidate(rolTypeListCache) { _, _ -> true }
    }

    /**
     * Removes the besluittype and the besluittype lists of the zaaktypen that it is or was linked to.
     * The besluittype is read from the ZTC to find the zaaktypen it is linked to now, unless it was deleted.
     * When it cannot be read, all besluittype lists are removed.
     */
    fun invalidateBesluittype(besluittypeURI: URI, isDeleted: Boolean = false) {
        val besluittypeUUID = besluittypeURI.extractUuid()
        val linkedZaaktypeUUIDs = if (isDeleted) emptySet() else readLinkedZaaktypeUUIDs(besluittypeUUID)
        invalidate(uuidToBesluitTypeCache) { uuid, _ -> uuid == besluittypeUUID }
        invalidate(uriToBesluitTypeListCache) { zaaktype, besluittypen ->
            linkedZaaktypeUUIDs == null ||
                linkedZaaktypeUUIDs.any { zaaktype.refersTo(it) } ||
                besluittypen.any { it.url.refersTo(besluittypeUUID) }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun readLinkedZaaktypeUUIDs(besluittypeUUID: UUID): Set<UUID>? =
        try {
            ztcClient.besluittypeRead(besluittypeUUID).zaaktypen.orEmpty().map { it.extractUuid() }.toSet()
        } catch (runtimeException: RuntimeException) {
            LOG.log(Level.WARNING, runtimeException) {
                "Failed to read besluittype $besluittypeUUID, so all besluittype lists are removed from the cache"
            }
            null
        }

    /**
     * Removes the informatieobjecttype and the informatieobjecttype lists that contain it. A zaaktype is linked to
     * an informatieobjecttype through a zaaktype-informatieobjecttype, which does not change with it.
     */
    fun invalidateInformatieobjecttype(informatieobjecttypeURI: URI) {
        val informatieobjecttypeUUID = informatieobjecttypeURI.extractUuid()
        invalidate(uuidToInformatieObjectTypeCache) { uuid, _ -> uuid == informatieobjecttypeUUID }
        invalidate(uriToInformatieObjectTypeListCache) { _, informatieobjecttypen ->
            informatieobjecttypen.any { it.url.refersTo(informatieobjecttypeUUID) }
        }
    }

    /**
     * Compares the text of the URI, because [extractUuid] throws on a URI that does not end with a UUID, and one
     * such cache key would then stop the invalidation of all others.
     */
    private fun URI?.refersTo(uuid: UUID) = this?.toString()?.contains("$uuid") == true

    @Suppress("UNCHECKED_CAST")
    private fun <K : Any, V> invalidate(cache: Cache<K, V>, isInvalidated: (K, V) -> Boolean) =
        invalidateEntries(cache) { key, value -> isInvalidated(key as K, value as V) }

    /**
     * Also remembers the invalidation while a refresh runs, so that the refresh does not write the entries that
     * match it.
     */
    @Suppress("UNCHECKED_CAST")
    private fun invalidateEntries(cache: Cache<*, *>, isInvalidated: (Any, Any?) -> Boolean) =
        synchronized(cacheLock) {
            if (runningRefreshes > 0) invalidationsDuringRefresh.getOrPut(cache, ::mutableListOf) += isInvalidated
            (cache as Cache<Any, Any?>).run {
                invalidateAll(asMap().filter { isInvalidated(it.key, it.value) }.keys)
            }
        }

    fun clearCacheTime(): String {
        ztcTimeCache.invalidateAll()
        return cleared(Caching.ZTC_CACHE_TIME)
    }

    override fun cacheStatistics(): Map<String, CacheStats> =
        caches.mapValuesTo(mutableMapOf<String, CacheStats>()) { it.value.stats() }

    override fun estimatedCacheSizes(): Map<String, Long> =
        caches.mapValuesTo(mutableMapOf()) { it.value.estimatedSize() }
}
