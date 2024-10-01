/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin.model

import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import net.atos.zac.util.FlywayIntegrator
import nl.lifely.zac.persistence.PreventAnyUpdate

@Entity
@EntityListeners(PreventAnyUpdate::class)
@Table(schema = FlywayIntegrator.SCHEMA, name = "zaakafhandelparameters")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_zaakafhandelparameters",
    sequenceName = "sq_zaakafhandelparameters",
    allocationSize = 1
)
open class ZaakafhandelParametersSummary : ZaakafhandelParametersBase()
