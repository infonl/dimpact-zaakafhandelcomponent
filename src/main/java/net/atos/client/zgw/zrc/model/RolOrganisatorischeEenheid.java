/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum;
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie;
import nl.info.client.zgw.ztc.model.generated.RolType;

public class RolOrganisatorischeEenheid extends Rol<OrganisatorischeEenheidIdentificatie> {

    public RolOrganisatorischeEenheid() {
    }

    /**
     * For testing purposes only.
     */
    public RolOrganisatorischeEenheid(
            final UUID uuid,
            final RolType roltype
    ) {
        super(uuid, roltype, BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID);
    }

    public RolOrganisatorischeEenheid(
            final URI zaak,
            final RolType roltype,
            final String roltoelichting,
            final OrganisatorischeEenheidIdentificatie organisatorischeEenheid
    ) {
        super(zaak, roltype, BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID, organisatorischeEenheid, roltoelichting);
    }

    @Override
    protected boolean equalBetrokkeneIdentificatie(final OrganisatorischeEenheidIdentificatie identificatie) {
        final OrganisatorischeEenheidIdentificatie betrokkeneIdentificatie = getBetrokkeneIdentificatie();
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
