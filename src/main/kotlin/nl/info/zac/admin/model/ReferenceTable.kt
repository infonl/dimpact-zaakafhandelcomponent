/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
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
import net.atos.zac.app.admin.model.RestReferenceTable
import net.atos.zac.util.FlywayIntegrator
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
    enum class Systeem {
        ADVIES,
        AFZENDER,
        COMMUNICATIEKANAAL,
        DOMEIN,
        SERVER_ERROR_ERROR_PAGINA_TEKST
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
