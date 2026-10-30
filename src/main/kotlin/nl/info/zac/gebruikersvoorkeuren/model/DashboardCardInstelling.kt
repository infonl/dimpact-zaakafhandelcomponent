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
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.signalering.model.SignaleringType
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "dashboard_card")
@SequenceGenerator(schema = FlywayIntegrator.SCHEMA, name = "sq_dashboard_card", sequenceName = "sq_dashboard_card", allocationSize = 1)
@AllOpen
class DashboardCardInstelling {
    @Id
    @GeneratedValue(generator = "sq_dashboard_card", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_dashboard_card")
    var id: Long? = null

    @NotBlank
    @Column(name = "id_medewerker", nullable = false)
    lateinit var medewerkerId: String

    @NotNull
    @Column(name = "dashboard_card_enum", nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var cardId: DashboardCardId

    @Transient
    var signaleringType: SignaleringType.Type? = null

    @NotNull
    @Column(name = "kolom", nullable = false)
    var kolom: Int = 0

    @NotNull
    @Column(name = "volgorde", nullable = false)
    var volgorde: Int = 0
}
