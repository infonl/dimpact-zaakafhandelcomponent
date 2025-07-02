/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import java.net.URI;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum;
import nl.info.client.zgw.zrc.model.generated.RolNietNatuurlijkPersoon;
import nl.info.client.zgw.zrc.model.generated.VestigingIdentificatie;
import nl.info.client.zgw.ztc.model.generated.RolType;

/**
 * Manually copied from {@link nl.info.client.zgw.zrc.model.generated.RolVestiging} and modified to allow for
 * polymorphism using a generic base {@link Rol} class.
 * Ideally we would use the generated class, but currently we cannot get the OpenAPI Generator framework to generate
 * polymorphic relationships correctly.
 *
 * @deprecated the vestiging role is deprecated in the ZGW ZRC API and will be removed in a future version.
 *             Please use {@link RolNietNatuurlijkPersoon} instead for vestigingen.
 *             For details see: <a href="https://github.com/open-zaak/open-zaak/issues/1935">Add 'vestigingsnummer' to NNP, deprecate
 *             'vestiging'</a>.
 */
@Deprecated
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
