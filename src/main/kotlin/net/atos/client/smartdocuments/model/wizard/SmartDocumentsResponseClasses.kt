/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.smartdocuments.model.wizard

import jakarta.json.bind.annotation.JsonbProperty
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
data class UnattendedResponse(
    @field:JsonbProperty("file")
    var files: List<File>? = null
)

@NoArgConstructor
data class File(
    var fileName: String? = null,
    var document: Document? = null
)

class Document

@NoArgConstructor
data class WizardResponse(
    var ticket: String? = null
)
