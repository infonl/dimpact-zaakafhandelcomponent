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
import jakarta.persistence.OneToMany
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

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeCmmnConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var zaaktypeCmmnCompletionParameters: MutableSet<ZaaktypeCmmnCompletionParameters>? = null

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

    @Suppress("TooGenericExceptionThrown")
    fun readZaakbeeindigParameter(zaakbeeindigRedenId: Long): ZaaktypeCmmnCompletionParameters =
        getZaakbeeindigParameters().firstOrNull {
            it.zaakbeeindigReden.id == zaakbeeindigRedenId
        } ?: throw RuntimeException(
            "No ZaakbeeindigParameter found for zaaktypeUUID: '$zaaktypeUuid' and zaakbeeindigRedenId: '$zaakbeeindigRedenId'"
        )

    fun getZaakbeeindigParameters(): Set<ZaaktypeCmmnCompletionParameters> =
        zaaktypeCmmnCompletionParameters ?: emptySet()

    fun setZaakbeeindigParameters(desired: Collection<ZaaktypeCmmnCompletionParameters>?) {
        if (zaaktypeCmmnCompletionParameters == null) {
            zaaktypeCmmnCompletionParameters = mutableSetOf()
        }
        desired?.forEach { setZaakbeeindigParameter(it) }
        zaaktypeCmmnCompletionParameters?.let { cmmnCompletionParameters ->
            desired?.let { d ->
                cmmnCompletionParameters.removeIf { existing -> isElementNotInCollection(d, existing) }
            }
        }
    }

    private fun setZaakbeeindigParameter(param: ZaaktypeCmmnCompletionParameters) {
        param.zaaktypeCmmnConfiguration = this
        zaaktypeCmmnCompletionParameters?.let { setComponent(it, param) }
    }

    /**
     * This method replaces the Hibernate's PersistentSet#contains that does not use overridden <code>equals</code>
     * and <code>hashCode</code>.
     *
     * @param targetCollection Collection that should be checked for the existence of the candidate element.
     * @param candidate        Candidate element to be added to the collection.
     * @return <code>true</code> if the element is not in the collection, <code>false</code> otherwise.
     *
     * @see <a href=https://hibernate.atlassian.net/browse/HHH-3799>Hibernate issue</a>
     *
     */
    fun <T> isElementNotInCollection(targetCollection: Collection<T>, candidate: T): Boolean =
        targetCollection.none { it == candidate }

    fun <T : UserModifiable<T>> elementToChange(
        persistentCollection: Collection<T>,
        changeCandidate: T
    ): T? = persistentCollection.firstOrNull { it.isModifiedFrom(changeCandidate) }

    fun <T : UserModifiable<T>> setComponent(
        targetCollection: MutableCollection<T>,
        candidate: T
    ) {
        val existingElement = elementToChange(targetCollection, candidate)
        if (existingElement != null) {
            existingElement.applyChanges(candidate)
        } else {
            if (isElementNotInCollection(targetCollection, candidate)) {
                targetCollection.add(candidate.resetId())
            }
        }
    }
}
