/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(schema = SCHEMA, name = "zaaktype_configuration")
@SequenceGenerator(
    schema = SCHEMA,
    name = "zaaktype_generator",
    sequenceName = "sq_zaaktype_configuration",
    allocationSize = 1
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "configuration_type", discriminatorType = DiscriminatorType.STRING)
@AllOpen
abstract class ZaaktypeConfiguration {
    companion object {
        enum class ZaaktypeConfigurationType { UNKNOWN, CMMN, BPMN }

        val PRODUCTAANVRAAGTYPE_VARIABLE_NAME = ZaaktypeConfiguration::productaanvraagtype.name
        val ZAAKTYPE_UUID_VARIABLE_NAME = ZaaktypeConfiguration::zaakTypeUUID.name
        val ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME = ZaaktypeConfiguration::zaaktypeOmschrijving.name
        val CREATIEDATUM_VARIABLE_NAME = ZaaktypeConfiguration::creatiedatum.name
    }

    @Id
    @GeneratedValue(generator = "zaaktype_generator", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    // Nullable to avoid lateinit init errors; DB NOT NULL—set before persist.
    @Column(name = "zaaktype_uuid", nullable = false)
    var zaakTypeUUID: UUID? = null

    @NotBlank
    @Column(name = "zaaktype_omschrijving", nullable = false)
    lateinit var zaaktypeOmschrijving: String

    /**
     * This field is nullable because when a new zaaktype is published,
     * ZAC creates an initial 'inactive' zaaktypeCmmnConfiguration record without a value.
     * For 'active' zaaktypeCmmnConfiguration, however, this field becomes mandatory is never null.
     */
    @Column(name = "groep_id")
    var groepID: String? = null

    // Nullable to avoid lateinit init errors; DB NOT NULL—set before persist.
    @Column(name = "creatiedatum", nullable = false)
    var creatiedatum: ZonedDateTime? = null

    @Column(name = "productaanvraagtype")
    var productaanvraagtype: String? = null

    @Column(name = "domein")
    var domein: String? = null

    abstract fun getConfigurationType(): ZaaktypeConfigurationType
}
