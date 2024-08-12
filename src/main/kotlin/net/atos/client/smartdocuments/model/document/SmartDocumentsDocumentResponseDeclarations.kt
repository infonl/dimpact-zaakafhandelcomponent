/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.smartdocuments.model.document

import jakarta.json.bind.annotation.JsonbProperty
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
data class AttendedResponse(
    @field:JsonbProperty("ticket")
    var ticket: String? = null
)

@NoArgConstructor
data class Document(
    @field:JsonbProperty("data")
    var data: String? = null
)

@NoArgConstructor
data class File(
    @field:JsonbProperty("filename")
    var fileName: String? = null,

    @field:JsonbProperty("document")
    var document: Document? = null,

    @field:JsonbProperty("outputFormat")
    var outputFormat: String? = null
)

@NoArgConstructor
data class UnattendedResponse(
    @field:JsonbProperty("file")
    var files: List<File>? = null
)
