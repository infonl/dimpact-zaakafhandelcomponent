/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin.model

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
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import net.atos.zac.util.FlywayIntegrator
import nl.lifely.zac.util.AllOpen
import java.util.Arrays
import java.util.function.Consumer

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
    lateinit var code: @NotBlank String

    @Column(name = "naam", nullable = false)
    lateinit var naam: @NotBlank String

    @Transient
    final var isSysteem: Boolean? = null
        get() {
            if (field == null) {
                field = Arrays.stream(Systeem.entries.toTypedArray())
                    .anyMatch { value: Systeem -> value.name == code }
            }
            return field
        }
        private set

    @OneToMany(mappedBy = "tabel", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    private val waarden: MutableList<ReferenceTableValue> = ArrayList()

    fun getWaarden(): List<ReferenceTableValue> {
        return waarden.stream()
            .sorted(Comparator.comparingInt { obj: ReferenceTableValue -> obj.volgorde })
            .toList()
    }

    fun setWaarden(waarden: List<ReferenceTableValue>) {
        this.waarden.clear()
        waarden.forEach(Consumer { waarde: ReferenceTableValue -> this.addWaarde(waarde) })
    }

    fun addWaarde(waarde: ReferenceTableValue) {
        waarde.tabel = this
        waarde.volgorde = waarden.size
        waarden.add(waarde)
    }

    fun removeWaarde(waarde: ReferenceTableValue) {
        waarden.remove(waarde)
    }
}
