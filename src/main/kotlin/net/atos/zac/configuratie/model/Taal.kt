/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.configuratie.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import net.atos.zac.util.FlywayIntegrator
import nl.lifely.zac.util.AllOpen

/**
 * These are ISO 639-2/B language codes.
 * See https://en.wikipedia.org/wiki/List_of_ISO_639-2_codes (when there are two codes the * indicates the B-code)
 */
@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "taal")
@SequenceGenerator(schema = FlywayIntegrator.SCHEMA, name = "sq_taal", sequenceName = "sq_taal", allocationSize = 1)
@AllOpen
class Taal {
    @Id
    @GeneratedValue(generator = "sq_taal", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_taal")
    var id: Long = 0

    @Column(name = "code", nullable = false)
    @NotBlank
    lateinit var code: String

    @Column(name = "naam", nullable = false)
    @NotBlank
    lateinit var naam: String

    @Column(name = "name", nullable = false)
    @NotBlank
    lateinit var name: String

    @Column(name = "native", nullable = false)
    @NotBlank
    lateinit var local: String
}
