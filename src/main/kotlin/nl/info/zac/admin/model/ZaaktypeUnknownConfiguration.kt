/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = SCHEMA, name = "zaaktype_unknown_configuration")
@DiscriminatorValue("UNKNOWN")
@PrimaryKeyJoinColumn(name = "id")
@AllOpen
class ZaaktypeUnknownConfiguration : ZaaktypeConfiguration() {
    override fun getConfigurationType() = Companion.ZaaktypeConfigurationType.UNKNOWN
}
