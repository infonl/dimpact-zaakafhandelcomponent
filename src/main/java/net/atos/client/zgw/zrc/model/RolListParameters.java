/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import java.net.URI;

import jakarta.ws.rs.QueryParam;

import net.atos.client.zgw.shared.model.AbstractListParameters;
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum;

/**
 *
 */
public class RolListParameters extends AbstractListParameters {

    /**
     * URL-referentie naar de ZAAK.
     */
    @QueryParam("zaak")
    private URI zaak;

    /**
     * Type van de betrokkene
     */
    @QueryParam("betrokkeneType")
    private String betrokkeneType;

    /**
     * URL-referentie naar een roltype binnen het ZAAKTYPE van de ZAAK.
     */
    @QueryParam("roltype")
    private URI roltype;

    public RolListParameters(final URI zaak) {
        this.zaak = zaak;
    }

    public RolListParameters(final URI zaak, final URI roltype) {
        this.zaak = zaak;
        this.roltype = roltype;
    }

    public RolListParameters(final URI zaak, final URI roltype, final BetrokkeneTypeEnum betrokkeneType) {
        this.zaak = zaak;
        this.betrokkeneType = betrokkeneType.toString();
        this.roltype = roltype;
    }

    public URI getZaak() {
        return zaak;
    }

    public void setZaak(final URI zaak) {
        this.zaak = zaak;
    }

    public String getBetrokkeneType() {
        return betrokkeneType;
    }

    public void setBetrokkeneType(final BetrokkeneTypeEnum betrokkeneType) {
        this.betrokkeneType = betrokkeneType.toString();
    }

    public URI getRoltype() {
        return roltype;
    }

    public void setRoltype(final URI roltype) {
        this.roltype = roltype;
    }
}
