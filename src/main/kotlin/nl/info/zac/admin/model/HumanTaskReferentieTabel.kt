/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
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
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = SCHEMA, name = "humantask_referentie_tabel")
@SequenceGenerator(
    schema = SCHEMA,
    name = "sq_humantask_referentie_tabel",
    sequenceName = "sq_humantask_referentie_tabel",
    allocationSize = 1
)
@AllOpen
class HumanTaskReferentieTabel() {

    constructor(veld: String, tabel: ReferenceTable) : this() {
        this.veld = veld
        this.tabel = tabel
    }

    @Id
    @GeneratedValue(generator = "sq_humantask_referentie_tabel", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_humantask_referentie_tabel")
    var id: Long? = null

    @field:NotNull
    @ManyToOne
    @JoinColumn(name = "id_referentie_tabel", referencedColumnName = "id_referentie_tabel")
    var tabel: ReferenceTable? = null

    @field:NotNull
    @ManyToOne
    @JoinColumn(name = "id_humantask_parameters", referencedColumnName = "id")
    var humantask: ZaaktypeCmmnHumantaskParameters? = null

    @field:NotBlank
    @Column(name = "veld", nullable = false)
    var veld: String? = null

    override fun equals(other: Any?): Boolean {
        if (other !is HumanTaskReferentieTabel) return false
        return tabel == other.tabel && veld == other.veld
    }

    override fun hashCode(): Int {
        var result = tabel?.hashCode() ?: 0
        result = 31 * result + (veld?.hashCode() ?: 0)
        return result
    }
}
