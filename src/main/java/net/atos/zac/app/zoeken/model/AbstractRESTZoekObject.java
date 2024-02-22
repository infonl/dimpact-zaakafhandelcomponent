/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model;

import net.atos.zac.zoeken.model.index.ZoekObjectType;

public abstract class AbstractRESTZoekObject {

  public String id;

  public ZoekObjectType type;

  public String identificatie;
}
