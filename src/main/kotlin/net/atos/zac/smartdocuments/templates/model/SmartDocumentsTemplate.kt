/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.templates.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import net.atos.zac.admin.model.ZaakafhandelParametersSummary
import net.atos.zac.util.FlywayIntegrator
import nl.lifely.zac.util.AllOpen
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "smartdocuments_document_creatie_sjabloon")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_sd_document_creatie_sjabloon",
    sequenceName = "sq_sd_document_creatie_sjabloon",
    allocationSize = 1
)
@AllOpen
class SmartDocumentsTemplate {
    @Id
    @GeneratedValue(generator = "sq_sd_document_creatie_sjabloon", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_sjabloon")
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

    @ManyToOne
    @JoinColumn(name = "zaakafhandelparameters_id", nullable = false)
    lateinit var zaakafhandelParameters: ZaakafhandelParametersSummary

    @Column(name = "informatie_object_type_uuid", nullable = false)
    lateinit var informatieObjectTypeUUID: UUID
}
