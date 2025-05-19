/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notities.model

import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import net.atos.zac.util.FlywayIntegrator
import nl.info.zac.util.AllOpen
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Een Notitie kan worden gekoppeld aan een zaak.
 */
@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "notitie")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "notitie_sq",
    sequenceName = "notitie_sq",
    allocationSize = 1
)
@AllOpen
class Notitie {
    companion object {
        /**
         * Naam van property: [Notitie.zaakUUID]
         */
        const val ZAAK_UUID: String = "zaakUUID"
    }

    @Id
    @GeneratedValue(generator = "notitie_sq", strategy = GenerationType.SEQUENCE)
    var id: Long = 0

    @Basic
    @Column(name = "zaak_uuid", updatable = false)
    lateinit var zaakUUID: UUID

    @Column(nullable = false)
    @NotBlank
    lateinit var tekst: String

    @Column(name = "tijdstip_laatste_wijziging", nullable = false)
    lateinit var tijdstipLaatsteWijziging: ZonedDateTime

    @Column(name = "gebruikersnaam_medewerker", nullable = false, updatable = true)
    @NotBlank
    lateinit var gebruikersnaamMedewerker: String
}
