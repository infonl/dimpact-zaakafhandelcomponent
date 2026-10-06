/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen

/**
 * The process engine that runs the zaken of a zaaktype version, with the definition in that engine: the case
 * definition for CMMN, the process definition key for BPMN.
 */
@Entity
@Table(schema = SCHEMA, name = "zaaktype_process_binding")
@SequenceGenerator(
    schema = SCHEMA,
    name = "sq_zaaktype_process_binding",
    sequenceName = "sq_zaaktype_process_binding",
    allocationSize = 1
)
@AllOpen
class ZaaktypeProcessBinding {
    companion object {
        val DEFINITION_KEY_VARIABLE_NAME = ZaaktypeProcessBinding::definitionKey.name
        val PROCESS_ENGINE_VARIABLE_NAME = ZaaktypeProcessBinding::processEngine.name
    }

    @Id
    @GeneratedValue(generator = "sq_zaaktype_process_binding", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @OneToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id", nullable = false)
    lateinit var zaaktypeConfiguration: ZaaktypeConfiguration

    @Enumerated(EnumType.STRING)
    @Column(name = "process_engine", nullable = false)
    lateinit var processEngine: ProcessEngine

    @field:NotBlank
    @Column(name = "definition_key", nullable = false)
    lateinit var definitionKey: String
}
