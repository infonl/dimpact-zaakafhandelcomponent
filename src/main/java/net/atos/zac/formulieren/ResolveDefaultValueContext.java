/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.formulieren;

import java.util.Map;

import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.zac.app.task.model.RestTask;
import net.atos.zac.flowable.ZaakVariabelenService;

public class ResolveDefaultValueContext {

    private final RestTask task;

    private final ZrcClientService zrcClientService;

    private final ZaakVariabelenService zaakVariabelenService;

    private Zaak zaak;

    private Map<String, Object> zaakData;

    public ResolveDefaultValueContext(
            final RestTask task,
            final ZrcClientService zrcClientService,
            final ZaakVariabelenService zaakVariabelenService
    ) {
        this.task = task;
        this.zrcClientService = zrcClientService;
        this.zaakVariabelenService = zaakVariabelenService;
    }

    public RestTask getTask() {
        return task;
    }

    public Zaak getZaak() {
        if (zaak == null) {
            zaak = zrcClientService.readZaak(task.getZaakUuid());
        }
        return zaak;
    }

    public Map<String, Object> getZaakData() {
        if (zaakData == null) {
            zaakData = zaakVariabelenService.readProcessZaakdata(task.getZaakUuid());
        }
        return zaakData;
    }
}
