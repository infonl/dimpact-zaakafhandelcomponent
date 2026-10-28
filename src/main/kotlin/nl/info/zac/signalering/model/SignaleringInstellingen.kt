/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = SCHEMA, name = "signalering_instellingen")
@SequenceGenerator(
    schema = SCHEMA,
    name = "sq_signalering_instellingen",
    sequenceName = "sq_signalering_instellingen",
    allocationSize = 1
)
@AllOpen
class SignaleringInstellingen() {
    @Id
    @GeneratedValue(generator = "sq_signalering_instellingen", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_signalering_instellingen")
    var id: Long? = null

    @NotNull
    @ManyToOne
    @JoinColumn(name = "signaleringtype_enum", referencedColumnName = "signaleringtype_enum", nullable = false)
    lateinit var type: SignaleringType

    @Column(name = "id_groep", nullable = false)
    var groep: String? = null

    @Column(name = "id_medewerker", nullable = false)
    var medewerker: String? = null

    @Column(name = "dashboard")
    var isDashboard: Boolean = false

    @Column(name = "mail")
    var isMail: Boolean = false

    constructor(type: SignaleringType, ownerType: SignaleringTarget, ownerId: String) : this() {
        this.type = type
        when (ownerType) {
            SignaleringTarget.GROUP -> groep = ownerId
            SignaleringTarget.USER -> medewerker = ownerId
        }
    }

    val ownerType: SignaleringTarget?
        get() = when {
            groep != null -> SignaleringTarget.GROUP
            medewerker != null -> SignaleringTarget.USER
            else -> null
        }

    val isEmpty: Boolean
        get() = !isDashboard && !isMail

    @get:AssertTrue(message = "Of medewerker of groep moet ingevuld zijn (niet beide).")
    val isValid: Boolean
        get() = !medewerker.isNullOrBlank() != !groep.isNullOrBlank()

    override fun toString() = "$type-signaleringinstellingen voor ${groep ?: medewerker}"
}
