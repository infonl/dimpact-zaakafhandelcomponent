/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import java.net.URI;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum;
import nl.info.client.zgw.zrc.model.generated.VestigingIdentificatie;
import nl.info.client.zgw.ztc.model.generated.RolType;

public class RolVestiging extends Rol<VestigingIdentificatie> {

    public RolVestiging() {
    }

    public RolVestiging(
            final URI zaak,
            final RolType roltype,
            final String roltoelichting,
            final VestigingIdentificatie betrokkeneIdentificatie
    ) {
        super(zaak, roltype, BetrokkeneTypeEnum.VESTIGING, betrokkeneIdentificatie, roltoelichting);
    }

    @Override
    protected boolean equalBetrokkeneIdentificatie(final VestigingIdentificatie identificatie) {
        final VestigingIdentificatie betrokkeneIdentificatie = getBetrokkeneIdentificatie();
        if (betrokkeneIdentificatie == identificatie) {
            return true;
        }
        if (identificatie == null) {
            return false;
        }
        return Objects.equals(betrokkeneIdentificatie.getVestigingsNummer(), identificatie.getVestigingsNummer());
    }

    @Override
    public String getNaam() {
        if (getBetrokkeneIdentificatie() == null) {
            return null;
        }
        final String namen = getBetrokkeneIdentificatie().getHandelsnaam() != null ? String.join("; ", getBetrokkeneIdentificatie()
                .getHandelsnaam()) : null;
        return StringUtils.isNotEmpty(namen) ? namen : getIdentificatienummer();
    }

    @Override
    public String getIdentificatienummer() {
        if (getBetrokkeneIdentificatie() == null) {
            return null;
        }
        return getBetrokkeneIdentificatie().getVestigingsNummer();
    }

    @Override
    protected int hashCodeBetrokkeneIdentificatie() {
        return Objects.hash(getBetrokkeneIdentificatie().getVestigingsNummer());
    }
}
