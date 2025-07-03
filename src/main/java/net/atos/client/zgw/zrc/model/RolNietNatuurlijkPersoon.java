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
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie;
import nl.info.client.zgw.ztc.model.generated.RolType;

/**
 * Manually copied from {@link nl.info.client.zgw.zrc.model.generated.RolNietNatuurlijkPersoon} and modified to allow for
 * polymorphism using a generic base {@link Rol} class.
 * Ideally we would use the generated class, but currently we cannot get the OpenAPI Generator framework to generate
 * polymorphic relationships correctly.
 */
public class RolNietNatuurlijkPersoon extends Rol<NietNatuurlijkPersoonIdentificatie> {

    public RolNietNatuurlijkPersoon() {
    }

    public RolNietNatuurlijkPersoon(
            final URI zaak,
            final RolType roltype,
            final String roltoelichting,
            final NietNatuurlijkPersoonIdentificatie betrokkeneIdentificatie
    ) {
        super(zaak, roltype, BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON, betrokkeneIdentificatie, roltoelichting);
    }

    /**
     * For testing purposes only where we need a UUID.
     */
    public RolNietNatuurlijkPersoon(
            final UUID uuid,
            final RolType roltype,
            final String roltoelichting,
            final NietNatuurlijkPersoonIdentificatie betrokkeneIdentificatie
    ) {
        super(uuid, roltype, BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON, betrokkeneIdentificatie, roltoelichting);
    }

    @Override
    protected boolean equalBetrokkeneIdentificatie(final NietNatuurlijkPersoonIdentificatie identificatie) {
        final NietNatuurlijkPersoonIdentificatie betrokkeneIdentificatie = getBetrokkeneIdentificatie();
        if (betrokkeneIdentificatie == identificatie) {
            return true;
        }
        if (identificatie == null || betrokkeneIdentificatie == null) {
            return false;
        }
        if (betrokkeneIdentificatie.getAnnIdentificatie() != null || identificatie.getAnnIdentificatie() != null) {
            return Objects.equals(betrokkeneIdentificatie.getAnnIdentificatie(), identificatie.getAnnIdentificatie());
        }
        if (betrokkeneIdentificatie.getInnNnpId() != null || identificatie.getInnNnpId() != null) {
            return Objects.equals(betrokkeneIdentificatie.getInnNnpId(), identificatie.getInnNnpId());
        }
        if (betrokkeneIdentificatie.getVestigingsNummer() != null || identificatie.getVestigingsNummer() != null) {
            return Objects.equals(betrokkeneIdentificatie.getVestigingsNummer(), identificatie.getVestigingsNummer());
        }
        return true;
    }

    @Override
    public String getNaam() {
        if (getBetrokkeneIdentificatie() == null) {
            return null;
        }
        return StringUtils.isNotEmpty(getBetrokkeneIdentificatie().getStatutaireNaam()) ?
                getBetrokkeneIdentificatie().getStatutaireNaam() : getIdentificatienummer();
    }

    @Override
    public String getIdentificatienummer() {
        NietNatuurlijkPersoonIdentificatie identificatie = getBetrokkeneIdentificatie();
        if (identificatie == null) {
            return null;
        }
        if (StringUtils.isNotEmpty(identificatie.getAnnIdentificatie())) {
            return identificatie.getAnnIdentificatie();
        }
        if (StringUtils.isNotEmpty(identificatie.getInnNnpId())) {
            return identificatie.getInnNnpId();
        }
        return identificatie.getVestigingsNummer();
    }

    @Override
    protected int hashCodeBetrokkeneIdentificatie() {
        NietNatuurlijkPersoonIdentificatie identificatie = getBetrokkeneIdentificatie();
        if (identificatie == null) {
            return 0;
        }
        if (identificatie.getAnnIdentificatie() != null) {
            return Objects.hash(getBetrokkeneIdentificatie().getAnnIdentificatie());
        }
        if (identificatie.getInnNnpId() != null) {
            return Objects.hash(getBetrokkeneIdentificatie().getInnNnpId());
        }
        if (identificatie.getVestigingsNummer() != null) {
            return Objects.hash(getBetrokkeneIdentificatie().getVestigingsNummer());
        }
        return 0;
    }
}
