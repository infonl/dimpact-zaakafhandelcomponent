/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.Companion.SCHEMA, name = "bpmn_procesdefinitie_taakformulieren")
@SequenceGenerator(
    schema = FlywayIntegrator.Companion.SCHEMA,
    name = "sq_bpmn_procesdefinitie_taakformulieren",
    sequenceName = "sq_bpmn_procesdefinitie_taakformulieren",
    allocationSize = 1
)
@AllOpen
class BpmnProcessDefinitionTaskForm {

    @Id
    @GeneratedValue(generator = "sq_bpmn_procesdefinitie_taakformulieren", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long = 0

    @NotBlank
    @Column(name = "bpmn_procesdefinitie", nullable = false)
    lateinit var bpmnProcessDefinitionKey: String

    @Column(name = "bpmn_procesdefinitie_versie")
    var bpmnProcessDefinitionVersion: Int = 0

    @NotBlank
    @Column(name = "naam", nullable = false)
    lateinit var name: String

    @Column(name = "titel", nullable = false)
    lateinit var title: String

    @NotBlank
    @Column(name = "bestandsnaam", nullable = false)
    lateinit var filename: String

    @NotBlank
    @Column(nullable = false)
    lateinit var content: String
}
