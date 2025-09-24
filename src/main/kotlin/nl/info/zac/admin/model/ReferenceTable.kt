/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

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
import nl.info.zac.app.admin.model.RestReferenceTable
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "referentie_tabel")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_referentie_tabel",
    sequenceName = "sq_referentie_tabel",
    allocationSize = 1
)
@AllOpen
class ReferenceTable {
    /**
     * Defines the list of 'system reference tables'.
     * Make sure to keep this list in sync with the 'is_systeem_tabel' column in the database.
     */
    enum class SystemReferenceTable {
        ADVIES,
        AFZENDER,
        COMMUNICATIEKANAAL,
        DOMEIN,
        SERVER_ERROR_ERROR_PAGINA_TEKST,
        BRP_DOELBINDING_ZOEK_WAARDE,
        BRP_DOELBINDING_RAADPLEEG_WAARDE,
    }

    @Id
    @GeneratedValue(generator = "sq_referentie_tabel", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_referentie_tabel")
    var id: Long? = null

    @Column(name = "code", nullable = false)
    @NotBlank
    lateinit var code: String

    @Column(name = "naam", nullable = false)
    @NotBlank
    lateinit var name: String

    @Column(name = "is_systeem_tabel", nullable = false)
    var isSystemReferenceTable: Boolean = false

    @OneToMany(mappedBy = "referenceTable", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    var values: MutableList<ReferenceTableValue> = ArrayList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReferenceTable) return false

        if (isSystemReferenceTable != other.isSystemReferenceTable) return false
        if (code != other.code) return false
        if (name != other.name) return false
        if (!values.toTypedArray().contentDeepEquals(other.values.toTypedArray())) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isSystemReferenceTable.hashCode()
        result = 31 * result + code.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + values.hashCode()
        return result
    }
}

fun ReferenceTable.toRestReferenceTable(inclusiefWaarden: Boolean): RestReferenceTable {
    val restReferenceTableValues = if (inclusiefWaarden) {
        this.values.map { it.toRestReferenceTableValue() }
    } else {
        emptyList()
    }
    return RestReferenceTable(
        this.id!!,
        this.code,
        this.name,
        this.isSystemReferenceTable,
        this.values.size,
        restReferenceTableValues
    )
}
