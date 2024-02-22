/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model;

import java.time.LocalDate;
import java.util.UUID;

import net.atos.client.zgw.ztc.model.generated.ZaakType;

public class RESTZaaktypeOverzicht {

    public UUID uuid;

    public String identificatie;

    public String doel;

    public String omschrijving;

    public boolean servicenorm;

    public LocalDate versiedatum;

    public LocalDate beginGeldigheid;

    public LocalDate eindeGeldigheid;

    public ZaakType.VertrouwelijkheidaanduidingEnum vertrouwelijkheidaanduiding;

    public boolean nuGeldig;

    public RESTZaaktypeOverzicht() {}
}
