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
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.app.planitems.converter.toFormulierDefinitie
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen
import java.util.Collections
import java.util.Objects

@Entity
@Table(schema = SCHEMA, name = "zaaktype_cmmn_humantask_parameters")
@SequenceGenerator(
    schema = SCHEMA,
    name = "sq_zaaktype_cmmn_humantask_parameters",
    sequenceName = "sq_zaaktype_cmmn_humantask_parameters",
    allocationSize = 1
)
@AllOpen
class ZaaktypeCmmnHumantaskParameters :
    UserModifiableZaaktypeConfigurationData<ZaaktypeCmmnHumantaskParameters> {

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_humantask_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "zaaktype_cmmn_extension_id", referencedColumnName = "id")
    @NotNull
    lateinit var zaaktypeCmmnExtension: ZaaktypeCmmnExtension

    @Column(name = "actief")
    var isActief: Boolean = false

    @Column(name = "id_formulier_definition")
    private var formulierDefinitieID: String? = null

    @NotBlank
    @Column(name = "id_planitem_definition", nullable = false)
    lateinit var planItemDefinitionID: String

    @Column(name = "id_groep", nullable = false)
    var groepID: String? = null

    @Min(0)
    @Column(name = "doorlooptijd")
    var doorlooptijd: Int? = null

    @OneToMany(
        mappedBy = "humantask",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    private var referentieTabellen: MutableList<HumanTaskReferentieTabel> = ArrayList()

    fun getFormulierDefinitieID(): String? =
        formulierDefinitieID ?: planItemDefinitionID.toFormulierDefinitie().name

    fun setFormulierDefinitieID(formulierDefinitieID: String?) {
        this.formulierDefinitieID = formulierDefinitieID
    }

    fun getReferentieTabellen(): List<HumanTaskReferentieTabel> =
        Collections.unmodifiableList(referentieTabellen)

    // Hibernate flushes inserts before orphan deletes, so replacing a coupling instead of updating it would violate
    // the unique constraint on (human task, veld). A coupling that still belongs to another human task is copied,
    // because adopting it would move its row away from that human task.
    fun setReferentieTabellen(value: List<HumanTaskReferentieTabel>) {
        val desiredByVeld = value.associateBy { it.veld }
        require(desiredByVeld.size == value.size) { "Reference table couplings have duplicate velden" }
        referentieTabellen.removeIf { it.veld !in desiredByVeld }
        referentieTabellen.forEach { it.tabel = desiredByVeld.getValue(it.veld).tabel }
        val existingVelden = referentieTabellen.map { it.veld }.toSet()
        desiredByVeld.filterKeys { it !in existingVelden }.values.forEach {
            addReferentieTabel(it.copyForNewHumantask())
        }
    }

    private fun addReferentieTabel(referentieTabel: HumanTaskReferentieTabel): Boolean {
        referentieTabel.humantask = this
        return referentieTabellen.add(referentieTabel)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ZaaktypeCmmnHumantaskParameters) return false
        return isActief == other.isActief &&
            Objects.equals(formulierDefinitieID, other.formulierDefinitieID) &&
            Objects.equals(planItemDefinitionID, other.planItemDefinitionID) &&
            Objects.equals(groepID, other.groepID) &&
            Objects.equals(doorlooptijd, other.doorlooptijd) &&
            Objects.deepEquals(referentieTabellen.toTypedArray(), other.referentieTabellen.toTypedArray())
    }

    // Constant per class, because Hibernate adds an element to an eager PersistentSet before it has loaded all its
    // fields (https://hibernate.atlassian.net/browse/HHH-3799), and `applyChanges` modifies elements inside the set.
    // A hash code based on these fields would leave the element in the wrong bucket, so `contains` would miss it.
    override fun hashCode() = javaClass.hashCode()

    override fun applyChanges(changes: ZaaktypeCmmnHumantaskParameters) {
        isActief = changes.isActief
        formulierDefinitieID = changes.formulierDefinitieID
        groepID = changes.groepID
        doorlooptijd = changes.doorlooptijd
        setReferentieTabellen(changes.getReferentieTabellen())
    }

    override fun resetId(): ZaaktypeCmmnHumantaskParameters {
        id = null
        return this
    }
}
