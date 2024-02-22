/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.app.admin.model.RESTZaakafhandelParameters;

public class RESTZaaktype {

  @NotNull public UUID uuid;

  public String identificatie;

  public String doel;

  public String omschrijving;

  public String referentieproces;

  public boolean servicenorm;

  public LocalDate versiedatum;

  public LocalDate beginGeldigheid;

  public LocalDate eindeGeldigheid;

  public ZaakType.VertrouwelijkheidaanduidingEnum vertrouwelijkheidaanduiding;

  public boolean nuGeldig;

  public boolean opschortingMogelijk;

  public boolean verlengingMogelijk;

  public Integer verlengingstermijn;

  public List<RESTZaaktypeRelatie> zaaktypeRelaties;

  public List<UUID> informatieobjecttypes;

  public RESTZaakafhandelParameters zaakafhandelparameters;
}
