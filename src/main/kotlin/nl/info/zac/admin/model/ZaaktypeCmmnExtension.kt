/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
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
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen

/**
 * The settings of a zaaktype version that only the CMMN engine uses: the human tasks and user event listeners of the
 * case plan, and the status mails that its intake and afronden tasks offer.
 */
@Entity
@Table(schema = SCHEMA, name = "zaaktype_cmmn_extension")
@SequenceGenerator(
    schema = SCHEMA,
    name = "sq_zaaktype_cmmn_extension",
    sequenceName = "sq_zaaktype_cmmn_extension",
    allocationSize = 1
)
@AllOpen
class ZaaktypeCmmnExtension {
    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_extension", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @OneToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id", nullable = false)
    lateinit var zaaktypeConfiguration: ZaaktypeConfiguration

    /**
     * This field has a sensible default value because it is non-nullable.
     */
    @Column(name = "intake_mail", nullable = false)
    var intakeMail: String? = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT.name

    /**
     * This field has a sensible default value because it is non-nullable.
     */
    @Column(name = "afronden_mail", nullable = false)
    var afrondenMail: String? = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT.name

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeCmmnExtension",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var zaaktypeCmmnHumantaskParametersCollection: MutableSet<ZaaktypeCmmnHumantaskParameters>? = null

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeCmmnExtension",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var zaaktypeCmmnUsereventlistenerParametersCollection:
        MutableSet<ZaaktypeCmmnUsereventlistenerParameters>? = null

    fun getHumanTaskParametersCollection(): Set<ZaaktypeCmmnHumantaskParameters> =
        zaaktypeCmmnHumantaskParametersCollection.orEmpty()

    fun setHumanTaskParametersCollection(desired: Collection<ZaaktypeCmmnHumantaskParameters>) {
        val humanTaskParameters = zaaktypeCmmnHumantaskParametersCollection
            ?: mutableSetOf<ZaaktypeCmmnHumantaskParameters>().also { zaaktypeCmmnHumantaskParametersCollection = it }
        desired.forEach { it.zaaktypeCmmnExtension = this }
        humanTaskParameters.mergeWith(desired) { it.planItemDefinitionID }
    }

    fun getUserEventListenerParametersCollection(): Set<ZaaktypeCmmnUsereventlistenerParameters> =
        zaaktypeCmmnUsereventlistenerParametersCollection.orEmpty()

    fun setUserEventListenerParametersCollection(desired: Collection<ZaaktypeCmmnUsereventlistenerParameters>) {
        val userEventListenerParameters = zaaktypeCmmnUsereventlistenerParametersCollection
            ?: mutableSetOf<ZaaktypeCmmnUsereventlistenerParameters>().also {
                zaaktypeCmmnUsereventlistenerParametersCollection = it
            }
        desired.forEach { it.zaaktypeCmmnExtension = this }
        userEventListenerParameters.mergeWith(desired) { it.planItemDefinitionID }
    }

    @Suppress("TooGenericExceptionThrown")
    fun readUserEventListenerParameters(planitemDefinitionID: String): ZaaktypeCmmnUsereventlistenerParameters =
        getUserEventListenerParametersCollection().firstOrNull {
            it.planItemDefinitionID == planitemDefinitionID
        } ?: throw RuntimeException(
            "No UserEventListenerParameters found for zaaktypeUUID: '${zaaktypeConfiguration.zaaktypeUuid}' " +
                "and planitemDefinitionID: '$planitemDefinitionID'"
        )

    fun findHumanTaskParameter(planitemDefinitionID: String): ZaaktypeCmmnHumantaskParameters? =
        getHumanTaskParametersCollection().find { it.planItemDefinitionID == planitemDefinitionID }
}
