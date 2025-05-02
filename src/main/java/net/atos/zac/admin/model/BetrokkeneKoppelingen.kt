/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import net.atos.zac.util.FlywayIntegrator

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "betrokkene_koppelingen")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_betrokkene_koppelingen",
    sequenceName = "sq_betrokkene_koppelingen",
    allocationSize = 1
)
open class BetrokkeneKoppelingen(
    @Id
    @GeneratedValue(generator = "sq_betrokkene_koppelingen", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_betrokkene_koppelingen")
    var id: Long? = null,

    @OneToOne
    @JoinColumn(name = "id_zaakafhandelparameters", referencedColumnName = "id_zaakafhandelparameters")
    @NotNull
    var zaakafhandelParameters: ZaakafhandelParameters? = null,

    @Column(name = "brpKoppelen")
    var brpKoppelen: Boolean = false,

    @Column(name = "kvkKoppelen")
    var kvkKoppelen: Boolean = false
)
