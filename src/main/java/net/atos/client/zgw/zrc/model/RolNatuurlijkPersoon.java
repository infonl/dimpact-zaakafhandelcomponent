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
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie;
import nl.info.client.zgw.ztc.model.generated.RolType;

/**
 * Manually copied from {@link nl.info.client.zgw.zrc.model.generated.RolNatuurlijkPersoon} and modified to allow for
 * polymorphism using a generic base {@link Rol} class.
 * Ideally we would use the generated class, but currently we cannot get the OpenAPI Generator framework to generate
 * polymorphic relationships correctly.
 */
public class RolNatuurlijkPersoon extends Rol<NatuurlijkPersoonIdentificatie> {

    public RolNatuurlijkPersoon() {
    }

    public RolNatuurlijkPersoon(
            final URI zaak,
            final RolType roltype,
            final String roltoelichting,
            final NatuurlijkPersoonIdentificatie betrokkeneIdentificatie
    ) {
        super(zaak, roltype, BetrokkeneTypeEnum.NATUURLIJK_PERSOON, betrokkeneIdentificatie, roltoelichting);
    }

    /**
     * For testing purposes only where we need a UUID.
     */
    public RolNatuurlijkPersoon(
            final UUID uuid,
            final RolType roltype,
            final String roltoelichting,
            final NatuurlijkPersoonIdentificatie betrokkeneIdentificatie
    ) {
        super(uuid, roltype, BetrokkeneTypeEnum.NATUURLIJK_PERSOON, betrokkeneIdentificatie, roltoelichting);
    }

    @Override
    protected boolean equalBetrokkeneIdentificatie(final NatuurlijkPersoonIdentificatie identificatie) {
        final NatuurlijkPersoonIdentificatie betrokkeneIdentificatie = getBetrokkeneIdentificatie();
        if (betrokkeneIdentificatie == identificatie) {
            return true;
        }
        if (identificatie == null) {
            return false;
        }
        // In volgorde van voorkeur (als er 1 matcht wordt de rest overgeslagen)
        if (betrokkeneIdentificatie.getAnpIdentificatie() != null || identificatie.getAnpIdentificatie() != null) {
            return Objects.equals(betrokkeneIdentificatie.getAnpIdentificatie(), identificatie.getAnpIdentificatie());
        }
        if (betrokkeneIdentificatie.getInpANummer() != null || identificatie.getInpANummer() != null) {
            return Objects.equals(betrokkeneIdentificatie.getInpANummer(), identificatie.getInpANummer());
        }
        if (betrokkeneIdentificatie.getInpBsn() != null || identificatie.getInpBsn() != null) {
            return Objects.equals(betrokkeneIdentificatie.getInpBsn(), identificatie.getInpBsn());
        }
        return true;
    }

    @Override
    public String getNaam() {
        if (getBetrokkeneIdentificatie() == null) {
            return null;
        }
        return StringUtils.isNotEmpty(getBetrokkeneIdentificatie().getVoorvoegselGeslachtsnaam()) ? getBetrokkeneIdentificatie()
                .getVoorvoegselGeslachtsnaam() : getIdentificatienummer();
    }

    @Override
    public String getIdentificatienummer() {
        if (getBetrokkeneIdentificatie() == null) {
            return null;
        }
        return getBetrokkeneIdentificatie().getInpBsn();
    }

    @Override
    protected int hashCodeBetrokkeneIdentificatie() {
        if (getBetrokkeneIdentificatie().getAnpIdentificatie() != null) {
            return Objects.hash(getBetrokkeneIdentificatie().getAnpIdentificatie());
        }
        if (getBetrokkeneIdentificatie().getInpANummer() != null) {
            return Objects.hash(getBetrokkeneIdentificatie().getInpANummer());
        }
        if (getBetrokkeneIdentificatie().getInpBsn() != null) {
            return Objects.hash(getBetrokkeneIdentificatie().getInpBsn());
        }
        return 0;
    }
}
