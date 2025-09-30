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
import net.atos.zac.admin.model.HumanTaskReferentieTabel
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
    UserModifiable<ZaaktypeCmmnHumantaskParameters> {

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_humantask_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    @NotNull
    var zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration? = null

    @Column(name = "actief")
    var actief: Boolean = false

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

    fun setReferentieTabellen(value: List<HumanTaskReferentieTabel>) {
        this.referentieTabellen.clear()
        value.forEach { addReferentieTabel(it) }
    }

    private fun addReferentieTabel(referentieTabel: HumanTaskReferentieTabel): Boolean {
        referentieTabel.setHumantask(this)
        return referentieTabellen.add(referentieTabel)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ZaaktypeCmmnHumantaskParameters) return false
        return actief == other.actief &&
            Objects.equals(formulierDefinitieID, other.formulierDefinitieID) &&
            Objects.equals(planItemDefinitionID, other.planItemDefinitionID) &&
            Objects.equals(groepID, other.groepID) &&
            Objects.equals(doorlooptijd, other.doorlooptijd) && (
                (
                    (referentieTabellen != null && other.referentieTabellen != null) &&
                        Objects.deepEquals(
                            referentieTabellen.toTypedArray(),
                            other.referentieTabellen.toTypedArray()
                        )
                    )
                )
    }

    override fun hashCode(): Int =
        Objects.hash(actief, formulierDefinitieID, planItemDefinitionID, groepID, doorlooptijd, referentieTabellen)

    override fun isModifiedFrom(original: ZaaktypeCmmnHumantaskParameters): Boolean {
        return Objects.equals(original.planItemDefinitionID, planItemDefinitionID) &&
            (
                actief != original.actief ||
                    !Objects.equals(original.formulierDefinitieID, formulierDefinitieID) ||
                    !Objects.equals(original.groepID, groepID) ||
                    !Objects.equals(original.doorlooptijd, doorlooptijd) ||
                    (
                        referentieTabellen != null && original.referentieTabellen != null &&
                            !Objects.deepEquals(
                                referentieTabellen.toTypedArray(),
                                original.referentieTabellen.toTypedArray()
                            )
                        ) ||
                    (referentieTabellen == null && original.referentieTabellen == null)
                )
    }

    override fun applyChanges(changes: ZaaktypeCmmnHumantaskParameters) {
        actief = changes.actief
        formulierDefinitieID = changes.formulierDefinitieID
        groepID = changes.groepID
        doorlooptijd = changes.doorlooptijd
        referentieTabellen = changes.referentieTabellen
    }

    override fun resetId(): ZaaktypeCmmnHumantaskParameters {
        id = null
        return this
    }
}
