/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.formio.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import net.atos.zac.util.FlywayIntegrator
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "formio_formulier")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_formio_formulier",
    sequenceName = "sq_formio_formulier",
    allocationSize = 1
)
@AllOpen
class FomioFormulier {

    @Id
    @GeneratedValue(generator = "sq_formio_formulier", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_formio_formulier")
    var id: Long = 0

    @NotBlank
    lateinit var name: String

    @NotNull
    lateinit var title: String

    @NotBlank
    lateinit var filename: String

    @NotBlank
    lateinit var content: String
}
