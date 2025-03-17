/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.model.audit.documenten;

import net.atos.client.zgw.shared.model.ObjectType;
import net.atos.client.zgw.shared.model.audit.AuditWijziging;
import nl.info.client.zgw.drc.model.generated.Gebruiksrechten;

public class GebuiksrechtenWijziging extends AuditWijziging<Gebruiksrechten> {

    @Override
    public ObjectType getObjectType() {
        return ObjectType.GEBRUIKSRECHTEN;
    }
}
