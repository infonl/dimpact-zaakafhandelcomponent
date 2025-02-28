/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.model;

import java.util.UUID;

import jakarta.ws.rs.FormParam;

import net.atos.zac.app.zaak.model.RelatieType;

public class RestGekoppeldeZaakEnkelvoudigInformatieObject extends RestEnkelvoudigInformatieobject {

    @FormParam("relatieType")
    public RelatieType relatieType;

    @FormParam("zaakIdentificatie")
    public String zaakIdentificatie;

    @FormParam("zaakUUID")
    public UUID zaakUUID;
}
