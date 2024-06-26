/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.smartdocuments.model.wizard

import jakarta.json.bind.annotation.JsonbProperty

class UnattendedResponse {
    @JsonbProperty("file")
    var files: List<File>? = null
}

class File {
    lateinit var fileName: String
    lateinit var document: Document
}

class Document
