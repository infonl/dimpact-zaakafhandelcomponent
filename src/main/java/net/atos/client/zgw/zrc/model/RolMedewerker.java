/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum;
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie;
import nl.info.client.zgw.ztc.model.generated.RolType;

/**
 * Manually copied from {@link nl.info.client.zgw.zrc.model.generated.RolMedewerker} and modified to allow for
 * polymorphism using a generic base {@link Rol} class.
 * Ideally we would use the generated class, but currently we cannot get the OpenAPI Generator framework to generate
 * polymorphic relationships correctly.
 */
public class RolMedewerker extends Rol<MedewerkerIdentificatie> {

    public RolMedewerker() {
    }

    /**
     * For testing purposes only where we need a UUID.
     */
    public RolMedewerker(
            final UUID uuid,
            final RolType roltype,
            final String roltoelichting,
            final MedewerkerIdentificatie betrokkeneIdentificatie
    ) {
        super(uuid, roltype, BetrokkeneTypeEnum.MEDEWERKER, betrokkeneIdentificatie, roltoelichting);
    }

    public RolMedewerker(
            final URI zaak,
            final RolType roltype,
            final String roltoelichting,
            // it is possible in the ZGW API to have a RolMedewerker without a Medewerker,
            // and this does occur in practice in certain circumstances
            @Nullable final MedewerkerIdentificatie betrokkeneIdentificatie
    ) {
        super(zaak, roltype, BetrokkeneTypeEnum.MEDEWERKER, betrokkeneIdentificatie, roltoelichting);
    }

    public String getNaam() {
        if (getBetrokkeneIdentificatie() == null) {
            return null;
        }
        final MedewerkerIdentificatie medewerker = getBetrokkeneIdentificatie();
        if (isNotBlank(medewerker.getAchternaam())) {
            final StringBuilder naam = new StringBuilder();
            if (isNotBlank(medewerker.getVoorletters())) {
                naam.append(medewerker.getVoorletters());
                naam.append(StringUtils.SPACE);
            }
            if (isNotBlank(medewerker.getVoorvoegselAchternaam())) {
                naam.append(medewerker.getVoorvoegselAchternaam());
                naam.append(StringUtils.SPACE);
            }
            naam.append(medewerker.getAchternaam());
            return naam.toString();
        } else {
            return medewerker.getIdentificatie();
        }
    }

    @Override
    protected boolean equalBetrokkeneIdentificatie(final MedewerkerIdentificatie identificatie) {
        final MedewerkerIdentificatie betrokkeneIdentificatie = getBetrokkeneIdentificatie();
        if (betrokkeneIdentificatie == identificatie) {
            return true;
        }
        if (identificatie == null) {
            return false;
        }
        return Objects.equals(betrokkeneIdentificatie.getIdentificatie(), identificatie.getIdentificatie());
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
        if (getBetrokkeneIdentificatie() == null) {
            return -1;
        }
        return Objects.hash(getBetrokkeneIdentificatie().getIdentificatie());
    }
}
