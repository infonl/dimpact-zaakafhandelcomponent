/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.smartdocuments.model.document

import jakarta.json.bind.annotation.JsonbProperty
import net.atos.zac.documentcreation.model.Data
import net.atos.zac.documentcreation.model.Registratie

data class Deposit(
    @field:JsonbProperty("SmartDocument")
    val smartDocument: SmartDocument
)

data class SmartDocument(
    @field:JsonbProperty("Selection")
    val selection: Selection
)

data class Selection(
    @field:JsonbProperty("TemplateGroup")
    val templateGroup: String? = null,

    @field:JsonbProperty("Template")
    val template: String? = null
)

data class WizardRequest(
    @field:JsonbProperty("SmartDocument")
    val smartDocument: SmartDocument,

    @field:JsonbProperty("registratie")
    val registratie: Registratie,

    @field:JsonbProperty("data")
    val data: Data
)
