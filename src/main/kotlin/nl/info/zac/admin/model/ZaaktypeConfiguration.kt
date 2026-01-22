/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
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
        enum class ZaaktypeConfigurationType { CMMN, BPMN }

        val PRODUCTAANVRAAGTYPE_VARIABLE_NAME = ZaaktypeConfiguration::productaanvraagtype.name
        val ZAAKTYPE_UUID_VARIABLE_NAME = ZaaktypeConfiguration::zaaktypeUuid.name
        val ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME = ZaaktypeConfiguration::zaaktypeOmschrijving.name
        val CREATIEDATUM_VARIABLE_NAME = ZaaktypeConfiguration::creatiedatum.name
    }

    @Id
    @GeneratedValue(generator = "zaaktype_generator", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @field:NotNull
    @Column(name = "zaaktype_uuid", nullable = false)
    lateinit var zaaktypeUuid: UUID

    @field:NotBlank
    @Column(name = "zaaktype_omschrijving", nullable = false)
    lateinit var zaaktypeOmschrijving: String

    @field:NotBlank
    @Column(name = "groep_id", nullable = false)
    var groepID: String? = null

    @field:NotNull
    @Column(name = "creatiedatum", nullable = false)
    var creatiedatum: ZonedDateTime? = null

    @Column(name = "productaanvraagtype")
    var productaanvraagtype: String? = null

    @Column(name = "domein")
    var domein: String? = null

    @Column(name = "niet_ontvankelijk_resultaattype_uuid")
    var nietOntvankelijkResultaattype: UUID? = null

    @OneToOne(
        mappedBy = "zaaktypeConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var zaaktypeBetrokkeneParameters: ZaaktypeBetrokkeneParameters? = null

    @OneToOne(
        mappedBy = "zaaktypeConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var zaaktypeBrpParameters: ZaaktypeBrpParameters? = null

    abstract fun getConfigurationType(): ZaaktypeConfigurationType

    fun getBetrokkeneParameters(): ZaaktypeBetrokkeneParameters =
        zaaktypeBetrokkeneParameters ?: ZaaktypeBetrokkeneParameters()

    fun getBrpParameters(): ZaaktypeBrpParameters =
        zaaktypeBrpParameters ?: ZaaktypeBrpParameters()

    fun mapBetrokkeneKoppelingen(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration
    ) = newZaaktypeConfiguration.apply {
        zaaktypeBetrokkeneParameters = ZaaktypeBetrokkeneParameters().apply {
            zaaktypeConfiguration = newZaaktypeConfiguration
            brpKoppelen = previousZaaktypeConfiguration.zaaktypeBetrokkeneParameters?.brpKoppelen
            kvkKoppelen = previousZaaktypeConfiguration.zaaktypeBetrokkeneParameters?.kvkKoppelen
        }
    }

    fun mapBrpDoelbindingen(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration
    ) = newZaaktypeConfiguration.apply {
        zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
            zaaktypeConfiguration = newZaaktypeConfiguration
            zoekWaarde = previousZaaktypeConfiguration.zaaktypeBrpParameters?.zoekWaarde
            raadpleegWaarde = previousZaaktypeConfiguration.zaaktypeBrpParameters?.raadpleegWaarde
            verwerkingregisterWaarde = previousZaaktypeConfiguration.zaaktypeBrpParameters?.verwerkingregisterWaarde
        }
    }
}
