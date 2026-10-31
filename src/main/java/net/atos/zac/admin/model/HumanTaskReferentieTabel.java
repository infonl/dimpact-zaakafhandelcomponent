/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model;

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

import nl.info.zac.admin.model.ReferenceTable;

@Entity
@Table(schema = SCHEMA, name = "humantask_referentie_tabel")
@SequenceGenerator(schema = SCHEMA, name = "sq_humantask_referentie_tabel", sequenceName = "sq_humantask_referentie_tabel", allocationSize = 1)
public class HumanTaskReferentieTabel {

    @Id
    @GeneratedValue(generator = "sq_humantask_referentie_tabel", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_humantask_referentie_tabel")
    private Long id;

    @NotNull @ManyToOne
    @JoinColumn(name = "id_referentie_tabel", referencedColumnName = "id_referentie_tabel")
    private ReferenceTable tabel;

    @NotNull @ManyToOne
    @JoinColumn(name = "id_humantask_parameters", referencedColumnName = "id_humantask_parameters")
    private HumanTaskParameters humantask;

    @NotBlank @Column(name = "veld", nullable = false)
    private String veld;

    public HumanTaskReferentieTabel() {
    }

    public HumanTaskReferentieTabel(final String veld, final ReferenceTable tabel) {
        this.veld = veld;
        this.tabel = tabel;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public ReferenceTable getTabel() {
        return tabel;
    }

    public void setTabel(final ReferenceTable tabel) {
        this.tabel = tabel;
    }

    public HumanTaskParameters getHumantask() {
        return humantask;
    }

    public void setHumantask(final HumanTaskParameters humantask) {
        this.humantask = humantask;
    }

    public String getVeld() {
        return veld;
    }

    public void setVeld(final String veld) {
        this.veld = veld;
    }
}
