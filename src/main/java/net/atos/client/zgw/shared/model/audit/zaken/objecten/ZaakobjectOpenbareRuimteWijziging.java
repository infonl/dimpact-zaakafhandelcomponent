/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared.model.audit.zaken.objecten;

import net.atos.client.zgw.shared.model.ObjectType;
import net.atos.client.zgw.shared.model.audit.AuditWijziging;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte;

public class ZaakobjectOpenbareRuimteWijziging extends AuditWijziging<ZaakobjectOpenbareRuimte> {

  @Override
  public ObjectType getObjectType() {
    return ObjectType.ZAAKOBJECT;
  }
}
