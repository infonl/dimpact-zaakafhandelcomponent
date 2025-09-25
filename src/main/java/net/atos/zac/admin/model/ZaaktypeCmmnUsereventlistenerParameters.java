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
@Table(schema = SCHEMA, name = "zaaktype_cmmn_usereventlistener_parameters")
@SequenceGenerator(schema = SCHEMA, name = "sq_zaaktype_cmmn_usereventlistener_parameters", sequenceName = "sq_zaaktype_cmmn_usereventlistener_parameters", allocationSize = 1)
public class ZaaktypeCmmnUsereventlistenerParameters implements UserModifiable<ZaaktypeCmmnUsereventlistenerParameters> {

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_usereventlistener_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @NotBlank @Column(name = "id_planitem_definition", nullable = false)
    private String planItemDefinitionID;

    @NotNull @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    private ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration;

    @Column(name = "toelichting")
    private String toelichting;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public ZaaktypeCmmnConfiguration getZaaktypeCmmnConfiguration() {
        return zaaktypeCmmnConfiguration;
    }

    public void setZaaktypeCmmnConfiguration(final ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration) {
        this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration;
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
        if (!(o instanceof ZaaktypeCmmnUsereventlistenerParameters that))
            return false;
        return Objects.equals(planItemDefinitionID, that.planItemDefinitionID) &&
               Objects.equals(toelichting, that.toelichting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(planItemDefinitionID, toelichting);
    }


    @Override
    public boolean isModifiedFrom(ZaaktypeCmmnUsereventlistenerParameters original) {
        return Objects.equals(planItemDefinitionID, original.planItemDefinitionID) &&
               !Objects.equals(this.toelichting, original.toelichting);
    }

    @Override
    public void applyChanges(ZaaktypeCmmnUsereventlistenerParameters changes) {
        this.toelichting = changes.toelichting;
    }

    @Override
    public ZaaktypeCmmnUsereventlistenerParameters resetId() {
        id = null;
        return this;
    }
}
