/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import net.atos.client.zgw.shared.cache.Caching;
import net.atos.zac.admin.model.ZaaktypeCmmnConfiguration;
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService;

@ApplicationScoped
public class ZaaktypeCmmnConfigurationService implements Caching {
    private static final Logger LOG = Logger.getLogger(ZaaktypeCmmnConfigurationService.class.getName());
    private static final int MAX_CACHE_SIZE = 20;
    private static final int EXPIRATION_TIME_HOURS = 1;

    /**
     * Hardcoded zaakbeeindigreden that we don't manage via ZaaktypeCmmnConfiguration
     */
    public static final String INADMISSIBLE_TERMINATION_ID = "ZAAK_NIET_ONTVANKELIJK";
    public static final String INADMISSIBLE_TERMINATION_REASON = "Zaak is niet ontvankelijk";

    @Inject
    private ZaaktypeCmmnConfigurationBeheerService zaaktypeCmmnConfigurationBeheerService;

    private static final Map<String, Cache<?, ?>> CACHES = new HashMap<>();

    private <K, V> Cache<K, V> createCache(String name) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterAccess(EXPIRATION_TIME_HOURS, TimeUnit.HOURS)
                .recordStats()
                .removalListener(
                        (K key, V value, RemovalCause cause) -> LOG.fine("Removing key: %s in cache %s because of: %s".formatted(key, name,
                                cause))
                )
                .build();

        CACHES.put(name, cache);
        return cache;
    }

    private final Cache<UUID, ZaaktypeCmmnConfiguration> uuidToZaaktypeCmmnConfigurationCache = createCache(
            "UUID -> ZaaktypeCmmnConfiguration");
    private final Cache<String, List<ZaaktypeCmmnConfiguration>> stringToZaaktypeCmmnConfigurationListCache = createCache(
            "List<ZaaktypeCmmnConfiguration>");

    public ZaaktypeCmmnConfiguration readZaaktypeCmmnConfiguration(final UUID zaaktypeUUID) {
        return uuidToZaaktypeCmmnConfigurationCache.get(
                zaaktypeUUID,
                uuid -> zaaktypeCmmnConfigurationBeheerService.fetchZaaktypeCmmnConfiguration(zaaktypeUUID)
        );
    }

    public List<ZaaktypeCmmnConfiguration> listZaaktypeCmmnConfiguration() {
        return stringToZaaktypeCmmnConfigurationListCache.get(
                ZAC_ZAAKTYPECMMNCONFIGURATION,
                s -> zaaktypeCmmnConfigurationBeheerService.listZaaktypeCmmnConfiguration()
        );
    }

    public boolean isSmartDocumentsEnabled(final UUID zaaktypeUUID) {
        return readZaaktypeCmmnConfiguration(zaaktypeUUID).isSmartDocumentsIngeschakeld();
    }

    public void cacheRemoveZaaktypeCmmnConfiguration(final UUID zaaktypeUUID) {
        uuidToZaaktypeCmmnConfigurationCache.invalidate(zaaktypeUUID);
    }

    public String clearManagedCache() {
        uuidToZaaktypeCmmnConfigurationCache.invalidateAll();
        return cleared(Caching.ZAC_ZAAKTYPECMMNCONFIGURATION_MANAGED);
    }

    public String clearListCache() {
        stringToZaaktypeCmmnConfigurationListCache.invalidateAll();
        return cleared(Caching.ZAC_ZAAKTYPECMMNCONFIGURATION);
    }

    @Override
    public Map<String, CacheStats> cacheStatistics() {
        return CACHES.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, cache -> cache.getValue().stats()));
    }

    @Override
    public Map<String, Long> estimatedCacheSizes() {
        return CACHES.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, cache -> cache.getValue().estimatedSize()));
    }
}
