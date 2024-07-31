/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import jakarta.json.bind.annotation.JsonbProperty
import net.atos.client.smartdocuments.model.wizard.SmartDocument

data class WizardRequest(
    @get:JsonbProperty("SmartDocument")
    val smartDocument: SmartDocument,

    val registratie: Registratie,

    val data: Data
)
