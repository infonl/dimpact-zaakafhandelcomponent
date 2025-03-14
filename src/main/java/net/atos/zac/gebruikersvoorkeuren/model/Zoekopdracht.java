/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.gebruikersvoorkeuren.model;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(schema = SCHEMA, name = "zoekopdracht")
@SequenceGenerator(schema = SCHEMA, name = "sq_zoekopdracht", sequenceName = "sq_zoekopdracht", allocationSize = 1)
public class Zoekopdracht {

    /** Naam van property: {@link Zoekopdracht#medewerkerID} */
    public static final String MEDEWERKER_ID = "medewerkerID";

    /** Naam van property: {@link Zoekopdracht#lijstID} */
    public static final String LIJST_ID = "lijstID";

    /** Naam van property: {@link Zoekopdracht#naam} */
    public static final String NAAM = "naam";

    @Id
    @GeneratedValue(generator = "sq_zoekopdracht", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_zoekopdracht")
    private Long id;

    @NotNull @Column(name = "creatiedatum", nullable = false)
    private ZonedDateTime creatiedatum;

    @NotBlank @Column(name = "naam", nullable = false)
    private String naam;

    @NotNull @Column(name = "id_lijst", nullable = false)
    @Enumerated(EnumType.STRING)
    private Werklijst lijstID;

    @Column(name = "actief")
    private boolean actief;

    @NotBlank @Column(name = "id_medewerker", nullable = false)
    private String medewerkerID;

    @NotBlank @Column(name = "json_zoekopdracht", nullable = false)
    private String json;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public ZonedDateTime getCreatiedatum() {
        return creatiedatum;
    }

    public void setCreatiedatum(final ZonedDateTime creatiedatum) {
        this.creatiedatum = creatiedatum;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(final String naam) {
        this.naam = naam;
    }

    public Werklijst getLijstID() {
        return lijstID;
    }

    public void setLijstID(final Werklijst lijstID) {
        this.lijstID = lijstID;
    }

    public String getMedewerkerID() {
        return medewerkerID;
    }

    public void setMedewerkerID(final String medewerkerID) {
        this.medewerkerID = medewerkerID;
    }

    public String getJson() {
        return json;
    }

    public void setJson(final String json) {
        this.json = json;
    }

    public boolean isActief() {
        return actief;
    }

    public void setActief(final boolean actief) {
        this.actief = actief;
    }
}
