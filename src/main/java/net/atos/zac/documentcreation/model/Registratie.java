/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreation.model;

import java.net.URI;
import java.time.LocalDate;

import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.client.zgw.drc.model.generated.StatusEnum;


public class Registratie {

    @JsonbProperty("zaak")
    public URI zaak;

    @JsonbProperty("informatieobjectStatus")
    public StatusEnum informatieObjectStatus;

    @JsonbProperty("informatieobjecttype")
    public URI informatieObjectType;

    @JsonbProperty("bronorganisatie")
    public String bronOrganisatie;

    @JsonbProperty("creatiedatum")
    public LocalDate creatieDatum;

    @JsonbProperty("auditToelichting")
    public String auditToelichting;
}
