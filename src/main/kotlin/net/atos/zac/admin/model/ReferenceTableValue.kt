/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import net.atos.zac.util.FlywayIntegrator
import nl.lifely.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "referentie_waarde")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_referentie_waarde",
    sequenceName = "sq_referentie_waarde",
    allocationSize = 1
)
@AllOpen
class ReferenceTableValue {
    @Id
    @GeneratedValue(generator = "sq_referentie_waarde", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_referentie_waarde")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "id_referentie_tabel", referencedColumnName = "id_referentie_tabel")
    lateinit var tabel: @NotNull ReferenceTable

    @Column(name = "naam", nullable = false)
    lateinit var naam: @NotBlank String

    @Column(name = "volgorde", nullable = false)
    var volgorde: Int = 0
}
