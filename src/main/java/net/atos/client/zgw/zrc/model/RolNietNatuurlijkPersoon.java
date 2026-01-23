/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import java.net.URI;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import nl.info.client.zgw.ztc.model.generated.RolType;

public class RolNietNatuurlijkPersoon extends Rol<NietNatuurlijkPersoon> {

    public RolNietNatuurlijkPersoon() {
    }

    public RolNietNatuurlijkPersoon(
            final URI zaak,
            final RolType roltype,
            final String roltoelichting,
            final NietNatuurlijkPersoon betrokkeneIdentificatie
    ) {
        super(zaak, roltype, BetrokkeneType.NIET_NATUURLIJK_PERSOON, betrokkeneIdentificatie, roltoelichting);
    }

    @Override
    protected boolean equalBetrokkeneIdentificatie(final NietNatuurlijkPersoon identificatie) {
        final NietNatuurlijkPersoon betrokkeneIdentificatie = getBetrokkeneIdentificatie();
        if (betrokkeneIdentificatie == identificatie) {
            return true;
        }
        if (identificatie == null) {
            return false;
        }
        // In volgorde van voorkeur (als er 1 matcht wordt de rest overgeslagen)
        if (betrokkeneIdentificatie.getAnnIdentificatie() != null || identificatie.getAnnIdentificatie() != null) {
            return Objects.equals(betrokkeneIdentificatie.getAnnIdentificatie(), identificatie.getAnnIdentificatie());
        }
        if (betrokkeneIdentificatie.getInnNnpId() != null || identificatie.getInnNnpId() != null) {
            return Objects.equals(betrokkeneIdentificatie.getInnNnpId(), identificatie.getInnNnpId());
        }
        return true;
    }

    @Override
    public String getNaam() {
        if (getBetrokkeneIdentificatie() == null) {
            return null;
        }
        return StringUtils.isNotEmpty(getBetrokkeneIdentificatie().getStatutaireNaam()) ? getBetrokkeneIdentificatie().getStatutaireNaam() :
                getIdentificatienummer();
    }

    @Override
    public String getIdentificatienummer() {
        if (getBetrokkeneIdentificatie() == null) {
            return null;
        }
        return StringUtils.isNotEmpty(getBetrokkeneIdentificatie().getAnnIdentificatie()) ? getBetrokkeneIdentificatie()
                .getAnnIdentificatie() : getBetrokkeneIdentificatie().getInnNnpId();
    }

    @Override
    protected int hashCodeBetrokkeneIdentificatie() {
        if (getBetrokkeneIdentificatie().getAnnIdentificatie() != null) {
            return Objects.hash(getBetrokkeneIdentificatie().getAnnIdentificatie());
        }
        if (getBetrokkeneIdentificatie().getInnNnpId() != null) {
            return Objects.hash(getBetrokkeneIdentificatie().getInnNnpId());
        }
        return 0;
    }
}
