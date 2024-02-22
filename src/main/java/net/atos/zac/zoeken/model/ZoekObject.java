/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model;

import net.atos.zac.zoeken.model.index.ZoekObjectType;

public interface ZoekObject {

  String IS_TOEGEKEND_FIELD = "isToegekend";

  String getObjectId();

  ZoekObjectType getType();
}
