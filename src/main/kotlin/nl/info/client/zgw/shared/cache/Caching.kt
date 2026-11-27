/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.cache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import nl.info.client.zgw.shared.log.CacheCleared
import nl.info.client.zgw.shared.log.CacheEntryRemoved
import nl.info.zac.log.ZacLogger

/**
 * Never call methods with caching annotations from within the service (or it will not work).
 * Do not introduce caches with keys other than URI and UUID.
 * Use Optional for caches that need to hold nulls (Infinispan does not cache nulls).
 */
interface Caching {
    companion object {
        private val LOG = ZacLogger.getLogger(Caching::class)

        const val ZTC_CACHE_TIME = "ztc-cache-datetime"
        const val ZTC_RESULTAATTYPE = "ztc-resultaattype"
        const val ZTC_BESLUITTYPE = "ztc-besluittype"
        const val ZTC_STATUSTYPE = "ztc-statustype"
        const val ZTC_INFORMATIEOBJECTTYPE = "ztc-informatieobjecttype"
        const val ZTC_ZAAKTYPE = "ztc-zaaktype"
        const val ZTC_ROLTYPE = "ztc-roltype"
        const val ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE = "ztc-zaaktypeinformatieobjecttype"
        const val ZAC_ZAAKTYPECMMNCONFIGURATION_MANAGED = "zac-zaaktypecmmnconfiguration-read"
        const val ZAC_ZAAKTYPECMMNCONFIGURATION = "zac-zaaktypecmmnconfiguration-list"
    }

    fun cacheStatistics(): Map<String, CacheStats>

    fun estimatedCacheSizes(): Map<String, Long>

    fun cleared(cache: String): String {
        val message = "$cache cache cleared"
        LOG.info(CacheCleared(cacheName = cache))
        return message
    }

    fun <K> removed(cache: String, key: K) {
        LOG.fine (CacheEntryRemoved(cacheName = cache, key = key.toString()))
    }
}
