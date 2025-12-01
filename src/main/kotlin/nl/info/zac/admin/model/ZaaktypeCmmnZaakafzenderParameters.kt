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
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_cmmn_zaakafzender_parameters")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_zaaktype_cmmn_zaakafzender_parameters",
    sequenceName = "sq_zaaktype_cmmn_zaakafzender_parameters",
    allocationSize = 1
)
@AllOpen
class ZaaktypeCmmnZaakafzenderParameters : UserModifiable<ZaaktypeCmmnZaakafzenderParameters> {

    enum class SpecialMail {
        GEMEENTE,
        MEDEWERKER;

        fun name(name: String): Boolean = this.name == name
    }

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_zaakafzender_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    @field:NotNull
    lateinit var zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration

    @Column(name = "default_mail", nullable = false)
    var defaultMail: Boolean = false

    @Column(name = "mail", nullable = false)
    @field:NotBlank
    lateinit var mail: String

    @Column(name = "replyto")
    @field:NotBlank
    lateinit var replyTo: String

    override fun isModifiedFrom(original: ZaaktypeCmmnZaakafzenderParameters): Boolean {
        return Objects.equals(mail, original.mail) && (
            !defaultMail == original.defaultMail ||
                !Objects.equals(replyTo, original.replyTo)
            )
    }

    override fun applyChanges(changes: ZaaktypeCmmnZaakafzenderParameters) {
        this.defaultMail = changes.defaultMail
        this.replyTo = changes.replyTo
    }

    override fun resetId(): ZaaktypeCmmnZaakafzenderParameters {
        id = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ZaaktypeCmmnZaakafzenderParameters) return false
        return mail == other.mail && defaultMail == other.defaultMail && Objects.equals(replyTo, other.replyTo)
    }

    override fun hashCode(): Int {
        return Objects.hash(mail, defaultMail, replyTo)
    }
}
