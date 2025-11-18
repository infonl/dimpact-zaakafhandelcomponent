/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_bpmn_configuration")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_zaaktype_bpmn_configuration",
    sequenceName = "sq_zaaktype_bpmn_configuration",
    allocationSize = 1
)
@DiscriminatorValue("BPMN")
@PrimaryKeyJoinColumn(name = "zaaktype_configuration_id")
@AllOpen
class ZaaktypeBpmnConfiguration : ZaaktypeConfiguration() {
    @Id
    @GeneratedValue(generator = "sq_zaaktype_bpmn_configuration", strategy = GenerationType.SEQUENCE)
    override var id: Long? = null

    @NotBlank
    @Column(name = "bpmn_process_definition_key")
    lateinit var bpmnProcessDefinitionKey: String

    override fun getConfigurationType() = Companion.ZaakTypeConfigurationType.BPMN
}
