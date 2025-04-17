/*
 * SPDX-FileCopyrightText: 2023 Atos
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
import net.atos.zac.util.FlywayIntegrator
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
    var formulierDefinitie: @NotNull FormulierDefinitie? = null

    @Column(name = "systeemnaam", nullable = false, unique = true)
    var systeemnaam: @NotBlank String? = null

    @Column(name = "volgorde", nullable = false)
    var volgorde: @PositiveOrZero Int = 0

    @Column(name = "label", nullable = false)
    var label: @NotBlank String? = null

    @Column(name = "veldtype", nullable = false)
    @Enumerated(EnumType.STRING)
    var veldtype: @NotNull FormulierVeldtype? = null

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
