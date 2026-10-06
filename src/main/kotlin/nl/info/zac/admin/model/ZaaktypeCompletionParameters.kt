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
import java.util.UUID

@Entity
@Table(schema = FlywayIntegrator.Companion.SCHEMA, name = "zaaktype_completion_parameters")
@SequenceGenerator(
    schema = FlywayIntegrator.Companion.SCHEMA,
    name = "sq_zaaktype_completion_parameters",
    sequenceName = "sq_zaaktype_completion_parameters",
    allocationSize = 1
)
@AllOpen
class ZaaktypeCompletionParameters : UserModifiableZaaktypeConfigurationData<ZaaktypeCompletionParameters> {
    @Id
    @GeneratedValue(generator = "sq_zaaktype_completion_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    @NotNull
    lateinit var zaaktypeConfiguration: ZaaktypeConfiguration

    @ManyToOne
    @JoinColumn(name = "id_zaakbeeindigreden", referencedColumnName = "id_zaakbeeindigreden")
    @NotNull
    lateinit var zaakbeeindigReden: ZaakbeeindigReden

    @Column(name = "resultaattype_uuid", nullable = false)
    @NotNull
    lateinit var resultaattype: UUID

    @Column(name = "resultaattype_omschrijving")
    var resultaattypeOmschrijving: String? = null

    override fun equals(other: Any?): Boolean {
        if (other !is ZaaktypeCompletionParameters) return false
        return zaakbeeindigReden.id == other.zaakbeeindigReden.id &&
            resultaattype == other.resultaattype
    }

    // Constant per class, because Hibernate adds an element to an eager PersistentSet before it has loaded all its
    // fields (https://hibernate.atlassian.net/browse/HHH-3799), and `applyChanges` modifies elements inside the set.
    // A hash code based on these fields would leave the element in the wrong bucket, so `contains` would miss it.
    override fun hashCode() = javaClass.hashCode()

    override fun applyChanges(changes: ZaaktypeCompletionParameters) {
        resultaattype = changes.resultaattype
        resultaattypeOmschrijving = changes.resultaattypeOmschrijving
    }

    override fun resetId(): ZaaktypeCompletionParameters {
        id = null
        return this
    }
}
