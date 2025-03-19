/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import net.atos.zac.util.FlywayIntegrator
import nl.info.zac.util.AllOpen
import java.util.UUID

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_bpmn_process_definition")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_zaaktype_bpmn_process_definition",
    sequenceName = "sq_zaaktype_bpmn_process_definition",
    allocationSize = 1
)
@AllOpen
class ZaaktypeBpmnProcessDefinition {
    @Id
    @GeneratedValue(generator = "sq_zaaktype_bpmn_process_definition", strategy = GenerationType.SEQUENCE)
    var id: Long = 0

    @NotBlank
    @Column(name = "zaaktype_uuid")
    lateinit var zaaktypeUuid: UUID

    @NotBlank
    @Column(name = "bpmn_process_definition_key")
    lateinit var bpmnProcessDefinitionKey: String
}
