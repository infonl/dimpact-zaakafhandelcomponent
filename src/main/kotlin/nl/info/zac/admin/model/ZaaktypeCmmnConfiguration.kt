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
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(schema = SCHEMA, name = "zaaktype_cmmn_configuration")
@SequenceGenerator(
    schema = SCHEMA,
    name = "sq_zaaktype_cmmn_configuration",
    sequenceName = "sq_zaaktype_cmmn_configuration",
    allocationSize = 1
)
@AllOpen
@Suppress("TooManyFunctions")
class ZaaktypeCmmnConfiguration {

    companion object {
        /** Naam van property: [zaakTypeUUID] */
        const val ZAAKTYPE_UUID = "zaakTypeUUID"

        /** Naam van property: [zaaktypeOmschrijving] */
        const val ZAAKTYPE_OMSCHRIJVING = "zaaktypeOmschrijving"

        /** Naam van property: [creatiedatum] */
        const val CREATIEDATUM = "creatiedatum"

        /** Naam van property: [productaanvraagtype] */
        const val PRODUCTAANVRAAGTYYPE = "productaanvraagtype"
    }

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_configuration", strategy = GenerationType.SEQUENCE)
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
    @Column(name = "id_case_definition")
    var caseDefinitionID: String? = null

    /**
     * This field is nullable because when a new zaaktype is published,
     * ZAC creates an initial 'inactive' zaaktypeCmmnConfiguration record without a value.
     * For 'active' zaaktypeCmmnConfiguration, however, this field becomes mandatory is never null.
     */
    @Column(name = "groep_id")
    var groepID: String? = null

    @Column(name = "gebruikersnaam_behandelaar")
    var gebruikersnaamMedewerker: String? = null

    @Column(name = "eindatum_gepland_waarschuwing")
    var einddatumGeplandWaarschuwing: Int? = null

    @Column(name = "uiterlijke_einddatum_afdoening_waarschuwing")
    var uiterlijkeEinddatumAfdoeningWaarschuwing: Int? = null

    @Column(name = "niet_ontvankelijk_resultaattype_uuid")
    var nietOntvankelijkResultaattype: UUID? = null

    // Nullable to avoid lateinit init errors; DB NOT NULL—set before persist.
    @Column(name = "creatiedatum", nullable = false)
    var creatiedatum: ZonedDateTime? = null

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

    @Column(name = "productaanvraagtype")
    var productaanvraagtype: String? = null

    @Column(name = "domein")
    var domein: String? = null

    @Column(name = "smartdocuments_ingeschakeld")
    var smartDocumentsIngeschakeld: Boolean = false

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeCmmnConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var zaaktypeCmmnHumantaskParametersCollection: MutableSet<ZaaktypeCmmnHumantaskParameters>? = null

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeCmmnConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var zaaktypeCmmnUsereventlistenerParametersCollection:
        MutableSet<ZaaktypeCmmnUsereventlistenerParameters>? = null

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeCmmnConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var zaaktypeCmmnCompletionParameters: MutableSet<ZaaktypeCmmnCompletionParameters>? = null

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeCmmnConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var zaaktypeCmmnMailtemplateKoppelingen: MutableSet<ZaaktypeCmmnMailtemplateParameters>? = null

