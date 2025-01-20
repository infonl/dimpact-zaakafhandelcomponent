/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
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

@Entity
@Table(schema = SCHEMA, name = "usereventlistener_parameters")
@SequenceGenerator(schema = SCHEMA, name = "sq_usereventlistener_parameters", sequenceName = "sq_usereventlistener_parameters", allocationSize = 1)
public class UserEventListenerParameters {

    @Id
    @GeneratedValue(generator = "sq_usereventlistener_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_usereventlistener_parameters")
    private Long id;

    @NotBlank @Column(name = "id_planitem_definition", nullable = false)
    private String planItemDefinitionID;

    @NotNull @ManyToOne
    @JoinColumn(name = "id_zaakafhandelparameters", referencedColumnName = "id_zaakafhandelparameters")
    private ZaakafhandelParameters zaakafhandelParameters;

    @Column(name = "toelichting")
    private String toelichting;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public ZaakafhandelParameters getZaakafhandelParameters() {
        return zaakafhandelParameters;
    }

    public void setZaakafhandelParameters(final ZaakafhandelParameters zaakafhandelParameters) {
        this.zaakafhandelParameters = zaakafhandelParameters;
    }

    public String getPlanItemDefinitionID() {
        return planItemDefinitionID;
    }

    public void setPlanItemDefinitionID(final String planItemDefinitionID) {
        this.planItemDefinitionID = planItemDefinitionID;
    }

    public String getToelichting() {
        return toelichting;
    }

    public void setToelichting(final String toelichting) {
        this.toelichting = toelichting;
    }
}
