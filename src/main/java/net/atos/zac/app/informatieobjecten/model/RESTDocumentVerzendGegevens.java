/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class RESTDocumentVerzendGegevens {
  public UUID zaakUuid;

  public LocalDate verzenddatum;

  public List<UUID> informatieobjecten;

  public String toelichting;
}
