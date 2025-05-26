/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

import net.atos.zac.event.EventingService;
import net.atos.zac.flowable.task.TaakVariabelenService;
import nl.info.client.zgw.shared.ZGWApiService;
import nl.info.client.zgw.zrc.ZrcClientService;
import nl.info.client.zgw.ztc.ZtcClientService;
import nl.info.zac.identity.IdentityService;
import nl.info.zac.search.IndexingService;

/**
 * A Helper for Flowable CMMN and BPMN LifecycleListener's, Interceptors etc. in order to get access to CDI resources.
 */
@ApplicationScoped
public class FlowableHelper {

    @Inject
    private ZaakVariabelenService zaakVariabelenService;

    @Inject
    private TaakVariabelenService taakVariabelenService;

    @Inject
    private ZGWApiService zgwApiService;

    @Inject
    private ZtcClientService ztcClientService;

    @Inject
    private ZrcClientService zrcClientService;

    @Inject
    private EventingService eventingService;

    @Inject
    private IndexingService indexingService;

    @Inject
    private IdentityService identityService;

    public static FlowableHelper getInstance() {
        return CDI.current().select(FlowableHelper.class).get();
    }

    public ZGWApiService getZgwApiService() {
        return zgwApiService;
    }

    public ZrcClientService getZrcClientService() {
        return zrcClientService;
    }

    public ZtcClientService getZtcClientService() {
        return ztcClientService;
    }

    public EventingService getEventingService() {
        return eventingService;
    }

    public IdentityService getIdentityService() {
        return identityService;
    }

    public ZaakVariabelenService getZaakVariabelenService() {
        return zaakVariabelenService;
    }

    public TaakVariabelenService getTaakVariabelenService() {
        return taakVariabelenService;
    }

    public IndexingService getIndexeerService() {
        return indexingService;
    }
}
