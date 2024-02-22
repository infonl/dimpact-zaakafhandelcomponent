/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zaaksturing;

import static net.atos.client.zgw.shared.cache.Caching.ZAC_ZAAKAFHANDELPARAMETERS;
import static net.atos.client.zgw.shared.cache.Caching.ZAC_ZAAKAFHANDELPARAMETERS_MANAGED;

import java.util.List;
import java.util.UUID;

import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import net.atos.zac.zaaksturing.model.ZaakafhandelParameters;

@ApplicationScoped
public class ZaakafhandelParameterService {

    @Inject private ZaakafhandelParameterBeheerService beheerService;

    @CacheResult(cacheName = ZAC_ZAAKAFHANDELPARAMETERS_MANAGED)
    public ZaakafhandelParameters readZaakafhandelParameters(final UUID zaaktypeUUID) {
        return beheerService.readZaakafhandelParameters(zaaktypeUUID);
    }

    @CacheResult(cacheName = ZAC_ZAAKAFHANDELPARAMETERS)
    public List<ZaakafhandelParameters> listZaakafhandelParameters() {
        return beheerService.listZaakafhandelParameters();
    }

    @CacheRemove(cacheName = ZAC_ZAAKAFHANDELPARAMETERS_MANAGED)
    public void cacheRemoveZaakafhandelParameters(final UUID zaaktypeUUID) {}
}
