/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.model;

import java.time.LocalDate;
import java.util.UUID;

import net.atos.client.zgw.shared.model.Vertrouwelijkheidaanduiding;

public class RESTZaaktypeOverzicht {

    public UUID uuid;

    public String identificatie;

    public String doel;

    public String omschrijving;

    public boolean servicenorm;

    public LocalDate versiedatum;

    public LocalDate beginGeldigheid;

    public LocalDate eindeGeldigheid;

    public Vertrouwelijkheidaanduiding vertrouwelijkheidaanduiding;

    public boolean nuGeldig;


    public RESTZaaktypeOverzicht() {
    }
}
