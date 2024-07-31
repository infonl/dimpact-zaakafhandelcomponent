/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreation.model;

import net.atos.client.zgw.drc.model.generated.StatusEnum;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType;

public class DocumentCreationData {
    private final Zaak zaak;
    private final String taskId;
    private final StatusEnum informatieobjectStatus = StatusEnum.TER_VASTSTELLING;
    private final InformatieObjectType informatieobjecttype;

    public DocumentCreationData(
            final Zaak zaak,
            final String taskId,
            final InformatieObjectType informatieobjecttype
    ) {
        this.zaak = zaak;
        this.taskId = taskId;
        this.informatieobjecttype = informatieobjecttype;
    }

    public Zaak getZaak() {
        return zaak;
    }

    public StatusEnum getInformatieobjectStatus() {
        return informatieobjectStatus;
    }

    public InformatieObjectType getInformatieobjecttype() {
        return informatieobjecttype;
    }

    public String getTaskId() {
        return taskId;
    }
}
