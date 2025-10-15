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
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_cmmn_email_parameters")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_zaaktype_cmmn_email_parameters",
    sequenceName = "sq_zaaktype_cmmn_email_parameters",
    allocationSize = 1
)
@AllOpen
class ZaaktypeCmmnEmailParameters {

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_email_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @OneToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id", nullable = false)
    var zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration? = null

    @Column(name = "enabled")
    var enabled: Boolean = false

    @Column(name = "template_name")
    var templateName: String? = null

    @Column(name = "email_sender")
    var emailSender: String? = null

    @Column(name = "email_reply")
    var emailReply: String? = null
}
