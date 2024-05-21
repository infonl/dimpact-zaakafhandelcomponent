/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreatie.model;

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType;

public class DocumentCreatieGegevens {
    private final Zaak zaak;
    private final String taskId;
    private final EnkelvoudigInformatieObject.StatusEnum informatieobjectStatus = EnkelvoudigInformatieObject.StatusEnum.TER_VASTSTELLING;
    private final InformatieObjectType informatieobjecttype;

    public DocumentCreatieGegevens(
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

    public EnkelvoudigInformatieObject.StatusEnum getInformatieobjectStatus() {
        return informatieobjectStatus;
    }

    public InformatieObjectType getInformatieobjecttype() {
        return informatieobjecttype;
    }

    public String getTaskId() {
        return taskId;
    }
}
