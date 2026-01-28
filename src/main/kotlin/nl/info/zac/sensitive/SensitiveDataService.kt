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

/**
 * Service to store sensitive data in memory.
 *
 * Implemented as BiMap of two Caffeine caches to store key and sensitive data.
 * At most [STORAGE_SIZE] data entries are kept in memory for [EXPIRATION_TIME_HOURS].
 */
@ApplicationScoped
@AllOpen
@NoArgConstructor
class SensitiveDataService {
    companion object {
        const val STORAGE_SIZE = 10000L
        const val EXPIRATION_TIME_HOURS = 12L

        private val LOG = Logger.getLogger(SensitiveDataService::class.java.name)

        val dataToUuidStorage: Cache<String, UUID> = Caffeine.newBuilder()
            .maximumSize(STORAGE_SIZE)
            .build()

        val uuidToDataStorage: Cache<UUID, String> = Caffeine.newBuilder()
            .maximumSize(STORAGE_SIZE)
            .expireAfterAccess(EXPIRATION_TIME_HOURS, TimeUnit.HOURS)
            .removalListener { key: UUID?, value: String?, cause ->
                LOG.fine { "Removing sensitive storage data with key $key, because of: $cause" }
                value?.let { dataToUuidStorage.invalidate(value) }
            }.build()
    }

    /**
     * Replaces a sensitive data with a UUID key and stores the key for later retrieval.
     *
     * Multiple calls with the same data will return the same UUID key until the data is evicted. Then a new UUID key is generated.
     * Parallel calls are thread-safe and will not result in duplicate UUID keys.
     *
     * @param data the sensitive data to store
     * @return the UUID key for the stored sensitive data
     */
    fun put(data: String): UUID =
        dataToUuidStorage.get(data) {
            UUID.randomUUID().also {
                uuidToDataStorage.put(it, data)
            }
        }

    /**
     * Retrieves the sensitive data for a given UUID key.
     *
     * @param key the UUID key for the sensitive data
     * @return the sensitive data or null if not found
     */
    fun get(key: UUID): String? = uuidToDataStorage.getIfPresent(key)

    /**
     * Clears the storage of sensitive data.
     *
     * @return a message indicating the operation was successful
     */
    fun clearStorage(): String {
        dataToUuidStorage.invalidateAll()
        uuidToDataStorage.invalidateAll()
        LOG.info("Sensitive storage cleared.")
        return "Storage cleared."
    }
}
