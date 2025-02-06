/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
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
import net.atos.zac.admin.model.ZaakafhandelParameters;

@ApplicationScoped
public class ZaakafhandelParameterService implements Caching {
    private static final Logger LOG = Logger.getLogger(ZaakafhandelParameterService.class.getName());
    private static final int MAX_CACHE_SIZE = 20;
    private static final int EXPIRATION_TIME_HOURS = 1;

    /**
     * Hardcoded zaakbeeindigreden that we don't manage via ZaakafhandelParameters
     */
    public static final String INADMISSIBLE_TERMINATION_ID = "ZNOR";
    public static final String INADMISSIBLE_TERMINATION_REASON = "Zaak is niet ontvankelijk";

    @Inject
    private ZaakafhandelParameterBeheerService zaakafhandelParameterBeheerService;

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

    private final Cache<UUID, ZaakafhandelParameters> uuidToZaakafhandelParametersCache = createCache("UUID -> ZaakafhandelParameters");
    private final Cache<String, List<ZaakafhandelParameters>> stringToZaakafhandelParametersListCache = createCache(
            "List<ZaakafhandelParameters>");

    public ZaakafhandelParameters readZaakafhandelParameters(final UUID zaaktypeUUID) {
        return uuidToZaakafhandelParametersCache.get(
                zaaktypeUUID,
                uuid -> zaakafhandelParameterBeheerService.readZaakafhandelParameters(zaaktypeUUID)
        );
    }

    public List<ZaakafhandelParameters> listZaakafhandelParameters() {
        return stringToZaakafhandelParametersListCache.get(
                ZAC_ZAAKAFHANDELPARAMETERS,
                s -> zaakafhandelParameterBeheerService.listZaakafhandelParameters()
        );
    }

    public boolean isSmartDocumentsEnabled(final UUID zaaktypeUUID) {
        return readZaakafhandelParameters(zaaktypeUUID).isSmartDocumentsIngeschakeld();
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

    @Override
    public Map<String, Long> cacheSizes() {
        return CACHES.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, cache -> cache.getValue().estimatedSize()));
    }
}
