/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.kvk.model;

import java.math.BigDecimal;

import jakarta.ws.rs.QueryParam;

public class KvkZoekenParameters {

    @QueryParam("kvkNummer")
    private String kvkNummer;

    @QueryParam("rsin")
    private String rsin;

    @QueryParam("vestigingsnummer")
    private String vestigingsnummer;

    @QueryParam("naam")
    private String naam;

    @QueryParam("straatnaam")
    private String straatnaam;

    @QueryParam("plaats")
    private String plaats;

    @QueryParam("postcode")
    private String postcode;

    @QueryParam("huisnummer")
    private String huisnummer;

    @QueryParam("type")
    private String type;

    @QueryParam("InclusiefInactieveRegistraties")
    private Boolean inclusiefInactieveRegistraties;

    @QueryParam("pagina")
    private BigDecimal pagina;

    @QueryParam("aantal")
    private BigDecimal resultatenPerPagina;

    public String getKvkNummer() {
        return kvkNummer;
    }

    public void setKvkNummer(final String kvkNummer) {
        this.kvkNummer = kvkNummer;
    }

    public String getRsin() {
        return rsin;
    }

    public void setRsin(final String rsin) {
        this.rsin = rsin;
    }

    public String getVestigingsnummer() {
        return vestigingsnummer;
    }

    public void setVestigingsnummer(final String vestigingsnummer) {
        this.vestigingsnummer = vestigingsnummer;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(final String naam) {
        this.naam = naam;
    }

    public String getStraatnaam() {
        return straatnaam;
    }

    public void setStraatnaam(final String straatnaam) {
        this.straatnaam = straatnaam;
    }

    public String getPlaats() {
        return plaats;
    }

    public void setPlaats(final String plaats) {
        this.plaats = plaats;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(final String postcode) {
        this.postcode = postcode;
    }

    public String getHuisnummer() {
        return huisnummer;
    }

    public void setHuisnummer(final String huisnummer) {
        this.huisnummer = huisnummer;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Boolean getInclusiefInactieveRegistraties() {
        return inclusiefInactieveRegistraties;
    }

    public void setInclusiefInactieveRegistraties(final Boolean inclusiefInactieveRegistraties) {
        this.inclusiefInactieveRegistraties = inclusiefInactieveRegistraties;
    }

    public BigDecimal getPagina() {
        return pagina;
    }

    public void setPagina(final BigDecimal pagina) {
        this.pagina = pagina;
    }

    public BigDecimal getResultatenPerPagina() {
        return resultatenPerPagina;
    }

    public void setResultatenPerPagina(final BigDecimal resultatenPerPagina) {
        this.resultatenPerPagina = resultatenPerPagina;
    }
}
