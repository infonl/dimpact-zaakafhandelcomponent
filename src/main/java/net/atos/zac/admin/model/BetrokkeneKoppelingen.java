/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import net.atos.zac.util.FlywayIntegrator;

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "betrokkene_koppelingen")
@SequenceGenerator(
        schema = FlywayIntegrator.SCHEMA, name = "sq_betrokkene_koppelingen", sequenceName = "sq_betrokkene_koppelingen", allocationSize = 1
)
public class BetrokkeneKoppelingen {

    @Id
    @GeneratedValue(generator = "sq_betrokkene_koppelingen", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_betrokkene_koppelingen")
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_zaakafhandelparameters", referencedColumnName = "id_zaakafhandelparameters")
    @NotNull
    private ZaakafhandelParameters zaakafhandelParameters;

    @Column(name = "brpKoppelen")
    private boolean brpKoppelen;

    @Column(name = "kvkKoppelen")
    private boolean kvkKoppelen;

    public BetrokkeneKoppelingen() {
        // Default constructor
    }

    public BetrokkeneKoppelingen(ZaakafhandelParameters zaakafhandelParameters, boolean brpKoppelen, boolean kvkKoppelen) {
        this.zaakafhandelParameters = zaakafhandelParameters;
        this.brpKoppelen = brpKoppelen;
        this.kvkKoppelen = kvkKoppelen;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ZaakafhandelParameters getZaakafhandelParameters() {
        return zaakafhandelParameters;
    }

    public void setZaakafhandelParameters(ZaakafhandelParameters zaakafhandelParameters) {
        this.zaakafhandelParameters = zaakafhandelParameters;
    }

    public boolean getBrpKoppelen() {
        return brpKoppelen;
    }

    public void setBrpKoppelen(boolean brpKoppelen) {
        this.brpKoppelen = brpKoppelen;
    }

    public boolean getKvkKoppelen() {
        return kvkKoppelen;
    }

    public void setKvkKoppelen(boolean kvkKoppelen) {
        this.kvkKoppelen = kvkKoppelen;
    }
}
