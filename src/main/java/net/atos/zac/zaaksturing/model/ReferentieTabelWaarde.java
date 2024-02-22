/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.zaaksturing.model;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(schema = SCHEMA, name = "referentie_waarde")
@SequenceGenerator(schema = SCHEMA, name = "sq_referentie_waarde", sequenceName = "sq_referentie_waarde", allocationSize = 1)
public class ReferentieTabelWaarde {

    @Id
    @GeneratedValue(generator = "sq_referentie_waarde", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_referentie_waarde")
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_referentie_tabel", referencedColumnName = "id_referentie_tabel")
    private ReferentieTabel tabel;

    @NotBlank
    @Column(name = "naam", nullable = false)
    private String naam;

    @Column(name = "volgorde", nullable = false)
    private int volgorde;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public ReferentieTabel getTabel() {
        return tabel;
    }

    public void setTabel(final ReferentieTabel tabel) {
        this.tabel = tabel;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(final String naam) {
        this.naam = naam;
    }

    public int getVolgorde() {return volgorde;}

    public void setVolgorde(final int volgorde) {this.volgorde = volgorde;}
}
