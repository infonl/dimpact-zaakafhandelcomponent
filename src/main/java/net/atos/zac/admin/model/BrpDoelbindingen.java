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

import net.atos.zac.util.FlywayIntegrator;

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "brp_doelbindingen")
@SequenceGenerator(
        schema = FlywayIntegrator.SCHEMA, name = "sq_brp_doelbindingen", sequenceName = "sq_brp_doelbindingen", allocationSize = 1
)
public class BrpDoelbindingen {
    @Id
    @GeneratedValue(generator = "sq_brp_doelbindingen", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_brp_doelbindingen")
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_zaakafhandelparameters", referencedColumnName = "id_zaakafhandelparameters")
    @NotNull
    private ZaakafhandelParameters zaakafhandelParameters;

    @Column(name = "zoekWaarde")
    private String zoekWaarde = "";

    @Column(name = "raadpleegWaarde")
    private String raadpleegWaarde = "";

    public BrpDoelbindingen() {
        // Default constructor
    }

    public BrpDoelbindingen(ZaakafhandelParameters zaakafhandelParameters, String zoekWaarde, String raadpleegWaarde) {
        this.zaakafhandelParameters = zaakafhandelParameters;
        this.zoekWaarde = zoekWaarde;
        this.raadpleegWaarde = raadpleegWaarde;
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
