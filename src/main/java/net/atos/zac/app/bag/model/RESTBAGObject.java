/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag.model;

import java.net.URI;

public abstract class RESTBAGObject {

  public URI url;

  public String identificatie;

  public boolean geconstateerd;

  public abstract BAGObjectType getBagObjectType();

  public abstract String getOmschrijving();
}
