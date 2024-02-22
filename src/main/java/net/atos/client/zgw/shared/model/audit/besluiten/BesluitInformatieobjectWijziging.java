/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.model.audit.besluiten;

import net.atos.client.zgw.brc.model.generated.BesluitInformatieObject;
import net.atos.client.zgw.shared.model.ObjectType;
import net.atos.client.zgw.shared.model.audit.AuditWijziging;

public class BesluitInformatieobjectWijziging extends AuditWijziging<BesluitInformatieObject> {

    @Override
    public ObjectType getObjectType() {
        return ObjectType.BESLUIT_INFORMATIEOBJECT;
    }
}
