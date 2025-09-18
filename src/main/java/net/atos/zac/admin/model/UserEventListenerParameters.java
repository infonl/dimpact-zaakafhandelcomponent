/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin.model;

import static nl.info.zac.database.flyway.FlywayIntegrator.SCHEMA;

import java.util.Objects;

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
public class UserEventListenerParameters implements ZaakafhandelComponent {

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserEventListenerParameters that))
            return false;
        return Objects.equals(toelichting, that.toelichting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toelichting);
    }

    @Override
    public <T extends ZaakafhandelComponent> boolean isChanged(T original) {
        return !this.equals(original);
    }

    @Override
    public <T extends ZaakafhandelComponent> void modify(T changes) {
        if (!(changes instanceof UserEventListenerParameters that))
            throw new IllegalArgumentException("Invalid data type for element modification");
        this.toelichting = that.toelichting;
    }
}
