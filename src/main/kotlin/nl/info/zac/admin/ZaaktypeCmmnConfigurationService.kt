/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.zgw.shared.cache.Caching
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

@ApplicationScoped
@AllOpen
@NoArgConstructor
class ZaaktypeCmmnConfigurationService @Inject constructor(
    private val zaaktypeCmmnConfigurationBeheerService: ZaaktypeCmmnConfigurationBeheerService
) : Caching {

    companion object {
        const val INADMISSIBLE_TERMINATION_ID = "ZAAK_NIET_ONTVANKELIJK"
        const val INADMISSIBLE_TERMINATION_REASON = "Zaak is niet ontvankelijk"

        private val LOG = Logger.getLogger(ZaaktypeCmmnConfigurationService::class.java.name)
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

    private val uuidToConfigCache: Cache<UUID, ZaaktypeCmmnConfiguration> =
        createCache("UUID -> ZaaktypeCmmnConfiguration")

    private val listCache: Cache<String, List<ZaaktypeCmmnConfiguration>> =
        createCache("List<ZaaktypeCmmnConfiguration>")

    fun readZaaktypeCmmnConfiguration(zaaktypeUUID: UUID): ZaaktypeCmmnConfiguration =
        uuidToConfigCache.get(zaaktypeUUID) {
            zaaktypeCmmnConfigurationBeheerService.fetchZaaktypeCmmnConfiguration(it)
        }

    fun listZaaktypeCmmnConfiguration(): List<ZaaktypeCmmnConfiguration> =
        listCache.get(Caching.ZAC_ZAAKTYPECMMNCONFIGURATION) {
            zaaktypeCmmnConfigurationBeheerService.listZaaktypeCmmnConfiguration()
        }

    fun cacheRemoveZaaktypeCmmnConfiguration(zaaktypeUUID: UUID) {
        uuidToConfigCache.invalidate(zaaktypeUUID)
    }

    fun clearManagedCache(): String {
        uuidToConfigCache.invalidateAll()
        return cleared(Caching.ZAC_ZAAKTYPECMMNCONFIGURATION_MANAGED)
    }

    fun clearListCache(): String {
        listCache.invalidateAll()
        return cleared(Caching.ZAC_ZAAKTYPECMMNCONFIGURATION)
    }

    override fun cacheStatistics(): Map<String, CacheStats> =
        caches.mapValues { it.value.stats() }

    override fun estimatedCacheSizes(): Map<String, Long> =
        caches.mapValues { it.value.estimatedSize() }
}
