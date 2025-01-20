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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(schema = SCHEMA, name = "zaakbeeindigreden")
@SequenceGenerator(schema = SCHEMA, name = "sq_zaakbeeindigreden", sequenceName = "sq_zaakbeeindigreden", allocationSize = 1)
public class ZaakbeeindigReden {

    @Id
    @GeneratedValue(generator = "sq_zaakbeeindigreden", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_zaakbeeindigreden")
    private Long id;

    @NotBlank @Column(name = "naam", nullable = false)
    private String naam;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(final String naam) {
        this.naam = naam;
    }
}
