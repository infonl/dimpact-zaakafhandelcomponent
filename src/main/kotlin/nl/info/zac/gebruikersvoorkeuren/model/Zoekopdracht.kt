/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
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
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen
import java.time.ZonedDateTime

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "zoekopdracht")
@SequenceGenerator(schema = FlywayIntegrator.SCHEMA, name = "sq_zoekopdracht", sequenceName = "sq_zoekopdracht", allocationSize = 1)
@AllOpen
class Zoekopdracht {
    @Id
    @GeneratedValue(generator = "sq_zoekopdracht", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_zoekopdracht")
    var id: Long? = null

    @NotNull
    @Column(name = "creatiedatum", nullable = false)
    lateinit var creatiedatum: ZonedDateTime

    @NotBlank
    @Column(name = "naam", nullable = false)
    lateinit var naam: String

    @NotNull
    @Column(name = "id_lijst", nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var lijstID: Werklijst

    @Column(name = "actief")
    var isActief: Boolean = false

    @NotBlank
    @Column(name = "id_medewerker", nullable = false)
    lateinit var medewerkerID: String

    @NotBlank
    @Column(name = "json_zoekopdracht", nullable = false)
    lateinit var json: String
}
