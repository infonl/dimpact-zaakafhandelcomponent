/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.validateObject
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
@AllOpen
@Suppress("TooManyFunctions")
class ZaaktypeConfiguration {
    companion object {
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

    @Column(name = "gebruikersnaam_behandelaar")
    var defaultBehandelaarId: String? = null

    @Column(name = "smartdocuments_ingeschakeld")
    var isSmartDocumentsEnabled: Boolean = false

    @field:NotNull
    @Column(name = "creatiedatum", nullable = false)
    var creatiedatum: ZonedDateTime? = null

    @Column(name = "productaanvraagtype")
    var productaanvraagtype: String? = null

    @Column(name = "niet_ontvankelijk_resultaattype_omschrijving")
    var nietOntvankelijkResultaattypeOmschrijving: String? = null

    @Column(name = "einddatum_gepland_waarschuwing")
    var einddatumGeplandWaarschuwing: Int? = null

    @Column(name = "uiterlijke_einddatum_afdoening_waarschuwing")
    var uiterlijkeEinddatumAfdoeningWaarschuwing: Int? = null

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

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var zaaktypeCompletionParameters: MutableSet<ZaaktypeCompletionParameters>? = null

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var zaaktypeMailtemplateKoppelingen: MutableSet<ZaaktypeMailtemplateParameters>? = null

    @OneToOne(
        mappedBy = "zaaktypeConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var zaaktypeEmailParameters: ZaaktypeEmailParameters? = null

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var zaaktypeZaakafzenderParameters: MutableSet<ZaaktypeZaakafzenderParameters>? = null

    @OneToOne(
        mappedBy = "zaaktypeConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var processBinding: ZaaktypeProcessBinding? = null

    @OneToOne(
        mappedBy = "zaaktypeConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var cmmnExtension: ZaaktypeCmmnExtension? = null

    fun getProcessEngine(): ProcessEngine? = processBinding?.processEngine

    /**
     * Binds this configuration to the given engine and definition, replacing the definition of a previous binding.
     * A configuration never changes engine, and a BPMN-bound configuration has no CMMN extension.
     */
    fun bindTo(processEngine: ProcessEngine, definitionKey: String) {
        getProcessEngine()?.takeIf { it != processEngine }?.let {
            throw InputValidationFailedException(
                message = "Zaaktype configuration for zaaktype '$zaaktypeUuid' is bound to $it and cannot be " +
                    "bound to $processEngine"
            )
        }
        if (processEngine == ProcessEngine.BPMN) {
            cmmnExtension = null
        }
        val binding = processBinding ?: ZaaktypeProcessBinding().also { processBinding = it }
        binding.zaaktypeConfiguration = this
        binding.processEngine = processEngine
        binding.definitionKey = definitionKey
    }

    /**
     * Returns the CMMN extension, after creating it when this configuration has none yet.
     */
    fun getOrCreateCmmnExtension(): ZaaktypeCmmnExtension =
        cmmnExtension ?: ZaaktypeCmmnExtension().also {
            it.zaaktypeConfiguration = this
            cmmnExtension = it
        }

    /**
     * Indicates whether this zaaktype configuration is valid to be used to create a zaak of this zaaktype or not.
     * A CMMN zaaktype also needs the niet-ontvankelijk resultaattype, because its intake can end the zaak as niet-ontvankelijk.
     */
    fun isValidForZaakCreation(): Boolean =
        !groepID.isNullOrBlank() &&
            processBinding?.definitionKey?.isNotBlank() == true &&
            (getProcessEngine() != ProcessEngine.CMMN || nietOntvankelijkResultaattypeOmschrijving != null)

    fun getBetrokkeneParameters(): ZaaktypeBetrokkeneParameters =
        zaaktypeBetrokkeneParameters ?: ZaaktypeBetrokkeneParameters()

    fun getBrpParameters(): ZaaktypeBrpParameters =
        zaaktypeBrpParameters ?: ZaaktypeBrpParameters()

    @Suppress("TooGenericExceptionThrown")
    fun readZaakbeeindigParameter(zaakbeeindigRedenId: Long): ZaaktypeCompletionParameters =
        getZaakbeeindigParameters().firstOrNull {
            it.zaakbeeindigReden.id == zaakbeeindigRedenId
        } ?: throw RuntimeException(
            "No ZaakbeeindigParameter found for zaaktypeUUID: '$zaaktypeUuid' and zaakbeeindigRedenId: '$zaakbeeindigRedenId'"
        )

    fun getZaakbeeindigParameters(): Set<ZaaktypeCompletionParameters> =
        zaaktypeCompletionParameters.orEmpty()

    fun setZaakbeeindigParameters(desired: Collection<ZaaktypeCompletionParameters>) {
        val completionParameters = zaaktypeCompletionParameters ?: mutableSetOf<ZaaktypeCompletionParameters>().also {
            zaaktypeCompletionParameters = it
        }
        desired.forEach { it.zaaktypeConfiguration = this }
        completionParameters.mergeWith(desired) { it.zaakbeeindigReden.id }
    }

    fun getMailtemplateKoppelingen(): Set<ZaaktypeMailtemplateParameters> = zaaktypeMailtemplateKoppelingen.orEmpty()

    fun setMailtemplateKoppelingen(desired: Collection<ZaaktypeMailtemplateParameters>) {
        val mailtemplateKoppelingen = zaaktypeMailtemplateKoppelingen
            ?: mutableSetOf<ZaaktypeMailtemplateParameters>().also { zaaktypeMailtemplateKoppelingen = it }
        desired.forEach { it.zaaktypeConfiguration = this }
        mailtemplateKoppelingen.mergeWith(desired) { it.mailTemplate?.mail }
    }

    fun getAutomaticEmailConfirmation(): ZaaktypeEmailParameters? = zaaktypeEmailParameters

    fun getZaakAfzenders(): Set<ZaaktypeZaakafzenderParameters> = zaaktypeZaakafzenderParameters.orEmpty()

    fun setZaakAfzenders(desired: Collection<ZaaktypeZaakafzenderParameters>) {
        val zaakAfzenders = zaaktypeZaakafzenderParameters
            ?: mutableSetOf<ZaaktypeZaakafzenderParameters>().also { zaaktypeZaakafzenderParameters = it }
        desired.forEach { it.zaaktypeConfiguration = this }
        zaakAfzenders.mergeWith(desired) { it.mail }
    }
}

fun ZaaktypeConfiguration.validate() {
    validateObject(this)
    processBinding?.let { validateObject(it) }
    getMailtemplateKoppelingen().forEach { validateObject(it) }
    cmmnExtension?.getHumanTaskParametersCollection()?.forEach { validateObject(it) }
    cmmnExtension?.getUserEventListenerParametersCollection()?.forEach { validateObject(it) }
}
