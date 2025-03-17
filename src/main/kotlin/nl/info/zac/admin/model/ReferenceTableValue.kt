/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import net.atos.zac.app.admin.model.RestReferenceTableValue
import net.atos.zac.util.FlywayIntegrator
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "referentie_waarde")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_referentie_waarde",
    sequenceName = "sq_referentie_waarde",
    allocationSize = 1
)
@AllOpen
class ReferenceTableValue {
    @Id
    @GeneratedValue(generator = "sq_referentie_waarde", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_referentie_waarde")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "id_referentie_tabel", referencedColumnName = "id_referentie_tabel")
    lateinit var referenceTable: ReferenceTable

    @Column(name = "naam", nullable = false)
    @NotBlank
    lateinit var name: String

    @Column(name = "volgorde", nullable = false)
    var sortOrder: Int = 0

    @Column(name = "is_systeem_waarde", nullable = false)
    var isSystemValue: Boolean = false
}

fun ReferenceTableValue.toRestReferenceTableValue() =
    RestReferenceTableValue(
        id = this.id!!,
        naam = this.name,
        isSystemValue = this.isSystemValue
    )