    @OneToOne(
        mappedBy = "zaaktypeCmmnConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var zaaktypeCmmnEmailParameters: ZaaktypeCmmnEmailParameters? = null

    @OneToOne(
        mappedBy = "zaaktypeCmmnConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var zaaktypeCmmnBetrokkeneParameters: ZaaktypeCmmnBetrokkeneParameters? = null

    @OneToOne(
        mappedBy = "zaaktypeCmmnConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var zaaktypeCmmnBrpParameters: ZaaktypeCmmnBrpParameters? = null

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(
        mappedBy = "zaaktypeCmmnConfiguration",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var zaaktypeCmmnZaakafzenderParameters: MutableSet<ZaaktypeCmmnZaakafzenderParameters>? = null

    fun getHumanTaskParametersCollection(): Set<ZaaktypeCmmnHumantaskParameters> =
        zaaktypeCmmnHumantaskParametersCollection ?: emptySet()

    fun setHumanTaskParametersCollection(
        desired: Collection<ZaaktypeCmmnHumantaskParameters>
    ) {
        if (zaaktypeCmmnHumantaskParametersCollection == null) {
            zaaktypeCmmnHumantaskParametersCollection = mutableSetOf()
        }
        desired.forEach { setHumanTaskParameters(it) }
        zaaktypeCmmnHumantaskParametersCollection?.let { collection ->
            collection.removeIf { existing ->
                isElementNotInCollection(desired, existing)
            }
        }
    }

    fun getMailtemplateKoppelingen(): Set<ZaaktypeCmmnMailtemplateParameters> =
        zaaktypeCmmnMailtemplateKoppelingen ?: emptySet()

    fun setMailtemplateKoppelingen(
        desired: Collection<ZaaktypeCmmnMailtemplateParameters>
    ) {
        if (zaaktypeCmmnMailtemplateKoppelingen == null) {
            zaaktypeCmmnMailtemplateKoppelingen = mutableSetOf()
        }
        desired.forEach { setMailtemplateKoppeling(it) }
        zaaktypeCmmnMailtemplateKoppelingen?.let { mailtemplateParameters ->
            mailtemplateParameters.removeIf { existing -> isElementNotInCollection(desired, existing) }
        }
    }

    fun getAutomaticEmailConfirmation(): ZaaktypeCmmnEmailParameters? = zaaktypeCmmnEmailParameters

    fun setAutomaticEmailConfirmation(value: ZaaktypeCmmnEmailParameters?) {
        zaaktypeCmmnEmailParameters = value
    }

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

    fun getUserEventListenerParametersCollection(): Set<ZaaktypeCmmnUsereventlistenerParameters> =
        zaaktypeCmmnUsereventlistenerParametersCollection ?: emptySet()

    fun setUserEventListenerParametersCollection(
        desired: Collection<ZaaktypeCmmnUsereventlistenerParameters>
    ) {
        if (zaaktypeCmmnUsereventlistenerParametersCollection == null) {
            zaaktypeCmmnUsereventlistenerParametersCollection = mutableSetOf()
        }
        desired.forEach { setUserEventListenerParameters(it) }
        zaaktypeCmmnUsereventlistenerParametersCollection?.let { cmmnUsereventlistenerParameters ->
            cmmnUsereventlistenerParameters.removeIf { existing -> isElementNotInCollection(desired, existing) }
        }
    }

    fun getZaakAfzenders(): Set<ZaaktypeCmmnZaakafzenderParameters> =
        zaaktypeCmmnZaakafzenderParameters ?: emptySet()

    fun setZaakAfzenders(desired: Collection<ZaaktypeCmmnZaakafzenderParameters>) {
        if (zaaktypeCmmnZaakafzenderParameters == null) {
            zaaktypeCmmnZaakafzenderParameters = mutableSetOf()
        }
        desired.forEach { setZaakAfzender(it) }
        zaaktypeCmmnZaakafzenderParameters?.let { cmmnZaakafzenderParameters ->
            cmmnZaakafzenderParameters.removeIf { existing -> isElementNotInCollection(desired, existing) }
        }
    }

    private fun setMailtemplateKoppeling(param: ZaaktypeCmmnMailtemplateParameters) {
        param.zaaktypeCmmnConfiguration = this
        zaaktypeCmmnMailtemplateKoppelingen?.let { setComponent(it, param) }
    }

    private fun setHumanTaskParameters(param: ZaaktypeCmmnHumantaskParameters) {
        param.zaaktypeCmmnConfiguration = this
        zaaktypeCmmnHumantaskParametersCollection?.let { setComponent(it, param) }
    }

    private fun setZaakbeeindigParameter(param: ZaaktypeCmmnCompletionParameters) {
        param.zaaktypeCmmnConfiguration = this
        zaaktypeCmmnCompletionParameters?.let { setComponent(it, param) }
    }

    private fun setUserEventListenerParameters(param: ZaaktypeCmmnUsereventlistenerParameters) {
        param.zaaktypeCmmnConfiguration = this
        zaaktypeCmmnUsereventlistenerParametersCollection?.let { setComponent(it, param) }
    }

    private fun setZaakAfzender(param: ZaaktypeCmmnZaakafzenderParameters) {
        param.zaaktypeCmmnConfiguration = this
        zaaktypeCmmnZaakafzenderParameters?.let { setComponent(it, param) }
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
    private fun <T> isElementNotInCollection(targetCollection: Collection<T>, candidate: T): Boolean =
        targetCollection.none { it == candidate }

    private fun <T : UserModifiable<T>> elementToChange(
        persistentCollection: Collection<T>,
        changeCandidate: T
    ): T? = persistentCollection.firstOrNull { it.isModifiedFrom(changeCandidate) }

    private fun <T : UserModifiable<T>> setComponent(
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

    fun getBetrokkeneParameters(): ZaaktypeCmmnBetrokkeneParameters =
        zaaktypeCmmnBetrokkeneParameters ?: ZaaktypeCmmnBetrokkeneParameters()

    fun setBetrokkeneParameters(value: ZaaktypeCmmnBetrokkeneParameters?) {
        zaaktypeCmmnBetrokkeneParameters = value
    }

    fun getBrpDoelbindingen(): ZaaktypeCmmnBrpParameters =
        zaaktypeCmmnBrpParameters ?: ZaaktypeCmmnBrpParameters()

    fun setBrpDoelbindingen(value: ZaaktypeCmmnBrpParameters?) {
        zaaktypeCmmnBrpParameters = value
    }

    /**
     * Geeft aan dat er voldoende gegevens zijn ingevuld om een zaak te starten
     *
     * @return true indien er een zaak kan worden gestart
     */
    fun isValide(): Boolean =
        !groepID.isNullOrBlank() &&
            !caseDefinitionID.isNullOrBlank() &&
            nietOntvankelijkResultaattype != null

    @Suppress("TooGenericExceptionThrown")
    fun readZaakbeeindigParameter(zaakbeeindigRedenId: Long): ZaaktypeCmmnCompletionParameters =
        getZaakbeeindigParameters().firstOrNull {
            it.zaakbeeindigReden.id == zaakbeeindigRedenId
        } ?: throw RuntimeException(
            "No ZaakbeeindigParameter found for zaaktypeUUID: '$zaakTypeUUID' and zaakbeeindigRedenId: '$zaakbeeindigRedenId'"
        )

    @Suppress("TooGenericExceptionThrown")
    fun readUserEventListenerParameters(planitemDefinitionID: String): ZaaktypeCmmnUsereventlistenerParameters =
        getUserEventListenerParametersCollection().firstOrNull {
            it.planItemDefinitionID == planitemDefinitionID
        } ?: throw RuntimeException(
            "No UserEventListenerParameters found for zaaktypeUUID: '$zaakTypeUUID' and planitemDefinitionID: '$planitemDefinitionID'"
        )

//    fun findHumanTaskParameter(planitemDefinitionID: String): Optional<ZaaktypeCmmnHumantaskParameters> =
//        Optional.ofNullable(
//            getHumanTaskParametersCollection().firstOrNull { it.planItemDefinitionID == planitemDefinitionID }
//        )

    fun findHumanTaskParameter(planitemDefinitionID: String): ZaaktypeCmmnHumantaskParameters? =
        getHumanTaskParametersCollection().find { it.planItemDefinitionID == planitemDefinitionID }
}
