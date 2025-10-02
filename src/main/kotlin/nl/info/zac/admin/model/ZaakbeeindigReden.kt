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
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen
import java.util.Objects

@Entity
@Table(schema = FlywayIntegrator.Companion.SCHEMA, name = "zaakbeeindigreden")
@SequenceGenerator(
    schema = FlywayIntegrator.Companion.SCHEMA,
    name = "sq_zaakbeeindigreden",
    sequenceName = "sq_zaakbeeindigreden",
    allocationSize = 1
)
@AllOpen
class ZaakbeeindigReden {
    @Id
    @GeneratedValue(generator = "sq_zaakbeeindigreden", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_zaakbeeindigreden")
    var id: Long? = null

    @Column(name = "naam", nullable = false)
    @field:NotBlank
    var naam: String? = null

    override fun equals(other: Any?): Boolean {
        if (other !is ZaakbeeindigReden) return false
        return naam == other.naam
    }

    override fun hashCode(): Int {
        return Objects.hashCode(naam)
    }
}
