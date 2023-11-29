/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreatie.model;

import net.atos.client.zgw.drc.model.InformatieobjectStatus;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.ztc.model.Informatieobjecttype;

public class DocumentCreatieGegevens {

    private final Zaak zaak;

    private String taskId;

    private InformatieobjectStatus informatieobjectStatus = InformatieobjectStatus.TER_VASTSTELLING;

    private Informatieobjecttype informatieobjecttype;

    public DocumentCreatieGegevens(
            final Zaak zaak,
            final String taskId,
            final Informatieobjecttype informatieobjecttype
    ) {
        this.zaak = zaak;
        this.taskId = taskId;
        this.informatieobjecttype = informatieobjecttype;
    }

    public Zaak getZaak() {
        return zaak;
    }

    public InformatieobjectStatus getInformatieobjectStatus() {
        return informatieobjectStatus;
    }

    public Informatieobjecttype getInformatieobjecttype() { return informatieobjecttype; }

    public String getTaskId() {
        return taskId;
    }
}
