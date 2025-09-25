/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments.templates.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import net.atos.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_cmmn_smartdocuments_document_template_parameters")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_zaaktype_cmmn_smartdocuments_document_template_parameters",
    sequenceName = "sq_zaaktype_cmmn_smartdocuments_document_template_parameters",
    allocationSize = 1
)
@AllOpen
class SmartDocumentsTemplate {
    @Id
    @GeneratedValue(
        generator = "sq_zaaktype_cmmn_smartdocuments_document_template_parameters",
        strategy = GenerationType.SEQUENCE
    )
    @Column(name = "id")
    var id: Long = 0

    @Column(name = "smartdocuments_id", nullable = false)
    lateinit var smartDocumentsId: String

    @Column(name = "naam", nullable = false)
    lateinit var name: String

    @Column(name = "aanmaakdatum", nullable = false)
    lateinit var creationDate: ZonedDateTime

    @ManyToOne
    @JoinColumn(name = "sjabloon_groep_id", nullable = false)
    lateinit var templateGroup: SmartDocumentsTemplateGroup

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zaaktype_configuration_id", nullable = false)
    lateinit var zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration

    @Column(name = "informatie_object_type_uuid", nullable = false)
    lateinit var informatieObjectTypeUUID: UUID
}
