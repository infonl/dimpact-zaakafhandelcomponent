/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import net.atos.client.zgw.shared.cache.Caching;
import net.atos.zac.admin.model.ZaakafhandelParameters;

@ApplicationScoped
public class ZaakafhandelParameterService implements Caching {
    private static final Logger LOG = Logger.getLogger(ZaakafhandelParameterService.class.getName());
    private static final int MAX_CACHE_SIZE = 100;

    @Inject
    private ZaakafhandelParameterBeheerService beheerService;

    private static final Map<String, Cache<?, ?>> CACHES = new HashMap<>();

    private <K, V> Cache<K, V> createCache(String name) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .recordStats()
                .removalListener(
                        (K key, V value, RemovalCause cause) -> LOG.info("Removing key: %s because of: %s".formatted(key, cause))
                )
                .build();

        CACHES.put(name, cache);
        return cache;
    }

    private final Cache<UUID, ZaakafhandelParameters> uuidToZaakafhandelParametersCache = createCache("UUID -> ZaakafhandelParameters");
    private final Cache<String, List<ZaakafhandelParameters>> stringToZaakafhandelParametersListCache = createCache(
            "List<ZaakafhandelParameters>");

    public ZaakafhandelParameters readZaakafhandelParameters(final UUID zaaktypeUUID) {
        return uuidToZaakafhandelParametersCache.get(
                zaaktypeUUID,
                uuid -> beheerService.readZaakafhandelParameters(zaaktypeUUID)
        );
    }

    public List<ZaakafhandelParameters> listZaakafhandelParameters() {
        return stringToZaakafhandelParametersListCache.get(
                ZAC_ZAAKAFHANDELPARAMETERS,
                s -> beheerService.listZaakafhandelParameters()
        );
    }

    public void cacheRemoveZaakafhandelParameters(final UUID zaaktypeUUID) {
        uuidToZaakafhandelParametersCache.invalidate(zaaktypeUUID);
    }

    public String clearManagedCache() {
        uuidToZaakafhandelParametersCache.invalidateAll();
        return cleared(Caching.ZAC_ZAAKAFHANDELPARAMETERS_MANAGED);
    }

    public String clearListCache() {
        stringToZaakafhandelParametersListCache.invalidateAll();
        return cleared(Caching.ZAC_ZAAKAFHANDELPARAMETERS);
    }

    @Override
    public Map<String, CacheStats> cacheStatistics() {
        return CACHES.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, cache -> cache.getValue().stats()));
    }
}
