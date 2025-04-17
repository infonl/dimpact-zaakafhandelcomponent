/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.formulieren.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import net.atos.zac.util.FlywayIntegrator
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "formulier_definitie")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_formulier_definitie",
    sequenceName = "sq_formulier_definitie",
    allocationSize = 1
)
@AllOpen
@NoArgConstructor
class FormulierDefinitie {
    @Id
    @GeneratedValue(generator = "sq_formulier_definitie", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_formulier_definitie")
    var id: Long? = null

    @Column(name = "systeemnaam", nullable = false, unique = true)
    @field:NotBlank
    var systeemnaam: String? = null

    @Column(name = "naam", nullable = false)
    @field:NotBlank
    var naam: String? = null

    @Column(name = "beschrijving")
    var beschrijving: String? = null

    @Column(name = "uitleg")
    var uitleg: String? = null

    @Column(name = "creatiedatum", nullable = false)
    var creatiedatum: ZonedDateTime? = null

    @Column(name = "wijzigingsdatum", nullable = false)
    var wijzigingsdatum: ZonedDateTime? = null

    @OneToMany(
        mappedBy = "formulierDefinitie",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var veldDefinities: MutableSet<FormulierVeldDefinitie> = mutableSetOf()

    fun getVeldDefinities() = veldDefinities

    fun setVeldDefinities(veldDefinities: Collection<FormulierVeldDefinitie>) {
        this.veldDefinities.clear()
        veldDefinities.forEach { addVeldDefinitie(it) }
    }

    private fun addVeldDefinitie(veldDefinitie: FormulierVeldDefinitie) {
        veldDefinitie.formulierDefinitie = this
        veldDefinities.add(veldDefinitie)
    }
}
