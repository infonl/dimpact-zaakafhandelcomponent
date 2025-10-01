/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen
import java.util.Objects

@Entity
@Table(schema = SCHEMA, name = "zaaktype_cmmn_usereventlistener_parameters")
@SequenceGenerator(
    schema = SCHEMA,
    name = "sq_zaaktype_cmmn_usereventlistener_parameters",
    sequenceName = "sq_zaaktype_cmmn_usereventlistener_parameters",
    allocationSize = 1
)
@AllOpen
class ZaaktypeCmmnUsereventlistenerParameters : UserModifiable<ZaaktypeCmmnUsereventlistenerParameters> {

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_usereventlistener_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    var id: Long? = null

    @Column(name = "id_planitem_definition")
    var planItemDefinitionID: String? = null

    @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    @NotNull
    lateinit var zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration

    @Column(name = "toelichting")
    var toelichting: String? = null

    override fun isModifiedFrom(original: ZaaktypeCmmnUsereventlistenerParameters): Boolean {
        return Objects.equals(planItemDefinitionID, original.planItemDefinitionID) &&
            !Objects.equals(this.toelichting, original.toelichting)
    }

    override fun applyChanges(changes: ZaaktypeCmmnUsereventlistenerParameters) {
        this.toelichting = changes.toelichting
    }

    override fun resetId(): ZaaktypeCmmnUsereventlistenerParameters {
        id = null
        return this
    }
}
