/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.sensitive

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.enterprise.context.ApplicationScoped
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

@ApplicationScoped
@AllOpen
@NoArgConstructor
class SensitiveDataService {
    companion object {
        const val STORAGE_SIZE = 10000L
        const val EXPIRATION_TIME_HOURS = 12L

        private val LOG = Logger.getLogger(SensitiveDataService::class.java.name)

        val storage: Cache<UUID, String?> = Caffeine.newBuilder()
            .maximumSize(STORAGE_SIZE)
            .expireAfterAccess(EXPIRATION_TIME_HOURS, TimeUnit.HOURS)
            .recordStats()
            .removalListener { key: UUID?, _: String?, cause ->
                LOG.fine("Removing sensitive storage data with key $key, because of: $cause")
            }.build()
    }

    fun put(data: String): UUID =
        UUID.randomUUID().also {
            storage.put(it, data)
        }

    fun get(key: UUID): String? = storage.getIfPresent(key)
}
