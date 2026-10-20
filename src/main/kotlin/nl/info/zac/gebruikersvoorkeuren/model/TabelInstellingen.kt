/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.gebruikersvoorkeuren.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "tabel_instellingen")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_tabel_instellingen",
    sequenceName = "sq_tabel_instellingen",
    allocationSize = 1
)
@AllOpen
class TabelInstellingen {
    companion object {
        const val AANTAL_PER_PAGINA_DEFAULT = 25
        const val AANTAL_PER_PAGINA_MAX = 100
        const val AANTAL_PER_PAGINA_MIN = 10

        val PAGE_SIZE_OPTIONS = listOf(AANTAL_PER_PAGINA_MIN, AANTAL_PER_PAGINA_DEFAULT, 50, AANTAL_PER_PAGINA_MAX)
    }

    @Id
    @GeneratedValue(generator = "sq_tabel_instellingen", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_tabel_instellingen")
    var id: Long? = null

    @NotNull
    @Column(name = "id_lijst_enum", nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var lijstID: Werklijst

    @NotBlank
    @Column(name = "id_medewerker", nullable = false)
    lateinit var medewerkerID: String

    @Min(AANTAL_PER_PAGINA_MIN.toLong())
    @Max(AANTAL_PER_PAGINA_MAX.toLong())
    @Column(name = "aantal_per_pagina", nullable = false)
    var aantalPerPagina: Int = 0
}
