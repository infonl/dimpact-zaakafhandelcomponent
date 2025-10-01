/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
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
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen
import java.util.Objects
import java.util.UUID

@Entity
@Table(schema = FlywayIntegrator.Companion.SCHEMA, name = "zaaktype_cmmn_completion_parameters")
@SequenceGenerator(
    schema = FlywayIntegrator.Companion.SCHEMA,
    name = "sq_zaaktype_cmmn_completion_parameters",
    sequenceName = "sq_zaaktype_cmmn_completion_parameters",
    allocationSize = 1
)
@AllOpen
class ZaaktypeCmmnCompletionParameters : UserModifiable<ZaaktypeCmmnCompletionParameters> {
    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_completion_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    @NotNull
    lateinit var zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration

    @ManyToOne
    @JoinColumn(name = "id_zaakbeeindigreden", referencedColumnName = "id_zaakbeeindigreden")
    @NotNull
    lateinit var zaakbeeindigReden: ZaakbeeindigReden

    @Column(name = "resultaattype_uuid", nullable = false)
    @NotNull
    lateinit var resultaattype: UUID

    override fun equals(other: Any?): Boolean {
        if (other !is ZaaktypeCmmnCompletionParameters) return false
        return zaakbeeindigReden.id == other.zaakbeeindigReden.id &&
            resultaattype == other.resultaattype
    }

    override fun hashCode(): Int {
        checkNotNull(zaakbeeindigReden) { "zaakbeeindigReden is null" }
        return Objects.hash(zaakbeeindigReden.id, resultaattype)
    }

    override fun isModifiedFrom(original: ZaaktypeCmmnCompletionParameters): Boolean {
        return zaakbeeindigReden == original.zaakbeeindigReden && resultaattype != original.resultaattype
    }

    override fun applyChanges(changes: ZaaktypeCmmnCompletionParameters) {
        resultaattype = changes.resultaattype
    }

    override fun resetId(): ZaaktypeCmmnCompletionParameters {
        id = null
        return this
    }
}
