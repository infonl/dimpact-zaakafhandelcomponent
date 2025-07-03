/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import static net.atos.client.zgw.shared.util.DateTimeUtil.DATE_TIME_FORMAT_WITH_MILLISECONDS;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;

import nl.info.client.zgw.zrc.jsonb.RolJsonbDeserializer;
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum;
import nl.info.client.zgw.zrc.model.generated.IndicatieMachtigingEnum;
import nl.info.client.zgw.ztc.model.generated.RolType;

@JsonbTypeDeserializer(RolJsonbDeserializer.class)
public abstract class Rol<T> {

    public static final String BETROKKENE_TYPE_NAAM = "betrokkeneType";

    /**
     * URL-referentie naar dit object.
     * Dit is de unieke identificatie en locatie van dit object.
     */
    private URI url;

    /**
     * Unieke resource identifier (UUID4)
     */
    private UUID uuid;

    /**
     * URL-referentie naar de ZAAK.
     * - Required
     */
    private URI zaak;

    /**
     * URL-referentie naar een betrokkene gerelateerd aan de ZAAK.
     */
    private URI betrokkene;

    /**
     * De generieke betrokkene
     * - Required
     */
    private T betrokkeneIdentificatie;

    /**
     * Betrokkene type
     * - Required
     */
    private BetrokkeneTypeEnum betrokkeneType;

    /**
     * URL-referentie naar een roltype binnen het ZAAKTYPE van de ZAAK.
     * - Required
     */
    private URI roltype;

    /**
     * Omschrijving van de aard van de ROL, afgeleid uit het ROLTYPE.
     */
    private String omschrijving;

    /**
     * Algemeen gehanteerde benaming van de aard van de ROL, afgeleid uit het ROLTYPE.
     * Uitleg bij mogelijke waarden:
     * 'adviseur' - (Adviseur) Kennis in dienst stellen van de behandeling van (een deel van) een zaak.
     * 'behandelaar' - (Behandelaar) De vakinhoudelijke behandeling doen van (een deel van) een zaak.
     * 'belanghebbende' - (Belanghebbende) Vanuit eigen en objectief belang rechtstreeks betrokken zijn bij de behandeling en/of de uitkomst
     * van een zaak.
     * 'beslisser' - (Beslisser) Nemen van besluiten die voor de uitkomst van een zaak noodzakelijk zijn.
     * 'initiator' - (Initiator) Aanleiding geven tot de start van een zaak
     * 'klantcontacter' - (Klantcontacter) Het eerste aanspreekpunt zijn voor vragen van burgers en bedrijven
     * 'zaakcoordinator' - (Zaakcoordinator) Er voor zorg dragen dat de behandeling van de zaak in samenhang uitgevoerd wordt conform de
     * daarover gemaakte afspraken.
     * 'mede_initiator' - 'Mede-initiator'
     */
    private String omschrijvingGeneriek;

    /**
     * Roltoelichting
     * - Required
     */
    private String roltoelichting;

    /**
     * De datum waarop dit object is geregistreerd.
     */
    @JsonbDateFormat(DATE_TIME_FORMAT_WITH_MILLISECONDS)
    private ZonedDateTime registratiedatum;

    private IndicatieMachtigingEnum indicatieMachtiging;

    public Rol() {
    }

    /**
     * For testing purposes only where a rol with a UUID is needed.
     */
    public Rol(
            final UUID uuid,
            final RolType roltype,
            final BetrokkeneTypeEnum betrokkeneType,
            final T betrokkeneIdentificatie,
            final String roltoelichting
    ) {
        this.uuid = uuid;
        this.betrokkeneIdentificatie = betrokkeneIdentificatie;
        this.betrokkeneType = betrokkeneType;
        this.roltype = roltype.getUrl();
        this.roltoelichting = roltoelichting;
        this.omschrijving = roltype.getOmschrijving();
        this.omschrijvingGeneriek = roltype.getOmschrijvingGeneriek().name().toLowerCase();
    }

    /**
     * Constructor with required attributes for POST and PUT requests
     */
    public Rol(
            final URI zaak,
            final RolType roltype,
            final BetrokkeneTypeEnum betrokkeneType,
            final T betrokkeneIdentificatie,
            final String roltoelichting
    ) {
        this.zaak = zaak;
        this.betrokkeneIdentificatie = betrokkeneIdentificatie;
        this.betrokkeneType = betrokkeneType;
        this.roltype = roltype.getUrl();
        this.roltoelichting = roltoelichting;
        this.omschrijving = roltype.getOmschrijving();
        this.omschrijvingGeneriek = roltype.getOmschrijvingGeneriek().name().toLowerCase();
    }

    public URI getUrl() {
        return url;
    }

    public UUID getUuid() {
        return uuid;
    }

    public URI getZaak() {
        return zaak;
    }

    public URI getBetrokkene() {
        return betrokkene;
    }

    public void setBetrokkene(final URI betrokkene) {
        this.betrokkene = betrokkene;
    }

    public BetrokkeneTypeEnum getBetrokkeneType() {
        return betrokkeneType;
    }

    public URI getRoltype() {
        return roltype;
    }

    public String getOmschrijving() {
        return omschrijving;
    }

    public String getOmschrijvingGeneriek() {
        return omschrijvingGeneriek;
    }

    public String getRoltoelichting() {
        return roltoelichting;
    }

    public ZonedDateTime getRegistratiedatum() {
        return registratiedatum;
    }

    public IndicatieMachtigingEnum getIndicatieMachtiging() {
        return indicatieMachtiging;
    }

    public void setIndicatieMachtiging(final IndicatieMachtigingEnum indicatieMachtiging) {
        this.indicatieMachtiging = indicatieMachtiging;
    }

    /**
     * Can be null according to the ZGW API and this does occur in practice in certain circumstances.
     *
     * @return the betrokkene identificatie; or null if there is none
     */
    @Nullable
    public T getBetrokkeneIdentificatie() {
        return betrokkeneIdentificatie;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Rol<T> rol = (Rol<T>) o;
        return equalBetrokkeneRol(rol) && equalBetrokkeneIdentificatie(rol.getBetrokkeneIdentificatie());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRoltype(), getBetrokkeneType(), hashCodeBetrokkeneIdentificatie());
    }

    public boolean equalBetrokkeneRol(final Rol<?> other) {
        return getBetrokkeneType() == other.getBetrokkeneType() &&
               getRoltype().equals(other.getRoltype());
    }

    protected abstract boolean equalBetrokkeneIdentificatie(final T identificatie);

    protected abstract int hashCodeBetrokkeneIdentificatie();

    public abstract String getNaam();

    public abstract String getIdentificatienummer();
}
