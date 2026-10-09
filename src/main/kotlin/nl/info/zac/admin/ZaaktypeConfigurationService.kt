/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.zgw.shared.cache.Caching
import nl.info.zac.admin.exception.ZaaktypeConfigurationNotFoundException
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.ZaaktypeDeadlineWarningWindows
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.Optional
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Reads zaaktype configurations, whatever process engine they are bound to.
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class ZaaktypeConfigurationService @Inject constructor(
    private val zaaktypeConfigurationRepository: ZaaktypeConfigurationRepository
) : Caching {
    companion object {
        const val INADMISSIBLE_TERMINATION_ID = "ZAAK_NIET_ONTVANKELIJK"
        const val INADMISSIBLE_TERMINATION_REASON = "Zaak is niet ontvankelijk"

        private val LOG = Logger.getLogger(ZaaktypeConfigurationService::class.java.name)
        private const val MAX_CACHE_SIZE = 20L
        private const val EXPIRATION_TIME_HOURS = 1L

        private val caches = mutableMapOf<String, Cache<*, *>>()

        private fun <K : Any, V : Any> createCache(name: String): Cache<K, V> =
            Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterAccess(EXPIRATION_TIME_HOURS, TimeUnit.HOURS)
                .recordStats()
                .removalListener<K, V> { key, _, cause ->
                    LOG.fine("Removing key: $key in cache $name because of: $cause")
                }
                .build<K, V>()
                .also { caches[name] = it }
    }

    private val uuidToConfigurationCache: Cache<UUID, Optional<ZaaktypeConfiguration>> =
        createCache("UUID -> ZaaktypeConfiguration")

    private val deadlineWarningWindowsCache: Cache<String, List<ZaaktypeDeadlineWarningWindows>> =
        createCache("List<ZaaktypeDeadlineWarningWindows>")

    fun findConfiguration(zaaktypeUuid: UUID): ZaaktypeConfiguration? =
        uuidToConfigurationCache.get(zaaktypeUuid) {
            Optional.ofNullable(zaaktypeConfigurationRepository.findByZaaktypeUuid(it))
        }.orElse(null)

    fun readConfiguration(zaaktypeUuid: UUID): ZaaktypeConfiguration =
        findConfiguration(zaaktypeUuid) ?: throw ZaaktypeConfigurationNotFoundException(
            "No zaaktype configuration found for zaaktype with UUID '$zaaktypeUuid'"
        )

    fun findCurrentConfiguration(zaaktypeOmschrijving: String): ZaaktypeConfiguration? =
        zaaktypeConfigurationRepository.findCurrentByZaaktypeOmschrijving(zaaktypeOmschrijving)

    /**
     * Returns the current configurations whose productaanvraagtype is the given one, the most recently created first.
     * More than one indicates data stored before the productaanvraagtype check covered both process engines.
     */
    fun listCurrentConfigurationsByProductaanvraagtype(productaanvraagtype: String): List<ZaaktypeConfiguration> =
        zaaktypeConfigurationRepository.listCurrentByProductaanvraagtype(productaanvraagtype)

    fun listConfigurationsBoundTo(processEngine: ProcessEngine): List<ZaaktypeConfiguration> =
        zaaktypeConfigurationRepository.listBoundTo(processEngine)

    fun listDefinitionKeysBoundTo(processEngine: ProcessEngine): List<String> =
        zaaktypeConfigurationRepository.listDistinctDefinitionKeys(processEngine)

    fun listDeadlineWarningWindows(): List<ZaaktypeDeadlineWarningWindows> =
        deadlineWarningWindowsCache.get(Caching.ZAC_ZAAKTYPECMMNCONFIGURATION) {
            zaaktypeConfigurationRepository.listDeadlineWarningWindows()
        }

    fun listZaakbeeindigRedenen(): List<ZaakbeeindigReden> = zaaktypeConfigurationRepository.listZaakbeeindigRedenen()

    fun isSmartDocumentsEnabled(zaaktypeUuid: UUID): Boolean = findConfiguration(zaaktypeUuid)?.isSmartDocumentsEnabled ?: false

    fun evict(zaaktypeUuid: UUID) {
        uuidToConfigurationCache.invalidate(zaaktypeUuid)
        deadlineWarningWindowsCache.invalidateAll()
    }

    fun clearManagedCache(): String {
        uuidToConfigurationCache.invalidateAll()
        return cleared(Caching.ZAC_ZAAKTYPECMMNCONFIGURATION_MANAGED)
    }

    fun clearListCache(): String {
        deadlineWarningWindowsCache.invalidateAll()
        return cleared(Caching.ZAC_ZAAKTYPECMMNCONFIGURATION)
    }

    override fun cacheStatistics(): Map<String, CacheStats> = caches.mapValues { it.value.stats() }

    override fun estimatedCacheSizes(): Map<String, Long> = caches.mapValues { it.value.estimatedSize() }
}
