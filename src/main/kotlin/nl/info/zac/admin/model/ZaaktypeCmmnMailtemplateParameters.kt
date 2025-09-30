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
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.util.AllOpen
import java.util.Objects

@Entity
@Table(schema = SCHEMA, name = "zaaktype_cmmn_mailtemplate_parameters")
@SequenceGenerator(
    schema = SCHEMA,
    name = "sq_zaaktype_cmmn_mailtemplate_parameters",
    sequenceName = "sq_zaaktype_cmmn_mailtemplate_parameters",
    allocationSize = 1
)
@AllOpen
class ZaaktypeCmmnMailtemplateParameters :
    UserModifiable<ZaaktypeCmmnMailtemplateParameters> {

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_mailtemplate_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    @field:NotNull
    var zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration? = null

    @ManyToOne
    @JoinColumn(name = "id_mail_template", referencedColumnName = "id_mail_template")
    @field:NotNull
    var mailTemplate: MailTemplate? = null

    @Suppress("ExceptionRaisedInUnexpectedLocation", "UseCheckOrError")
    override fun equals(other: Any?): Boolean {
        if (other !is ZaaktypeCmmnMailtemplateParameters) return false
        if (mailTemplate == null || other.mailTemplate == null) {
            throw IllegalStateException("mailTemplate is null")
        }
        return Objects.equals(mailTemplate!!.id, other.mailTemplate!!.id)
    }

    @Suppress("ExceptionRaisedInUnexpectedLocation", "UseCheckOrError")
    override fun hashCode(): Int {
        if (mailTemplate == null) {
            throw IllegalStateException("mailTemplate is null")
        }
        return Objects.hash(mailTemplate!!.id)
    }

    @Suppress("UseCheckOrError")
    override fun isModifiedFrom(original: ZaaktypeCmmnMailtemplateParameters): Boolean {
        if (mailTemplate == null || original.mailTemplate == null) {
            throw IllegalStateException("mailTemplate is null")
        }
        return Objects.equals(mailTemplate!!.mail, original.mailTemplate!!.mail) &&
            !Objects.equals(mailTemplate!!.id, original.mailTemplate!!.id)
    }

    override fun applyChanges(changes: ZaaktypeCmmnMailtemplateParameters) {
        mailTemplate = changes.mailTemplate
    }

    override fun resetId(): ZaaktypeCmmnMailtemplateParameters {
        id = null
        return this
    }
}
