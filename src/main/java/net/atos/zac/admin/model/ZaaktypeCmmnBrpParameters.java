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
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_cmmn_brp_parameters")
@SequenceGenerator(
        schema = FlywayIntegrator.SCHEMA, name = "sq_zaaktype_cmmn_brp_parameters", sequenceName = "sq_zaaktype_cmmn_brp_parameters", allocationSize = 1
)
public class ZaaktypeCmmnBrpParameters {
    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_brp_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    @NotNull
    private ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration;

    @Column(name = "zoekWaarde")
    private String zoekWaarde = "";

    @Column(name = "raadpleegWaarde")
    private String raadpleegWaarde = "";

    public ZaaktypeCmmnBrpParameters() {
        // Default constructor
    }

    public ZaaktypeCmmnBrpParameters(ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration, String zoekWaarde, String raadpleegWaarde) {
        this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration;
        this.zoekWaarde = zoekWaarde;
        this.raadpleegWaarde = raadpleegWaarde;
    }


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

    public String getZoekWaarde() {
        return zoekWaarde;
    }

    public void setZoekWaarde(String zoekWaarde) {
        this.zoekWaarde = zoekWaarde;
    }

    public String getRaadpleegWaarde() {
        return raadpleegWaarde;
    }

    public void setRaadpleegWaarde(String raadpleegWaarde) {
        this.raadpleegWaarde = raadpleegWaarde;
    }
}
