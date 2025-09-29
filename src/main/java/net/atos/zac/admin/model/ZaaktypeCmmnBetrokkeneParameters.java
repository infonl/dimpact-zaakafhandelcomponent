/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
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

import nl.info.zac.database.flyway.FlywayIntegrator;

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_cmmn_betrokkene_parameters")
@SequenceGenerator(
        schema = FlywayIntegrator.SCHEMA, name = "sq_zaaktype_cmmn_betrokkene_parameters", sequenceName = "sq_zaaktype_cmmn_betrokkene_parameters", allocationSize = 1
)
public class ZaaktypeCmmnBetrokkeneParameters {

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_betrokkene_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    @NotNull
    private ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration;

    @Column(name = "brpKoppelen")
    private boolean brpKoppelen = false;

    @Column(name = "kvkKoppelen")
    private boolean kvkKoppelen = false;

    public ZaaktypeCmmnBetrokkeneParameters() {
        // Default constructor
    }

    public ZaaktypeCmmnBetrokkeneParameters(ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration, boolean brpKoppelen, boolean kvkKoppelen) {
        this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration;
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

    public ZaaktypeCmmnConfiguration getZaaktypeCmmnConfiguration() {
        return zaaktypeCmmnConfiguration;
    }

    public void setZaaktypeCmmnConfiguration(ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration) {
        this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration;
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
