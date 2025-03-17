/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import java.net.URI;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import nl.info.client.zgw.ztc.model.generated.RolType;

public class RolOrganisatorischeEenheid extends Rol<OrganisatorischeEenheid> {

    public RolOrganisatorischeEenheid() {
    }

    public RolOrganisatorischeEenheid(
            final URI zaak,
            final RolType roltype,
            final String roltoelichting,
            final OrganisatorischeEenheid organisatorischeEenheid
    ) {
        super(zaak, roltype, BetrokkeneType.ORGANISATORISCHE_EENHEID, organisatorischeEenheid, roltoelichting);
    }

    @Override
    protected boolean equalBetrokkeneIdentificatie(final OrganisatorischeEenheid identificatie) {
        final OrganisatorischeEenheid betrokkeneIdentificatie = getBetrokkeneIdentificatie();
        if (betrokkeneIdentificatie == identificatie) {
            return true;
        }
        if (identificatie == null) {
            return false;
        }
        return Objects.equals(betrokkeneIdentificatie.getIdentificatie(), identificatie.getIdentificatie());
    }

    @Override
    public String getNaam() {
        if (getBetrokkeneIdentificatie() == null) {
            return null;
        }
        return StringUtils.isNotEmpty(getBetrokkeneIdentificatie().getNaam()) ? getBetrokkeneIdentificatie().getNaam() :
                getIdentificatienummer();
    }

    @Override
    public String getIdentificatienummer() {
        if (getBetrokkeneIdentificatie() == null) {
            return null;
        }
        return getBetrokkeneIdentificatie().getIdentificatie();
    }

    @Override
    protected int hashCodeBetrokkeneIdentificatie() {
        return Objects.hash(getBetrokkeneIdentificatie().getIdentificatie());
    }
}
