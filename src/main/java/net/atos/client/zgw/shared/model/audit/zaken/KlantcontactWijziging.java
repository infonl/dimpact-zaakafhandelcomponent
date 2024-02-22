/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared.model.audit.zaken;

import net.atos.client.zgw.shared.model.ObjectType;
import net.atos.client.zgw.shared.model.audit.AuditWijziging;
import net.atos.client.zgw.zrc.model.Klantcontact;

public class KlantcontactWijziging extends AuditWijziging<Klantcontact> {

  @Override
  public ObjectType getObjectType() {
    return ObjectType.KLANTCONTACT;
  }
}
