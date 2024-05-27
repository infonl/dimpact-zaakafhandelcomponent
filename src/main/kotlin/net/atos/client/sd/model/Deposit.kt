/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model

import jakarta.json.bind.annotation.JsonbProperty

open class Deposit {
    @JsonbProperty("SmartDocument")
    lateinit var smartDocument: SmartDocument
}
