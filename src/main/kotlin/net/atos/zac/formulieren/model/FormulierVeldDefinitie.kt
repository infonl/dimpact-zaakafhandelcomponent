/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.formulieren.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "formulier_veld_definitie")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_formulier_veld_definitie",
    sequenceName = "sq_formulier_veld_definitie",
    allocationSize = 1
)
@AllOpen
@NoArgConstructor
class FormulierVeldDefinitie {
    @Id
    @GeneratedValue(generator = "sq_formulier_veld_definitie", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_formulier_veld_definitie")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "id_formulier_definitie", referencedColumnName = "id_formulier_definitie")
    @field:NotNull
    var formulierDefinitie: FormulierDefinitie? = null

    @Column(name = "systeemnaam", nullable = false, unique = true)
    @field:NotBlank
    var systeemnaam: String? = null

    @Column(name = "volgorde", nullable = false)
    @field:PositiveOrZero
    var volgorde: Int = 0

    @Column(name = "label", nullable = false)
    @field:NotBlank
    var label: String? = null

    @Column(name = "veldtype", nullable = false)
    @Enumerated(EnumType.STRING)
    @field:NotNull
    var veldtype: FormulierVeldtype? = null

    @Column(name = "beschrijving")
    var beschrijving: String? = null

    @Column(name = "helptekst")
    var helptekst: String? = null

    @Column(name = "verplicht")
    var isVerplicht: Boolean = false

    @Column(name = "default_waarde")
    var defaultWaarde: String? = null

    @Column(name = "meerkeuze_opties")
    var meerkeuzeOpties: String? = null

    @Column(name = "validaties")
    var validaties: String? = null
}
