/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model;

import static nl.info.zac.database.flyway.FlywayIntegrator.SCHEMA;

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
@Table(schema = SCHEMA, name = "zaakbeeindigparameter")
@SequenceGenerator(schema = SCHEMA, name = "sq_zaakbeeindigparameter", sequenceName = "sq_zaakbeeindigparameter", allocationSize = 1)
public class ZaakbeeindigParameter {

    @Id
    @GeneratedValue(generator = "sq_zaakbeeindigparameter", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_zaakbeeindigparameter")
    private Long id;

    @NotNull @ManyToOne
    @JoinColumn(name = "id_zaakafhandelparameters", referencedColumnName = "id_zaakafhandelparameters")
    private ZaakafhandelParameters zaakafhandelParameters;

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

    public ZaakafhandelParameters getZaakafhandelParameters() {
        return zaakafhandelParameters;
    }

    public void setZaakafhandelParameters(final ZaakafhandelParameters zaakafhandelParameters) {
        this.zaakafhandelParameters = zaakafhandelParameters;
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
}
