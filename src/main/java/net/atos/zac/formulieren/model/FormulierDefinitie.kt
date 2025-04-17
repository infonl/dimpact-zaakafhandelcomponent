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
import java.time.ZonedDateTime
import java.util.function.Consumer

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "formulier_definitie")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_formulier_definitie",
    sequenceName = "sq_formulier_definitie",
    allocationSize = 1
)
class FormulierDefinitie {
    @JvmField
    @Id
    @GeneratedValue(generator = "sq_formulier_definitie", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_formulier_definitie")
    var id: Long? = null

    @JvmField
    @Column(name = "systeemnaam", nullable = false, unique = true)
    var systeemnaam: @NotBlank String? = null

    @JvmField
    @Column(name = "naam", nullable = false)
    var naam: @NotBlank String? = null

    @JvmField
    @Column(name = "beschrijving")
    var beschrijving: String? = null

    @JvmField
    @Column(name = "uitleg")
    var uitleg: String? = null

    @JvmField
    @Column(name = "creatiedatum", nullable = false)
    var creatiedatum: ZonedDateTime? = null

    @JvmField
    @Column(name = "wijzigingsdatum", nullable = false)
    var wijzigingsdatum: ZonedDateTime? = null

    @OneToMany(
        mappedBy = "formulierDefinitie",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var veldDefinities: MutableSet<FormulierVeldDefinitie?>? = null


    fun getVeldDefinities(): MutableSet<FormulierVeldDefinitie?> {
        return (if (veldDefinities != null) veldDefinities else kotlin.collections.mutableSetOf<net.atos.zac.formulieren.model.FormulierVeldDefinitie?>())!!
    }

    fun setVeldDefinities(veldDefinities: MutableCollection<FormulierVeldDefinitie?>) {
        if (this.veldDefinities == null) {
            this.veldDefinities = HashSet<FormulierVeldDefinitie?>()
        } else {
            this.veldDefinities!!.clear()
        }
        veldDefinities.forEach(Consumer { veldDefinitie: FormulierVeldDefinitie? -> this.addVeldDefinitie(veldDefinitie!!) })
    }

    private fun addVeldDefinitie(veldDefinitie: FormulierVeldDefinitie) {
        veldDefinitie.setFormulierDefinitie(this)
        veldDefinities!!.add(veldDefinitie)
    }
}
