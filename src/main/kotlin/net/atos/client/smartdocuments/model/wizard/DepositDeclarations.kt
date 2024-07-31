/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.smartdocuments.model.wizard

import jakarta.json.bind.annotation.JsonbProperty

class Deposit {
    @JsonbProperty("SmartDocument")
    lateinit var smartDocument: SmartDocument
}

class SmartDocument {
    @JsonbProperty("Selection")
    lateinit var selection: Selection
}

class Selection {
    @JsonbProperty("TemplateGroup")
    var templateGroup: String? = null

    @JsonbProperty("Template")
    var template: String? = null
}
