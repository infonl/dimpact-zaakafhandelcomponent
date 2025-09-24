/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin.model;

import static nl.info.zac.database.flyway.FlywayIntegrator.SCHEMA;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(schema = SCHEMA, name = "zaaktype_cmmn_completion_parameters")
@SequenceGenerator(schema = SCHEMA, name = "sq_zaaktype_cmmn_completion_parameters", sequenceName = "sq_zaaktype_cmmn_completion_parameters", allocationSize = 1)
public class ZaaktypeCmmnCompletionParameters implements UserModifiable<ZaaktypeCmmnCompletionParameters> {

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_completion_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @NotNull @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    private ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration;

    @NotNull @ManyToOne
    @JoinColumn(name = "id_zaakbeeindigreden", referencedColumnName = "id_zaakbeeindigreden")
    private ZaakbeeindigReden zaakbeeindigReden;

    @NotNull @Column(name = "resultaattype_uuid", nullable = false)
    private UUID resultaattype;

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

    public ZaakbeeindigReden getZaakbeeindigReden() {
        return zaakbeeindigReden;
    }

    public void setZaakbeeindigReden(final ZaakbeeindigReden zaakbeeindigReden) {
        this.zaakbeeindigReden = zaakbeeindigReden;
    }

    public UUID getResultaattype() {
        return resultaattype;
    }

    public void setResultaattype(final UUID resultaattypeUUID) {
        this.resultaattype = resultaattypeUUID;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZaaktypeCmmnCompletionParameters that))
            return false;
        if (zaakbeeindigReden == null || that.zaakbeeindigReden == null)
            throw new IllegalStateException("zaakbeeindigReden is null");
        return Objects.equals(zaakbeeindigReden.getId(), that.zaakbeeindigReden.getId()) &&
               Objects.equals(resultaattype, that.resultaattype);
    }

    @Override
    public int hashCode() {
        if (zaakbeeindigReden == null)
            throw new IllegalStateException("zaakbeeindigReden is null");
        return Objects.hash(zaakbeeindigReden.getId(), resultaattype);
    }

    @Override
    public boolean isModifiedFrom(ZaaktypeCmmnCompletionParameters original) {
        return Objects.equals(zaakbeeindigReden, original.zaakbeeindigReden) && !Objects.equals(resultaattype, original.resultaattype);
    }

    @Override
    public void applyChanges(ZaaktypeCmmnCompletionParameters changes) {
        resultaattype = changes.resultaattype;
    }

    @Override
    public ZaaktypeCmmnCompletionParameters resetId() {
        id = null;
        return this;
    }
}
