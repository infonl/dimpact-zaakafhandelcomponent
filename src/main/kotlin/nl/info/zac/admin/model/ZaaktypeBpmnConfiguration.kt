/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_bpmn_configuration")
@DiscriminatorValue("BPMN")
@PrimaryKeyJoinColumn(name = "id")
@AllOpen
class ZaaktypeBpmnConfiguration : ZaaktypeConfiguration() {
    @NotBlank
    @Column(name = "bpmn_process_definition_key")
    lateinit var bpmnProcessDefinitionKey: String

    override fun getConfigurationType() = Companion.ZaakTypeConfigurationType.BPMN
}
