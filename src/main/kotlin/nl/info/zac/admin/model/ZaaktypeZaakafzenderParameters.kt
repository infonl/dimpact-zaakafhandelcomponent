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
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen
import java.util.Objects

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_zaakafzender_parameters")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_zaaktype_zaakafzender_parameters",
    sequenceName = "sq_zaaktype_zaakafzender_parameters",
    allocationSize = 1
)
@AllOpen
class ZaaktypeZaakafzenderParameters : UserModifiableZaaktypeConfigurationData<ZaaktypeZaakafzenderParameters> {

    enum class SpecialMail {
        GEMEENTE,
        MEDEWERKER;

        fun name(name: String): Boolean = this.name == name
    }

    @Id
    @GeneratedValue(generator = "sq_zaaktype_zaakafzender_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    @field:NotNull
    lateinit var zaaktypeConfiguration: ZaaktypeConfiguration

    @Column(name = "default_mail", nullable = false)
    var isDefaultMail: Boolean = false

    @Column(name = "mail", nullable = false)
    @field:NotBlank
    lateinit var mail: String

    @Column(name = "replyto")
    var replyTo: String? = null

    override fun isModifiedFrom(original: ZaaktypeZaakafzenderParameters): Boolean {
        return Objects.equals(mail, original.mail) && (
            !isDefaultMail == original.isDefaultMail ||
                !Objects.equals(replyTo, original.replyTo)
            )
    }

    override fun applyChanges(changes: ZaaktypeZaakafzenderParameters) {
        this.isDefaultMail = changes.isDefaultMail
        this.replyTo = changes.replyTo
    }

    override fun resetId(): ZaaktypeZaakafzenderParameters {
        id = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ZaaktypeZaakafzenderParameters) return false
        return mail == other.mail && isDefaultMail == other.isDefaultMail && Objects.equals(replyTo, other.replyTo)
    }

    // Constant per class, because Hibernate adds an element to an eager PersistentSet before it has loaded all its
    // fields (https://hibernate.atlassian.net/browse/HHH-3799), and `applyChanges` modifies elements inside the set.
    // A hash code based on these fields would leave the element in the wrong bucket, so `contains` would miss it.
    override fun hashCode() = javaClass.hashCode()
}
