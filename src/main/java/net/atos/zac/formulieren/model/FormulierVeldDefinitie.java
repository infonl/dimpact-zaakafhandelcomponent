/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.formulieren.model;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(schema = SCHEMA, name = "formulier_veld_definitie")
@SequenceGenerator(schema = SCHEMA, name = "sq_formulier_veld_definitie", sequenceName = "sq_formulier_veld_definitie", allocationSize = 1)
public class FormulierVeldDefinitie {

    @Id
    @GeneratedValue(generator = "sq_formulier_veld_definitie", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_formulier_veld_definitie")
    private Long id;

    @NotNull @ManyToOne
    @JoinColumn(name = "id_formulier_definitie", referencedColumnName = "id_formulier_definitie")
    private FormulierDefinitie formulierDefinitie;

    @NotBlank @Column(name = "systeemnaam", nullable = false, unique = true)
    private String systeemnaam;

    @PositiveOrZero @Column(name = "volgorde", nullable = false)
    private int volgorde;

    @NotBlank @Column(name = "label", nullable = false)
    private String label;

    @NotNull @Column(name = "veldtype", nullable = false)
    @Enumerated(EnumType.STRING)
    private FormulierVeldtype veldtype;

    @Column(name = "beschrijving")
    private String beschrijving;

    @Column(name = "helptekst")
    private String helptekst;

    @Column(name = "verplicht")
    private boolean verplicht;

    @Column(name = "default_waarde")
    private String defaultWaarde;

    @Column(name = "meerkeuze_opties")
    private String meerkeuzeOpties;

    @Column(name = "validaties")
    private String validaties;


    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public FormulierDefinitie getFormulierDefinitie() {
        return formulierDefinitie;
    }

    public void setFormulierDefinitie(final FormulierDefinitie formulierDefinitie) {
        this.formulierDefinitie = formulierDefinitie;
    }

    public String getSysteemnaam() {
        return systeemnaam;
    }

    public void setSysteemnaam(final String systeemnaam) {
        this.systeemnaam = systeemnaam;
    }

    public int getVolgorde() {
        return volgorde;
    }

    public void setVolgorde(final int volgorde) {
        this.volgorde = volgorde;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public FormulierVeldtype getVeldtype() {
        return veldtype;
    }

    public void setVeldtype(final FormulierVeldtype veldType) {
        this.veldtype = veldType;
    }

    public String getBeschrijving() {
        return beschrijving;
    }

    public void setBeschrijving(final String beschrijving) {
        this.beschrijving = beschrijving;
    }

    public String getHelptekst() {
        return helptekst;
    }

    public void setHelptekst(final String helptekst) {
        this.helptekst = helptekst;
    }

    public boolean isVerplicht() {
        return verplicht;
    }

    public void setVerplicht(final boolean verplicht) {
        this.verplicht = verplicht;
    }

    public String getDefaultWaarde() {
        return defaultWaarde;
    }

    public void setDefaultWaarde(final String defaultWaarde) {
        this.defaultWaarde = defaultWaarde;
    }

    public String getMeerkeuzeOpties() {
        return meerkeuzeOpties;
    }

    public void setMeerkeuzeOpties(final String meerkeuzeWaarden) {
        this.meerkeuzeOpties = meerkeuzeWaarden;
    }

    public String getValidaties() {
        return validaties;
    }

    public void setValidaties(final String validaties) {
        this.validaties = validaties;
    }
}
