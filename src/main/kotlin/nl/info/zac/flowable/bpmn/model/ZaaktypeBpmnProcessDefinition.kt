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
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaaktype_bpmn_process_definition")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_zaaktype_bpmn_process_definition",
    sequenceName = "sq_zaaktype_bpmn_process_definition",
    allocationSize = 1
)
@AllOpen
class ZaaktypeBpmnProcessDefinition {
    companion object {
        const val PRODUCTAANVRAAGTTYPE_VARIABELE_NAME = "productaanvraagtype"
        const val ZAAKTYPE_UUID_VARIABLE_NAME = "zaaktypeUuid"
    }

    @Id
    @GeneratedValue(generator = "sq_zaaktype_bpmn_process_definition", strategy = GenerationType.SEQUENCE)
    var id: Long = 0

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
     * Optional name of the group to assign when starting a BPMN process via the Dimpact productaanvraag flow.
     * When a BPMN process is started from the UI, the user selects the group; but when it is started from
     * [nl.info.zac.notification.NotificationReceiver], there is no selection, so this value is used.
     * If null, the productaanvraag flow fails to create a BPMN process.
     */
    @Column(name = "naam_groep")
    var groepNaam: String? = null
}
