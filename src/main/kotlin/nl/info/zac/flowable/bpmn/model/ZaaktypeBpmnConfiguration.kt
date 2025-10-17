/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
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
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen
import java.util.UUID

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_bpmn_configuration")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_zaaktype_bpmn_configuration",
    sequenceName = "sq_zaaktype_bpmn_configuration",
    allocationSize = 1
)
@AllOpen
class ZaaktypeBpmnConfiguration {
    companion object {
        const val PRODUCTAANVRAAGTYPE_VARIABLE_NAME = "productaanvraagtype"
        const val ZAAKTYPE_UUID_VARIABLE_NAME = "zaaktypeUuid"
    }

    @Id
    @GeneratedValue(generator = "sq_zaaktype_bpmn_configuration", strategy = GenerationType.SEQUENCE)
    var id: Long? = null

    @NotNull
    @Column(name = "zaaktype_uuid")
    lateinit var zaaktypeUuid: UUID

    @NotBlank
    @Column(name = "bpmn_process_definition_key")
    lateinit var bpmnProcessDefinitionKey: String

    @Column(name = "zaaktype_omschrijving")
    lateinit var zaaktypeOmschrijving: String

    @Column(name = "productaanvraagtype")
    var productaanvraagtype: String? = null

    /**
     * Name of the user group that will be assigned by default to BPMN zaken started for this zaaktype.
     * Note that the group name acts as the unique group ID. Both terms are used interchangeably.
     */
    @Column(name = "group_id", nullable = false)
    lateinit var groupId: String
}
